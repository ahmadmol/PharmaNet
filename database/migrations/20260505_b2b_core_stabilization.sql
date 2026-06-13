-- ====================================================================
-- B2B Core Stabilization
-- Purpose: Stabilize PHARMACY -> Request -> WAREHOUSE -> Order lifecycle
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. Schema corrections
-- --------------------------------------------------------------------

ALTER TABLE public.requests
  ADD COLUMN IF NOT EXISTS medicine_id UUID REFERENCES public.medicines(id),
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE public.orders
  ALTER COLUMN request_id DROP NOT NULL;

ALTER TABLE public.orders
  DROP CONSTRAINT IF EXISTS check_b2b_order,
  DROP CONSTRAINT IF EXISTS check_b2c_order;

ALTER TABLE public.orders
  ADD CONSTRAINT check_b2b_order
  CHECK (
    order_type != 'PHARMACY_WAREHOUSE'
    OR (
      request_id IS NOT NULL
      AND pharmacy_id IS NOT NULL
      AND warehouse_id IS NOT NULL
      AND medicine_id IS NOT NULL
      AND customer_id IS NULL
      AND fulfillment_type = 'DELIVERY'
    )
  );

ALTER TABLE public.orders
  ADD CONSTRAINT check_b2c_order
  CHECK (
    order_type != 'CUSTOMER_PHARMACY'
    OR (
      customer_id IS NOT NULL
      AND warehouse_id IS NULL
      AND request_id IS NULL
    )
  );

DO $$
BEGIN
  IF EXISTS (
    SELECT request_id
    FROM public.orders
    WHERE order_type = 'PHARMACY_WAREHOUSE'
      AND request_id IS NOT NULL
    GROUP BY request_id
    HAVING COUNT(*) > 1
  ) THEN
    RAISE EXCEPTION 'Cannot create unique B2B request/order index: duplicate PHARMACY_WAREHOUSE orders exist for the same request_id.';
  END IF;
END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_b2b_request_unique
  ON public.orders(request_id)
  WHERE order_type = 'PHARMACY_WAREHOUSE' AND request_id IS NOT NULL;

-- --------------------------------------------------------------------
-- 2. Status compatibility fix
-- --------------------------------------------------------------------

UPDATE public.requests
SET status = 'PENDING',
    updated_at = now()
WHERE status = 'SUBMITTED';

ALTER TABLE public.requests
  ALTER COLUMN status SET DEFAULT 'DRAFT';

-- --------------------------------------------------------------------
-- 3. Remove old draft-time order creation
-- --------------------------------------------------------------------

DROP TRIGGER IF EXISTS create_order_from_request ON public.requests;
DROP FUNCTION IF EXISTS public.create_order_from_request();
DROP FUNCTION IF EXISTS public.create_order_for_existing_request(TEXT);
DROP FUNCTION IF EXISTS public.create_order_for_existing_request(UUID);

-- --------------------------------------------------------------------
-- 4. Clean up legacy TEXT-based RPCs (if any)
-- --------------------------------------------------------------------

DROP FUNCTION IF EXISTS public.submit_pharmacy_request(TEXT);
DROP FUNCTION IF EXISTS public.warehouse_accept_b2b_request(TEXT, BIGINT, TEXT);
DROP FUNCTION IF EXISTS public.warehouse_accept_b2b_request(TEXT, BIGINT);
DROP FUNCTION IF EXISTS public.warehouse_reject_b2b_request(TEXT, TEXT);
DROP FUNCTION IF EXISTS public.warehouse_start_b2b_fulfillment(TEXT);
DROP FUNCTION IF EXISTS public.warehouse_mark_b2b_delivered(TEXT, TEXT);
DROP FUNCTION IF EXISTS public.warehouse_accept_b2b_request(UUID, BIGINT, TEXT);
DROP FUNCTION IF EXISTS public.warehouse_accept_b2b_request(UUID, BIGINT);
DROP FUNCTION IF EXISTS public.pharmacy_accept_b2b_quote(UUID);
DROP FUNCTION IF EXISTS public.pharmacy_reject_b2b_quote(UUID, TEXT);

-- --------------------------------------------------------------------
-- 5. B2B lifecycle RPCs (UUID-based)
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
  SELECT * INTO v_profile FROM public.profiles WHERE id = auth.uid();
  IF v_profile IS NULL OR v_profile.account_type != 'WAREHOUSE' OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only WAREHOUSE users can accept B2B requests';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found'; END IF;
  IF v_request.warehouse_id != v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse';
  END IF;
  IF v_request.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected PENDING, got %', v_request.status;
  END IF;
  IF p_total_price_cents IS NULL OR p_total_price_cents < 0 THEN
    RAISE EXCEPTION 'Accepted price must be non-null and non-negative';
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request'; END IF;
  IF v_order.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected PENDING, got %', v_order.status;
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
  SELECT * INTO v_profile FROM public.profiles WHERE id = auth.uid();
  IF v_profile IS NULL OR v_profile.account_type != 'PHARMACY' OR v_profile.pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only PHARMACY users can accept B2B quotes';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found'; END IF;
  IF v_request.pharmacy_id != v_profile.pharmacy_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this pharmacy';
  END IF;
  IF v_request.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected QUOTE_PENDING, got %', v_request.status;
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request'; END IF;
  IF v_order.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected QUOTE_PENDING, got %', v_order.status;
  END IF;
  IF v_order.total_price_cents IS NULL OR v_order.total_price_cents < 0 THEN
    RAISE EXCEPTION 'Cannot accept B2B quote without a valid price';
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
  SELECT * INTO v_profile FROM public.profiles WHERE id = auth.uid();
  IF v_profile IS NULL OR v_profile.account_type != 'PHARMACY' OR v_profile.pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only PHARMACY users can reject B2B quotes';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found'; END IF;
  IF v_request.pharmacy_id != v_profile.pharmacy_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this pharmacy';
  END IF;
  IF v_request.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected QUOTE_PENDING, got %', v_request.status;
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request'; END IF;
  IF v_order.status != 'QUOTE_PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected QUOTE_PENDING, got %', v_order.status;
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
  SELECT * INTO v_profile FROM public.profiles WHERE id = auth.uid();
  IF v_profile IS NULL OR v_profile.account_type != 'WAREHOUSE' OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only WAREHOUSE users can reject B2B requests';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found'; END IF;
  IF v_request.warehouse_id != v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse';
  END IF;
  IF v_request.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid request status: expected PENDING, got %', v_request.status;
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request'; END IF;
  IF v_order.status != 'PENDING' THEN
    RAISE EXCEPTION 'Invalid order status: expected PENDING, got %', v_order.status;
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
  SELECT * INTO v_profile FROM public.profiles WHERE id = auth.uid();
  IF v_profile IS NULL OR v_profile.account_type != 'WAREHOUSE' OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only WAREHOUSE users can start B2B fulfillment';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found'; END IF;
  IF v_request.warehouse_id != v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse';
  END IF;
  IF v_request.status != 'ACCEPTED' THEN
    RAISE EXCEPTION 'Invalid request status: expected ACCEPTED, got %', v_request.status;
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request'; END IF;
  IF v_order.status != 'CONFIRMED' THEN
    RAISE EXCEPTION 'Invalid order status: expected CONFIRMED, got %', v_order.status;
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
  SELECT * INTO v_profile FROM public.profiles WHERE id = auth.uid();
  IF v_profile IS NULL OR v_profile.account_type != 'WAREHOUSE' OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only WAREHOUSE users can deliver B2B requests';
  END IF;

  SELECT * INTO v_request FROM public.requests WHERE id = p_request_id FOR UPDATE;
  IF v_request IS NULL THEN RAISE EXCEPTION 'Request not found'; END IF;
  IF v_request.warehouse_id != v_profile.warehouse_id THEN
    RAISE EXCEPTION 'Unauthorized: request does not belong to this warehouse';
  END IF;
  IF v_request.status != 'IN_PROGRESS' THEN
    RAISE EXCEPTION 'Invalid request status: expected IN_PROGRESS, got %', v_request.status;
  END IF;

  SELECT * INTO v_order
  FROM public.orders
  WHERE request_id = p_request_id
    AND order_type = 'PHARMACY_WAREHOUSE'
  FOR UPDATE;

  IF v_order IS NULL THEN RAISE EXCEPTION 'B2B order not found for request'; END IF;
  IF v_order.status != 'IN_PROGRESS' THEN
    RAISE EXCEPTION 'Invalid order status: expected IN_PROGRESS, got %', v_order.status;
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

GRANT EXECUTE ON FUNCTION public.submit_pharmacy_request(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_accept_b2b_request(UUID, BIGINT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pharmacy_accept_b2b_quote(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pharmacy_reject_b2b_quote(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_reject_b2b_request(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_start_b2b_fulfillment(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.warehouse_mark_b2b_delivered(UUID, TEXT) TO authenticated;

-- --------------------------------------------------------------------
-- 6. RLS tightening
-- --------------------------------------------------------------------

DROP POLICY IF EXISTS "requests_pharmacy_insert" ON public.requests;
DROP POLICY IF EXISTS "requests_pharmacy_update_own" ON public.requests;
DROP POLICY IF EXISTS "requests_warehouse_update_status" ON public.requests;
DROP POLICY IF EXISTS "requests_pharmacy_insert_draft" ON public.requests;
DROP POLICY IF EXISTS "requests_pharmacy_update_own_draft" ON public.requests;

CREATE POLICY "requests_pharmacy_insert_draft"
ON public.requests
FOR INSERT
TO authenticated
WITH CHECK (
  pharmacy_id = (
    SELECT pharmacy_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
  )
  AND status = 'DRAFT'
);

CREATE POLICY "requests_pharmacy_update_own_draft"
ON public.requests
FOR UPDATE
TO authenticated
USING (
  pharmacy_id = (
    SELECT pharmacy_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
  )
  AND status = 'DRAFT'
)
WITH CHECK (
  pharmacy_id = (
    SELECT pharmacy_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
  )
  AND status = 'DRAFT'
);

DROP POLICY IF EXISTS "warehouse_create_b2b" ON public.orders;
DROP POLICY IF EXISTS "warehouse_update_b2b" ON public.orders;
DROP POLICY IF EXISTS "orders_admin_read" ON public.orders;

CREATE POLICY "orders_admin_read"
ON public.orders
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'ADMIN'
      AND is_active = true
  )
);

DROP POLICY IF EXISTS "pharmacy_read_active_warehouses" ON public.warehouses;

CREATE POLICY "pharmacy_read_active_warehouses"
ON public.warehouses
FOR SELECT
TO authenticated
USING (
  is_active = true
  AND EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
      AND is_active = true
  )
);
