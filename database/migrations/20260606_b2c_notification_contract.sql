-- Normalize PUBLIC_USER <-> PHARMACY B2C notification ownership and routing.
-- Server-side RPCs/triggers own these lifecycle notifications; Kotlin best-effort
-- B2C inserts are disabled to avoid duplicates and RLS-invisible rows.

CREATE OR REPLACE FUNCTION public.create_b2c_customer_notification(
    p_order public.orders,
    p_title text,
    p_body text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF p_order.customer_id IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO public.app_notifications (
        user_id,
        pharmacy_id,
        title,
        body,
        read,
        type,
        category,
        destination,
        destination_id,
        created_at
    ) VALUES (
        p_order.customer_id,
        p_order.pharmacy_id,
        p_title,
        p_body,
        false,
        'ORDER_UPDATE',
        'ORDERS',
        'ORDER',
        p_order.id::text,
        now()
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.create_b2c_pharmacy_notification(
    p_order public.orders,
    p_title text,
    p_body text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF p_order.pharmacy_id IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO public.app_notifications (
        user_id,
        pharmacy_id,
        title,
        body,
        read,
        type,
        category,
        destination,
        destination_id,
        created_at
    )
    SELECT
        p.id,
        p_order.pharmacy_id,
        p_title,
        p_body,
        false,
        'ORDER_UPDATE',
        'ORDERS',
        'PHARMACY_CUSTOMER_ORDER',
        p_order.id::text,
        now()
    FROM public.profiles p
    WHERE p.account_type = 'PHARMACY'
      AND p.is_active = true
      AND p.pharmacy_id = p_order.pharmacy_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_b2c_customer_notification(public.orders, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.create_b2c_pharmacy_notification(public.orders, text, text) FROM PUBLIC;

CREATE OR REPLACE FUNCTION public.handle_new_b2c_order_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
       OR NEW.request_id IS NOT NULL
       OR NEW.pharmacy_id IS NULL THEN
        RETURN NEW;
    END IF;

    PERFORM public.create_b2c_pharmacy_notification(
        NEW,
        'New customer order',
        'A customer order is waiting for review.'
    );

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS notify_pharmacy_on_new_order ON public.orders;
DROP TRIGGER IF EXISTS handle_new_b2c_order_notification ON public.orders;
DROP TRIGGER IF EXISTS new_b2c_order_notification ON public.orders;
DROP TRIGGER IF EXISTS trigger_new_b2c_order_notification ON public.orders;
DROP TRIGGER IF EXISTS notify_new_b2c_order ON public.orders;

CREATE TRIGGER notify_pharmacy_on_new_order
AFTER INSERT ON public.orders
FOR EACH ROW
WHEN (
    NEW.order_type = 'CUSTOMER_PHARMACY'
    AND NEW.request_id IS NULL
    AND NEW.pharmacy_id IS NOT NULL
)
EXECUTE FUNCTION public.handle_new_b2c_order_notification();

CREATE OR REPLACE FUNCTION public.confirm_customer_order(
    p_order_id text,
    p_total_price_cents bigint
)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    IF p_total_price_cents IS NULL OR p_total_price_cents < 0 THEN
        RAISE EXCEPTION 'Confirmed price must be non-null and non-negative'
            USING ERRCODE = 'P0001';
    END IF;

    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can confirm customer orders'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be confirmed by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'CONFIRMED',
        total_price_cents = p_total_price_cents,
        confirmed_at = now(),
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.status = 'PENDING'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order confirmation did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_customer_notification(
        v_updated,
        'Price quote ready',
        'Your pharmacy has sent a price quote. Review the order to accept or reject it.'
    );

    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.customer_accept_order_price(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_customer_id uuid;
BEGIN
    SELECT p.account_type, p.id
    INTO v_account_type, v_customer_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PUBLIC_USER' THEN
        RAISE EXCEPTION 'Only PUBLIC_USER can accept order prices'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.customer_id IS DISTINCT FROM v_customer_id THEN
        RAISE EXCEPTION 'Cannot accept price for order that does not belong to you'
            USING ERRCODE = '42501';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY' THEN
        RAISE EXCEPTION 'Can only accept prices for CUSTOMER_PHARMACY orders'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.status IS DISTINCT FROM 'CONFIRMED' THEN
        RAISE EXCEPTION 'Can only accept price for CONFIRMED orders. Current status: %', v_order.status
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.total_price_cents IS NULL OR v_order.total_price_cents < 0 THEN
        RAISE EXCEPTION 'Cannot accept order without a non-negative confirmed price'
            USING ERRCODE = 'P0001';
    END IF;

    UPDATE public.orders o
    SET status = 'IN_PROGRESS',
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.status = 'CONFIRMED'
      AND o.customer_id = v_customer_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order acceptance did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_pharmacy_notification(
        v_updated,
        'Price quote accepted',
        'The customer accepted the price quote. Prepare the order.'
    );

    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.customer_reject_order_price(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_customer_id uuid;
BEGIN
    SELECT p.account_type, p.id
    INTO v_account_type, v_customer_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PUBLIC_USER' THEN
        RAISE EXCEPTION 'Only PUBLIC_USER can reject order prices'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.customer_id IS DISTINCT FROM v_customer_id THEN
        RAISE EXCEPTION 'Cannot reject price for order that does not belong to you'
            USING ERRCODE = '42501';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY' THEN
        RAISE EXCEPTION 'Can only reject prices for CUSTOMER_PHARMACY orders'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.status IS DISTINCT FROM 'CONFIRMED' THEN
        RAISE EXCEPTION 'Can only reject price for CONFIRMED orders. Current status: %', v_order.status
            USING ERRCODE = 'P0001';
    END IF;

    UPDATE public.orders o
    SET status = 'REJECTED',
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.status = 'CONFIRMED'
      AND o.customer_id = v_customer_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order rejection did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_pharmacy_notification(
        v_updated,
        'Price quote rejected',
        'The customer rejected the price quote.'
    );

    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.reject_customer_order(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can reject customer orders'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be rejected by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'REJECTED',
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.status = 'PENDING'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order rejection did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_customer_notification(
        v_updated,
        'Order rejected',
        'The pharmacy rejected your order.'
    );

    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.mark_customer_order_ready_for_pickup(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can mark customer orders ready for pickup'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status NOT IN ('CONFIRMED', 'IN_PROGRESS')
        OR v_order.fulfillment_type IS DISTINCT FROM 'PICKUP'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be marked ready for pickup by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'READY_FOR_PICKUP',
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.status IN ('CONFIRMED', 'IN_PROGRESS')
      AND o.fulfillment_type = 'PICKUP'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Ready-for-pickup transition did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_customer_notification(
        v_updated,
        'Order ready for pickup',
        'Your order is ready for pickup.'
    );

    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.mark_customer_order_out_for_delivery(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can mark customer orders out for delivery'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status NOT IN ('CONFIRMED', 'IN_PROGRESS')
        OR v_order.fulfillment_type IS DISTINCT FROM 'DELIVERY'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be marked out for delivery by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'OUT_FOR_DELIVERY',
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.status IN ('CONFIRMED', 'IN_PROGRESS')
      AND o.fulfillment_type = 'DELIVERY'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Out-for-delivery transition did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_customer_notification(
        v_updated,
        'Order out for delivery',
        'Your order is out for delivery.'
    );

    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.mark_customer_order_delivered(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can mark customer orders delivered'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL
        OR NOT (
            (v_order.status = 'READY_FOR_PICKUP' AND v_order.fulfillment_type = 'PICKUP')
            OR
            (v_order.status = 'OUT_FOR_DELIVERY' AND v_order.fulfillment_type = 'DELIVERY')
        ) THEN
        RAISE EXCEPTION 'Order cannot be marked delivered by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'DELIVERED',
        fulfilled_at = now(),
        updated_at = now()
    WHERE o.id = p_order_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
      AND (
          (o.status = 'READY_FOR_PICKUP' AND o.fulfillment_type = 'PICKUP')
          OR
          (o.status = 'OUT_FOR_DELIVERY' AND o.fulfillment_type = 'DELIVERY')
      )
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Delivered transition did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    PERFORM public.create_b2c_customer_notification(
        v_updated,
        'Order delivered',
        'Your order has been delivered.'
    );

    RETURN v_updated;
END;
$$;

GRANT EXECUTE ON FUNCTION public.confirm_customer_order(text, bigint) TO authenticated;
GRANT EXECUTE ON FUNCTION public.customer_accept_order_price(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.customer_reject_order_price(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reject_customer_order(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_ready_for_pickup(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_out_for_delivery(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_delivered(text) TO authenticated;
