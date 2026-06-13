-- Backend contract repair:
-- - Re-align public pharmacy/customer order RPCs after location -> formatted_address.
-- - Enforce future app_notifications rows with user_id and route data for order/request notifications.
-- - Require active linked profiles for B2B lifecycle RPC actors and B2C notification recipients.

-- --------------------------------------------------------------------
-- 1. Public pharmacy discovery RPCs
-- --------------------------------------------------------------------

DROP FUNCTION IF EXISTS public.get_public_pharmacies_for_medicine(uuid);

CREATE OR REPLACE FUNCTION public.get_public_pharmacies_for_medicine(p_medicine_id uuid)
RETURNS TABLE (
    pharmacy_id text,
    pharmacy_name text,
    location text,
    supports_delivery boolean,
    supports_pickup boolean,
    is_on_duty boolean,
    availability_status text,
    estimated_time_label text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF p_medicine_id IS NULL THEN
        RAISE EXCEPTION 'p_medicine_id is required' USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT
        ph.id::text AS pharmacy_id,
        ph.name AS pharmacy_name,
        COALESCE(ph.formatted_address, '') AS location,
        COALESCE(ph.supports_delivery, true) AS supports_delivery,
        COALESCE(ph.supports_pickup, true) AS supports_pickup,
        COALESCE(ph.is_on_duty, false) AS is_on_duty,
        'NEEDS_CONFIRMATION'::text AS availability_status,
        'Awaiting pharmacy confirmation'::text AS estimated_time_label
    FROM public.pharmacies ph
    WHERE ph.is_active = true
    ORDER BY ph.name;
END;
$$;

DROP FUNCTION IF EXISTS public.get_public_pharmacies();

CREATE OR REPLACE FUNCTION public.get_public_pharmacies()
RETURNS TABLE (
    pharmacy_id text,
    pharmacy_name text,
    location text,
    area text,
    city text,
    district text,
    supports_delivery boolean,
    supports_pickup boolean,
    is_on_duty boolean,
    availability_status text,
    distance_label text,
    estimated_time_label text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id::text AS pharmacy_id,
        p.name AS pharmacy_name,
        COALESCE(p.formatted_address, '') AS location,
        NULL::text AS area,
        NULL::text AS city,
        NULL::text AS district,
        COALESCE(p.supports_delivery, false) AS supports_delivery,
        COALESCE(p.supports_pickup, true) AS supports_pickup,
        COALESCE(p.is_on_duty, false) AS is_on_duty,
        'UNKNOWN'::text AS availability_status,
        NULL::text AS distance_label,
        NULL::text AS estimated_time_label
    FROM public.pharmacies p
    WHERE p.is_active = true
    ORDER BY p.name;
END;
$$;

DROP FUNCTION IF EXISTS public.get_my_customer_orders();

CREATE OR REPLACE FUNCTION public.get_my_customer_orders()
RETURNS TABLE (
    id text,
    medicine_id text,
    medicine_name text,
    quantity integer,
    unit text,
    status text,
    order_type text,
    fulfillment_type text,
    pharmacy_id text,
    warehouse_id text,
    customer_id uuid,
    request_id text,
    total_price_cents bigint,
    currency text,
    delivery_address text,
    delivery_phone text,
    notes text,
    created_at timestamptz,
    updated_at timestamptz,
    confirmed_at timestamptz,
    fulfilled_at timestamptz,
    warehouse_name text,
    supplier_name text,
    eta_label text,
    is_urgent boolean,
    urgency text,
    request_scope text,
    pharmacy_name text,
    pharmacy_location text
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        o.id,
        o.medicine_id::text,
        o.medicine_name,
        o.quantity,
        o.unit,
        o.status,
        o.order_type,
        o.fulfillment_type,
        o.pharmacy_id,
        o.warehouse_id,
        o.customer_id,
        o.request_id,
        o.total_price_cents,
        o.currency,
        o.delivery_address,
        o.delivery_phone,
        o.notes,
        o.created_at,
        o.updated_at,
        o.confirmed_at,
        o.fulfilled_at,
        NULL::text AS warehouse_name,
        NULL::text AS supplier_name,
        NULL::text AS eta_label,
        (o.urgency = 'URGENT') AS is_urgent,
        o.urgency,
        o.request_scope,
        ph.name AS pharmacy_name,
        ph.formatted_address AS pharmacy_location
    FROM public.orders o
    LEFT JOIN public.pharmacies ph ON ph.id = o.pharmacy_id
    WHERE o.customer_id = auth.uid()
      AND o.order_type = 'CUSTOMER_PHARMACY'
    ORDER BY o.created_at DESC;
$$;

GRANT EXECUTE ON FUNCTION public.get_public_pharmacies_for_medicine(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_public_pharmacies() TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_my_customer_orders() TO authenticated;

-- --------------------------------------------------------------------
-- 2. app_notifications future-row contract
-- --------------------------------------------------------------------

DO $$
BEGIN
    ALTER TABLE public.app_notifications
        ADD CONSTRAINT app_notifications_user_id_present
        CHECK (user_id IS NOT NULL)
        NOT VALID;
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE public.app_notifications
        ADD CONSTRAINT app_notifications_order_request_route_present
        CHECK (
            (
                category IS NULL
                OR category NOT IN ('ORDERS', 'REQUESTS')
            )
            AND COALESCE(type, '') <> 'ORDER_UPDATE'
            OR (destination IS NOT NULL AND destination_id IS NOT NULL)
        )
        NOT VALID;
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- --------------------------------------------------------------------
-- 3. B2C notification recipient helpers
-- --------------------------------------------------------------------

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
    )
    SELECT
        p.id,
        p_order.pharmacy_id,
        p_title,
        p_body,
        false,
        'ORDER_UPDATE',
        'ORDERS',
        'ORDER',
        p_order.id::text,
        now()
    FROM public.profiles p
    WHERE p.id = p_order.customer_id
      AND p.account_type = 'PUBLIC_USER'
      AND p.is_active = true;
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

-- --------------------------------------------------------------------
-- 4. B2B lifecycle RPC active-profile hardening
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

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.warehouse_accept_b2b_request(UUID, BIGINT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pharmacy_accept_b2b_quote(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pharmacy_reject_b2b_quote(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_reject_b2b_request(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_start_b2b_fulfillment(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_mark_b2b_delivered(UUID, TEXT) TO authenticated;
