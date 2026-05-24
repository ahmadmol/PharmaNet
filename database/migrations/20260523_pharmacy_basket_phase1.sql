-- ====================================================================
-- PHARMACY Basket Phase 1: DB/RPC/RLS only
-- Purpose:
--   - Keep pharmacy request INSERT as DRAFT only.
--   - Store basket lines in request_items.
--   - Create exactly one PHARMACY_WAREHOUSE order only from submit RPC.
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. Disable legacy insert-time order creation
-- --------------------------------------------------------------------

DROP TRIGGER IF EXISTS create_order_from_request ON public.requests;
DROP FUNCTION IF EXISTS public.create_order_from_request();
DROP FUNCTION IF EXISTS public.create_order_for_existing_request(TEXT);
DROP FUNCTION IF EXISTS public.create_order_for_existing_request(UUID);

-- --------------------------------------------------------------------
-- 2. Request basket items
-- --------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.request_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL REFERENCES public.requests(id) ON DELETE CASCADE,
  line_no INT NOT NULL,
  medicine_id UUID NOT NULL REFERENCES public.medicines(id),
  medicine_name TEXT NOT NULL,
  medicine_subtitle TEXT DEFAULT '',
  quantity INT NOT NULL CHECK (quantity > 0),
  unit TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_request_items_request_line_no
  ON public.request_items(request_id, line_no);

CREATE UNIQUE INDEX IF NOT EXISTS idx_request_items_request_medicine
  ON public.request_items(request_id, medicine_id);

CREATE INDEX IF NOT EXISTS idx_request_items_request_id
  ON public.request_items(request_id);

CREATE OR REPLACE FUNCTION public.set_request_items_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS set_request_items_updated_at ON public.request_items;
CREATE TRIGGER set_request_items_updated_at
BEFORE UPDATE ON public.request_items
FOR EACH ROW
EXECUTE FUNCTION public.set_request_items_updated_at();

-- --------------------------------------------------------------------
-- 3. RLS: requests visibility and request_items ownership
-- --------------------------------------------------------------------

ALTER TABLE public.requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.request_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "requests_warehouse_read_incoming" ON public.requests;
CREATE POLICY "requests_warehouse_read_incoming"
ON public.requests
FOR SELECT
TO authenticated
USING (
  status <> 'DRAFT'
  AND warehouse_id = (
    SELECT warehouse_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'WAREHOUSE'
      AND is_active = true
  )
);

DROP POLICY IF EXISTS "requests_pharmacy_insert" ON public.requests;
DROP POLICY IF EXISTS "requests_pharmacy_update_own" ON public.requests;
DROP POLICY IF EXISTS "requests_pharmacy_insert_draft" ON public.requests;
DROP POLICY IF EXISTS "requests_pharmacy_update_own_draft" ON public.requests;

CREATE POLICY "requests_pharmacy_insert_draft"
ON public.requests
FOR INSERT
TO authenticated
WITH CHECK (
  status = 'DRAFT'
  AND pharmacy_id = (
    SELECT pharmacy_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
      AND is_active = true
  )
);

CREATE POLICY "requests_pharmacy_update_own_draft"
ON public.requests
FOR UPDATE
TO authenticated
USING (
  status = 'DRAFT'
  AND pharmacy_id = (
    SELECT pharmacy_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
      AND is_active = true
  )
)
WITH CHECK (
  status = 'DRAFT'
  AND pharmacy_id = (
    SELECT pharmacy_id
    FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'PHARMACY'
      AND is_active = true
  )
);

DROP POLICY IF EXISTS "request_items_pharmacy_select_own" ON public.request_items;
DROP POLICY IF EXISTS "request_items_pharmacy_insert_draft" ON public.request_items;
DROP POLICY IF EXISTS "request_items_pharmacy_update_draft" ON public.request_items;
DROP POLICY IF EXISTS "request_items_pharmacy_delete_draft" ON public.request_items;
DROP POLICY IF EXISTS "request_items_warehouse_select_submitted" ON public.request_items;
DROP POLICY IF EXISTS "request_items_admin_select_all" ON public.request_items;

CREATE POLICY "request_items_pharmacy_select_own"
ON public.request_items
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.requests r
    JOIN public.profiles p ON p.pharmacy_id = r.pharmacy_id
    WHERE r.id = request_items.request_id
      AND p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
  )
);

CREATE POLICY "request_items_pharmacy_insert_draft"
ON public.request_items
FOR INSERT
TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM public.requests r
    JOIN public.profiles p ON p.pharmacy_id = r.pharmacy_id
    WHERE r.id = request_items.request_id
      AND r.status = 'DRAFT'
      AND p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
  )
);

CREATE POLICY "request_items_pharmacy_update_draft"
ON public.request_items
FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.requests r
    JOIN public.profiles p ON p.pharmacy_id = r.pharmacy_id
    WHERE r.id = request_items.request_id
      AND r.status = 'DRAFT'
      AND p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM public.requests r
    JOIN public.profiles p ON p.pharmacy_id = r.pharmacy_id
    WHERE r.id = request_items.request_id
      AND r.status = 'DRAFT'
      AND p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
  )
);

CREATE POLICY "request_items_pharmacy_delete_draft"
ON public.request_items
FOR DELETE
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.requests r
    JOIN public.profiles p ON p.pharmacy_id = r.pharmacy_id
    WHERE r.id = request_items.request_id
      AND r.status = 'DRAFT'
      AND p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
  )
);

CREATE POLICY "request_items_warehouse_select_submitted"
ON public.request_items
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.requests r
    JOIN public.profiles p ON p.warehouse_id = r.warehouse_id
    WHERE r.id = request_items.request_id
      AND r.status <> 'DRAFT'
      AND p.id = auth.uid()
      AND p.account_type = 'WAREHOUSE'
      AND p.is_active = true
  )
);

CREATE POLICY "request_items_admin_select_all"
ON public.request_items
FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'ADMIN'
      AND p.is_active = true
  )
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.request_items TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.requests TO authenticated;

-- --------------------------------------------------------------------
-- 4. Submit DRAFT pharmacy request and create one linked B2B order
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

GRANT EXECUTE ON FUNCTION public.submit_pharmacy_request(UUID) TO authenticated;

-- --------------------------------------------------------------------
-- 5. DB verification guide
-- --------------------------------------------------------------------
-- Manual verification after applying this migration:
-- 1. Insert an own PHARMACY request with status = 'DRAFT'; no order should be created.
-- 2. Insert two public.request_items rows for that request.
-- 3. SELECT public.submit_pharmacy_request('<request-id>'::uuid);
-- 4. Verify:
--    - requests.status = 'PENDING'
--    - exactly one PHARMACY_WAREHOUSE order exists for request_id
--    - requests.related_order_id = orders.id
--    - first request_items row is mirrored into requests/orders legacy fields
--    - all request_items rows remain linked to the request
--    - warehouse cannot read DRAFT request/items, but can read submitted request/items
--    - non-owner/non-PHARMACY submit fails
--    - retry submit fails and creates no duplicate order
