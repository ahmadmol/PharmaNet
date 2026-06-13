-- Admin lifecycle audit:
-- - Write audit_logs rows for B2C and B2B lifecycle events so ADMIN recent
--   activities can observe business operations without admin notifications.
-- - Preserve existing lifecycle statuses and participant notifications.

-- --------------------------------------------------------------------
-- 1. Lifecycle audit metadata on existing audit_logs
-- --------------------------------------------------------------------

ALTER TABLE public.audit_logs
  ADD COLUMN IF NOT EXISTS actor_user_id uuid REFERENCES auth.users(id),
  ADD COLUMN IF NOT EXISTS actor_role text,
  ADD COLUMN IF NOT EXISTS entity_type text,
  ADD COLUMN IF NOT EXISTS entity_id text,
  ADD COLUMN IF NOT EXISTS old_status text,
  ADD COLUMN IF NOT EXISTS new_status text,
  ADD COLUMN IF NOT EXISTS pharmacy_id uuid REFERENCES public.pharmacies(id),
  ADD COLUMN IF NOT EXISTS warehouse_id uuid REFERENCES public.warehouses(id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_lifecycle_entity
  ON public.audit_logs(entity_type, entity_id, created_at DESC)
  WHERE entity_type IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_lifecycle_actor
  ON public.audit_logs(actor_user_id, created_at DESC)
  WHERE actor_user_id IS NOT NULL;

CREATE OR REPLACE FUNCTION public.write_lifecycle_audit(
  p_actor_user_id uuid,
  p_actor_role text,
  p_entity_type text,
  p_entity_id text,
  p_action text,
  p_old_status text,
  p_new_status text,
  p_pharmacy_id uuid DEFAULT NULL,
  p_warehouse_id uuid DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_actor_user_id uuid := COALESCE(p_actor_user_id, auth.uid());
  v_actor_role text := p_actor_role;
  v_actor_email text;
BEGIN
  IF v_actor_user_id IS NULL THEN
    RETURN;
  END IF;

  IF v_actor_role IS NULL THEN
    SELECT account_type
    INTO v_actor_role
    FROM public.profiles
    WHERE id = v_actor_user_id;
  END IF;

  SELECT email
  INTO v_actor_email
  FROM auth.users
  WHERE id = v_actor_user_id;

  INSERT INTO public.audit_logs (
    admin_id,
    admin_email,
    action,
    target_user_id,
    target_pharmacy_id,
    target_warehouse_id,
    old_value,
    new_value,
    actor_user_id,
    actor_role,
    entity_type,
    entity_id,
    old_status,
    new_status,
    pharmacy_id,
    warehouse_id
  ) VALUES (
    v_actor_user_id,
    v_actor_email,
    p_action,
    v_actor_user_id,
    p_pharmacy_id,
    p_warehouse_id,
    jsonb_build_object(
      'status', p_old_status,
      'entity_type', p_entity_type,
      'entity_id', p_entity_id
    ),
    jsonb_build_object(
      'status', p_new_status,
      'entity_type', p_entity_type,
      'entity_id', p_entity_id,
      'actor_user_id', v_actor_user_id,
      'actor_role', COALESCE(v_actor_role, 'UNKNOWN'),
      'pharmacy_id', p_pharmacy_id,
      'warehouse_id', p_warehouse_id
    ),
    v_actor_user_id,
    COALESCE(v_actor_role, 'UNKNOWN'),
    p_entity_type,
    p_entity_id,
    p_old_status,
    p_new_status,
    p_pharmacy_id,
    p_warehouse_id
  );
END;
$$;

REVOKE ALL ON FUNCTION public.write_lifecycle_audit(uuid, text, text, text, text, text, text, uuid, uuid) FROM PUBLIC;

-- --------------------------------------------------------------------
-- 2. B2C lifecycle audit
-- --------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.handle_new_b2c_order_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_actor_role text;
BEGIN
  IF NEW.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
     OR NEW.request_id IS NOT NULL
     OR NEW.pharmacy_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT account_type
  INTO v_actor_role
  FROM public.profiles
  WHERE id = auth.uid();

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    COALESCE(v_actor_role, 'PUBLIC_USER'),
    'ORDER',
    NEW.id::text,
    'B2C_ORDER_CREATED_SUCCESS',
    NULL,
    NEW.status,
    NEW.pharmacy_id,
    NEW.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_ORDER_CONFIRMED_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_PRICE_ACCEPTED_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_PRICE_REJECTED_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_ORDER_REJECTED_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_ORDER_READY_FOR_PICKUP_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_ORDER_OUT_FOR_DELIVERY_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_account_type,
    'ORDER',
    v_updated.id::text,
    'B2C_ORDER_DELIVERED_SUCCESS',
    v_order.status,
    v_updated.status,
    v_updated.pharmacy_id,
    v_updated.warehouse_id
  );

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

-- --------------------------------------------------------------------
-- 3. B2B submit audit
-- --------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.submit_pharmacy_request(p_request_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_first_item RECORD;
  v_order RECORD;
  v_item_count INT;
  v_items JSONB;
BEGIN
  IF p_request_id IS NULL THEN
    RAISE EXCEPTION 'p_request_id is required' USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'PHARMACY'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active PHARMACY users can submit pharmacy requests'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request
  FROM public.requests
  WHERE id = p_request_id
  FOR UPDATE;

  IF v_request IS NULL THEN
    RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001';
  END IF;

  IF v_request.pharmacy_id IS DISTINCT FROM v_profile.pharmacy_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this pharmacy'
      USING ERRCODE = '42501';
  END IF;

  IF v_request.status IS DISTINCT FROM 'DRAFT' THEN
    RAISE EXCEPTION 'Invalid request status: expected DRAFT, got %', v_request.status
      USING ERRCODE = '22023';
  END IF;

  IF v_request.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Cannot submit request without warehouse_id' USING ERRCODE = '22023';
  END IF;

  SELECT COUNT(*) INTO v_item_count
  FROM public.request_items
  WHERE request_id = p_request_id;

  IF v_item_count <= 0 THEN
    RAISE EXCEPTION 'Cannot submit request without basket items' USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_first_item
  FROM public.request_items
  WHERE request_id = p_request_id
  ORDER BY line_no ASC, created_at ASC, id ASC
  LIMIT 1
  FOR UPDATE;

  PERFORM 1
  FROM public.request_items
  WHERE request_id = p_request_id
  FOR UPDATE;

  IF v_first_item.medicine_id IS NULL THEN
    RAISE EXCEPTION 'Cannot submit request with invalid first basket item' USING ERRCODE = '22023';
  END IF;

  IF COALESCE(v_first_item.quantity, 0) <= 0 THEN
    RAISE EXCEPTION 'Cannot submit request with non-positive first item quantity' USING ERRCODE = '22023';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM public.orders
    WHERE request_id = p_request_id
      AND order_type = 'PHARMACY_WAREHOUSE'
  ) THEN
    RAISE EXCEPTION 'Order already exists for this request'
      USING ERRCODE = '23505';
  END IF;

  UPDATE public.requests
  SET medicine_id = v_first_item.medicine_id,
      medicine_name = v_first_item.medicine_name,
      medicine_subtitle = COALESCE(v_first_item.medicine_subtitle, ''),
      quantity = v_first_item.quantity,
      unit = v_first_item.unit,
      status = 'PENDING',
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  INSERT INTO public.orders (
    order_type,
    request_id,
    pharmacy_id,
    warehouse_id,
    medicine_id,
    medicine_name,
    quantity,
    unit,
    status,
    fulfillment_type,
    customer_id,
    total_price_cents,
    currency,
    notes,
    created_at,
    updated_at,
    warehouse_name,
    supplier_name,
    eta_label,
    is_urgent
  ) VALUES (
    'PHARMACY_WAREHOUSE',
    v_request.id,
    v_request.pharmacy_id,
    v_request.warehouse_id,
    v_first_item.medicine_id,
    v_first_item.medicine_name,
    v_first_item.quantity,
    v_first_item.unit,
    'PENDING',
    'DELIVERY',
    NULL,
    NULL,
    'SAR',
    v_request.notes,
    now(),
    now(),
    v_request.warehouse_name,
    v_request.supplier_name,
    v_request.eta_label,
    v_request.priority = 'URGENT'
  )
  RETURNING * INTO v_order;

  UPDATE public.requests
  SET related_order_id = v_order.id,
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_profile.account_type,
    'REQUEST',
    v_request.id::text,
    'B2B_REQUEST_SUBMITTED_SUCCESS',
    'DRAFT',
    v_request.status,
    v_request.pharmacy_id,
    v_request.warehouse_id
  );

  PERFORM public.write_lifecycle_audit(
    auth.uid(),
    v_profile.account_type,
    'ORDER',
    v_order.id::text,
    'B2B_ORDER_CREATED_SUCCESS',
    NULL,
    v_order.status,
    v_order.pharmacy_id,
    v_order.warehouse_id
  );

  SELECT COALESCE(jsonb_agg(to_jsonb(ri) ORDER BY ri.line_no ASC, ri.created_at ASC, ri.id ASC), '[]'::jsonb)
  INTO v_items
  FROM public.request_items ri
  WHERE ri.request_id = p_request_id;

  RETURN jsonb_build_object(
    'request', to_jsonb(v_request),
    'order', to_jsonb(v_order),
    'items', v_items
  );
END;
$$;

GRANT EXECUTE ON FUNCTION public.submit_pharmacy_request(UUID) TO authenticated;

-- --------------------------------------------------------------------
-- 4. B2B lifecycle audit
-- --------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.warehouse_accept_b2b_request(
  p_request_id UUID,
  p_total_price_cents BIGINT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_order RECORD;
  v_old_request_status text;
  v_old_order_status text;
BEGIN
  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'WAREHOUSE'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active WAREHOUSE users can accept B2B requests'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001'; END IF;
  IF v_request.warehouse_id IS DISTINCT FROM v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse' USING ERRCODE = '42501';
  END IF;
  IF v_request.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected PENDING, got %', v_request.status USING ERRCODE = '22023';
  END IF;
  IF p_total_price_cents IS NULL OR p_total_price_cents < 0 THEN
    RAISE EXCEPTION 'Accepted price must be non-null and non-negative' USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request' USING ERRCODE = 'P0001'; END IF;
  IF v_order.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected PENDING, got %', v_order.status USING ERRCODE = '22023';
  END IF;

  v_old_request_status := v_request.status;
  v_old_order_status := v_order.status;

  UPDATE public.requests
  SET status = 'QUOTE_PENDING',
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  UPDATE public.orders
  SET status = 'QUOTE_PENDING',
      total_price_cents = p_total_price_cents,
      updated_at = now()
  WHERE id = v_order.id
  RETURNING * INTO v_order;

  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'REQUEST', v_request.id::text, 'B2B_WAREHOUSE_QUOTE_SENT_SUCCESS', v_old_request_status, v_request.status, v_request.pharmacy_id, v_request.warehouse_id);
  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'ORDER', v_order.id::text, 'B2B_ORDER_QUOTE_PENDING_SUCCESS', v_old_order_status, v_order.status, v_order.pharmacy_id, v_order.warehouse_id);

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

CREATE OR REPLACE FUNCTION public.pharmacy_accept_b2b_quote(p_request_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_order RECORD;
  v_old_request_status text;
  v_old_order_status text;
BEGIN
  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'PHARMACY'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active PHARMACY users can accept B2B quotes'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001'; END IF;
  IF v_request.pharmacy_id IS DISTINCT FROM v_profile.pharmacy_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this pharmacy' USING ERRCODE = '42501';
  END IF;
  IF v_request.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected QUOTE_PENDING, got %', v_request.status USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request' USING ERRCODE = 'P0001'; END IF;
  IF v_order.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected QUOTE_PENDING, got %', v_order.status USING ERRCODE = '22023';
  END IF;
  IF v_order.total_price_cents IS NULL OR v_order.total_price_cents < 0 THEN
    RAISE EXCEPTION 'Cannot accept B2B quote without a valid price' USING ERRCODE = '22023';
  END IF;

  v_old_request_status := v_request.status;
  v_old_order_status := v_order.status;

  UPDATE public.requests
  SET status = 'ACCEPTED',
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  UPDATE public.orders
  SET status = 'CONFIRMED',
      confirmed_at = now(),
      updated_at = now()
  WHERE id = v_order.id
  RETURNING * INTO v_order;

  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'REQUEST', v_request.id::text, 'B2B_PHARMACY_QUOTE_ACCEPTED_SUCCESS', v_old_request_status, v_request.status, v_request.pharmacy_id, v_request.warehouse_id);
  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'ORDER', v_order.id::text, 'B2B_ORDER_CONFIRMED_SUCCESS', v_old_order_status, v_order.status, v_order.pharmacy_id, v_order.warehouse_id);

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

CREATE OR REPLACE FUNCTION public.pharmacy_reject_b2b_quote(
  p_request_id UUID,
  p_rejection_reason TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_order RECORD;
  v_old_request_status text;
  v_old_order_status text;
BEGIN
  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'PHARMACY'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active PHARMACY users can reject B2B quotes'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001'; END IF;
  IF v_request.pharmacy_id IS DISTINCT FROM v_profile.pharmacy_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this pharmacy' USING ERRCODE = '42501';
  END IF;
  IF v_request.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected QUOTE_PENDING, got %', v_request.status USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request' USING ERRCODE = 'P0001'; END IF;
  IF v_order.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected QUOTE_PENDING, got %', v_order.status USING ERRCODE = '22023';
  END IF;

  v_old_request_status := v_request.status;
  v_old_order_status := v_order.status;

  UPDATE public.requests
  SET status = 'REJECTED',
      rejection_reason = p_rejection_reason,
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  UPDATE public.orders
  SET status = 'REJECTED',
      updated_at = now()
  WHERE id = v_order.id
  RETURNING * INTO v_order;

  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'REQUEST', v_request.id::text, 'B2B_PHARMACY_QUOTE_REJECTED_SUCCESS', v_old_request_status, v_request.status, v_request.pharmacy_id, v_request.warehouse_id);
  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'ORDER', v_order.id::text, 'B2B_ORDER_REJECTED_SUCCESS', v_old_order_status, v_order.status, v_order.pharmacy_id, v_order.warehouse_id);

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

CREATE OR REPLACE FUNCTION public.warehouse_reject_b2b_request(
  p_request_id UUID,
  p_rejection_reason TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_order RECORD;
  v_old_request_status text;
  v_old_order_status text;
BEGIN
  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'WAREHOUSE'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active WAREHOUSE users can reject B2B requests'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001'; END IF;
  IF v_request.warehouse_id IS DISTINCT FROM v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse' USING ERRCODE = '42501';
  END IF;
  IF v_request.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected PENDING, got %', v_request.status USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request' USING ERRCODE = 'P0001'; END IF;
  IF v_order.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected PENDING, got %', v_order.status USING ERRCODE = '22023';
  END IF;

  v_old_request_status := v_request.status;
  v_old_order_status := v_order.status;

  UPDATE public.requests
  SET status = 'REJECTED',
      rejection_reason = p_rejection_reason,
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  UPDATE public.orders
  SET status = 'REJECTED',
      updated_at = now()
  WHERE id = v_order.id
  RETURNING * INTO v_order;

  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'REQUEST', v_request.id::text, 'B2B_WAREHOUSE_REQUEST_REJECTED_SUCCESS', v_old_request_status, v_request.status, v_request.pharmacy_id, v_request.warehouse_id);
  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'ORDER', v_order.id::text, 'B2B_ORDER_REJECTED_SUCCESS', v_old_order_status, v_order.status, v_order.pharmacy_id, v_order.warehouse_id);

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

CREATE OR REPLACE FUNCTION public.warehouse_start_b2b_fulfillment(p_request_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_order RECORD;
  v_old_request_status text;
  v_old_order_status text;
BEGIN
  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'WAREHOUSE'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active WAREHOUSE users can start B2B fulfillment'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001'; END IF;
  IF v_request.warehouse_id IS DISTINCT FROM v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse' USING ERRCODE = '42501';
  END IF;
  IF v_request.status != 'ACCEPTED' THEN
    RAISE EXCEPTION 'Invalid request status: expected ACCEPTED, got %', v_request.status USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request' USING ERRCODE = 'P0001'; END IF;
  IF v_order.status != 'CONFIRMED' THEN
    RAISE EXCEPTION 'Invalid order status: expected CONFIRMED, got %', v_order.status USING ERRCODE = '22023';
  END IF;

  v_old_request_status := v_request.status;
  v_old_order_status := v_order.status;

  UPDATE public.requests
  SET status = 'IN_PROGRESS',
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  UPDATE public.orders
  SET status = 'IN_PROGRESS',
      updated_at = now()
  WHERE id = v_order.id
  RETURNING * INTO v_order;

  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'REQUEST', v_request.id::text, 'B2B_WAREHOUSE_FULFILLMENT_STARTED_SUCCESS', v_old_request_status, v_request.status, v_request.pharmacy_id, v_request.warehouse_id);
  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'ORDER', v_order.id::text, 'B2B_ORDER_IN_PROGRESS_SUCCESS', v_old_order_status, v_order.status, v_order.pharmacy_id, v_order.warehouse_id);

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

CREATE OR REPLACE FUNCTION public.warehouse_mark_b2b_delivered(
  p_request_id UUID,
  p_delivery_note TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile RECORD;
  v_request RECORD;
  v_order RECORD;
  v_old_request_status text;
  v_old_order_status text;
BEGIN
  SELECT * INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_profile IS NULL
      OR v_profile.account_type != 'WAREHOUSE'
      OR v_profile.is_active IS DISTINCT FROM true
      OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active WAREHOUSE users can deliver B2B requests'
      USING ERRCODE = '42501';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found' USING ERRCODE = 'P0001'; END IF;
  IF v_request.warehouse_id IS DISTINCT FROM v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse' USING ERRCODE = '42501';
  END IF;
  IF v_request.status != 'IN_PROGRESS' THEN
    RAISE EXCEPTION 'Invalid request status: expected IN_PROGRESS, got %', v_request.status USING ERRCODE = '22023';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request' USING ERRCODE = 'P0001'; END IF;
  IF v_order.status != 'IN_PROGRESS' THEN
    RAISE EXCEPTION 'Invalid order status: expected IN_PROGRESS, got %', v_order.status USING ERRCODE = '22023';
  END IF;

  v_old_request_status := v_request.status;
  v_old_order_status := v_order.status;

  UPDATE public.requests
  SET status = 'FULFILLED',
      updated_at = now()
  WHERE id = p_request_id
  RETURNING * INTO v_request;

  UPDATE public.orders
  SET status = 'DELIVERED',
      notes = COALESCE(p_delivery_note, notes),
      fulfilled_at = now(),
      updated_at = now()
  WHERE id = v_order.id
  RETURNING * INTO v_order;

  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'REQUEST', v_request.id::text, 'B2B_WAREHOUSE_DELIVERED_SUCCESS', v_old_request_status, v_request.status, v_request.pharmacy_id, v_request.warehouse_id);
  PERFORM public.write_lifecycle_audit(auth.uid(), v_profile.account_type, 'ORDER', v_order.id::text, 'B2B_ORDER_DELIVERED_SUCCESS', v_old_order_status, v_order.status, v_order.pharmacy_id, v_order.warehouse_id);

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.warehouse_accept_b2b_request(UUID, BIGINT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pharmacy_accept_b2b_quote(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pharmacy_reject_b2b_quote(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_reject_b2b_request(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_start_b2b_fulfillment(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_mark_b2b_delivered(UUID, TEXT) TO authenticated;
