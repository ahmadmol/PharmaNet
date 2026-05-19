-- Allow pharmacies to fulfill B2C orders after the customer accepts the confirmed price.
-- customer_accept_order_price moves CONFIRMED orders to IN_PROGRESS, so fulfillment
-- RPCs must accept both CONFIRMED and IN_PROGRESS as source states.

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
        updated_at = NOW()
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
        updated_at = NOW()
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

    RETURN v_updated;
END;
$$;

GRANT EXECUTE ON FUNCTION public.mark_customer_order_ready_for_pickup(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_out_for_delivery(text) TO authenticated;
