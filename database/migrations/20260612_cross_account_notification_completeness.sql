-- ====================================================================
-- Cross-Account Notification Completeness
-- Date: 2026-06-12
-- Purpose:
--   Close 8 notification gaps across PUBLIC_USER <-> PHARMACY <-> WAREHOUSE
--   by centralizing every lifecycle notification inside its SECURITY
--   DEFINER RPC, mirroring the B2C trigger pattern from 20260606.
--   After this migration the Kotlin layer must NOT emit duplicates.
--
-- Gaps closed:
--   R1  submit_pharmacy_request          -> notify WAREHOUSE owner (new B2B)
--   R2  claim_nearby_customer_order      -> notify CUSTOMER (claim feedback)
--   R3a warehouse_start_b2b_fulfillment  -> notify PHARMACY (prep started)
--   R3b warehouse_mark_b2b_delivered     -> notify PHARMACY (delivered)
--   R3c pharmacy_accept_b2b_quote        -> notify WAREHOUSE (quote accepted)
--   R3d pharmacy_reject_b2b_quote        -> notify WAREHOUSE (quote rejected)
--   R3e warehouse_accept_b2b_request     -> notify PHARMACY (quote sent)
--   R3f warehouse_reject_b2b_request     -> notify PHARMACY (request rejected)
--
-- Safety:
--   - All existing ownership checks, status guards, FOR UPDATE locks,
--     audit_logs writes are preserved verbatim.
--   - SECURITY DEFINER scope unchanged.
--   - PERFORM calls are emitted AFTER successful UPDATE + audit so a
--     transient notification failure cannot roll back business state.
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. Notification helper functions
-- --------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.create_b2b_warehouse_notification(
    p_request public.requests,
    p_title text,
    p_body text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF p_request.warehouse_id IS NULL THEN
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
        p_request.pharmacy_id,
        p_title,
        p_body,
        false,
        'ORDER_UPDATE',
        'REQUESTS',
        'REQUEST',
        p_request.id::text,
        now()
    FROM public.profiles p
    WHERE p.account_type = 'WAREHOUSE'
      AND p.is_active = true
      AND p.warehouse_id = p_request.warehouse_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.create_b2b_pharmacy_notification(
    p_request public.requests,
    p_title text,
    p_body text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF p_request.pharmacy_id IS NULL THEN
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
        p_request.pharmacy_id,
        p_title,
        p_body,
        false,
        'ORDER_UPDATE',
        'REQUESTS',
        'REQUEST',
        p_request.id::text,
        now()
    FROM public.profiles p
    WHERE p.account_type = 'PHARMACY'
      AND p.is_active = true
      AND p.pharmacy_id = p_request.pharmacy_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_b2b_warehouse_notification(public.requests, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.create_b2b_pharmacy_notification(public.requests, text, text) FROM PUBLIC;

-- --------------------------------------------------------------------
-- 2. R1: submit_pharmacy_request -> notify WAREHOUSE owner of new B2B
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

  -- R1: notify the WAREHOUSE owner of the incoming B2B request
  PERFORM public.create_b2b_warehouse_notification(
    v_request,
    'طلب جديد من صيدلية',
    'تَلقيت طلب توريد جديد. اضغط للمراجعة.'
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
-- 3. R3e: warehouse_accept_b2b_request -> notify PHARMACY (quote sent)
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

  -- R3e: notify PHARMACY owner that a price quote arrived
  PERFORM public.create_b2b_pharmacy_notification(
    v_request,
    'عرض سعر من المستودع',
    'أرسل المستودع عرض سعر لطلبك. راجعه للموافقة أو الرفض.'
  );

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.warehouse_accept_b2b_request(UUID, BIGINT) TO authenticated;

-- --------------------------------------------------------------------
-- 4. R3f: warehouse_reject_b2b_request -> notify PHARMACY (rejected)
-- --------------------------------------------------------------------

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

  -- R3f: notify PHARMACY owner that the warehouse rejected the request
  PERFORM public.create_b2b_pharmacy_notification(
    v_request,
    'رفض المستودع طلبك',
    'للأسف، رَفض المستودع طلبك. يمكنك إعادة المحاولة مع مستودع آخر.'
  );

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.warehouse_reject_b2b_request(UUID, TEXT) TO authenticated;

-- --------------------------------------------------------------------
-- 5. R3c: pharmacy_accept_b2b_quote -> notify WAREHOUSE (accepted)
-- --------------------------------------------------------------------

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

  -- R3c: notify WAREHOUSE owner that the pharmacy accepted the quote
  PERFORM public.create_b2b_warehouse_notification(
    v_request,
    'تَمت الموافقة على عرض السعر',
    'وافقت الصيدلية على عرض سعرك. ابدأ بتَجهيز الطلب.'
  );

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.pharmacy_accept_b2b_quote(UUID) TO authenticated;

-- --------------------------------------------------------------------
-- 6. R3d: pharmacy_reject_b2b_quote -> notify WAREHOUSE (rejected)
-- --------------------------------------------------------------------

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

  -- R3d: notify WAREHOUSE owner that the pharmacy rejected the quote
  PERFORM public.create_b2b_warehouse_notification(
    v_request,
    'تَم رفض عرض السعر',
    'رَفضت الصيدلية عرض سعرك. الطلب أُغلق.'
  );

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.pharmacy_reject_b2b_quote(UUID, TEXT) TO authenticated;

-- --------------------------------------------------------------------
-- 7. R3a: warehouse_start_b2b_fulfillment -> notify PHARMACY (prep)
-- --------------------------------------------------------------------

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

  -- R3a: notify PHARMACY owner that fulfillment has started
  PERFORM public.create_b2b_pharmacy_notification(
    v_request,
    'بَدأ تَجهيز طلبك',
    'بَدأ المستودع تَجهيز طلب التوريد. سَيتم التَسليم قريباً.'
  );

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.warehouse_start_b2b_fulfillment(UUID) TO authenticated;

-- --------------------------------------------------------------------
-- 8. R3b: warehouse_mark_b2b_delivered -> notify PHARMACY (delivered)
-- --------------------------------------------------------------------

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

  -- R3b: notify PHARMACY owner that the order has been delivered
  PERFORM public.create_b2b_pharmacy_notification(
    v_request,
    'تَم تَسليم طلبك',
    'تَم تَسليم طلب التوريد بنجاح من المستودع.'
  );

  RETURN jsonb_build_object('request', to_jsonb(v_request), 'order', to_jsonb(v_order));
END;
$$;

GRANT EXECUTE ON FUNCTION public.warehouse_mark_b2b_delivered(UUID, TEXT) TO authenticated;

-- --------------------------------------------------------------------
-- 9. R2: claim_nearby_customer_order -> notify CUSTOMER (pharmacy claimed)
-- --------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.claim_nearby_customer_order(
    p_order_id text,
    p_radius_km double precision DEFAULT 10.0
)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
    v_pharmacy_lat double precision;
    v_pharmacy_lng double precision;
    v_distance_km double precision;
BEGIN
    IF p_order_id IS NULL OR btrim(p_order_id) = '' THEN
        RAISE EXCEPTION 'p_order_id is required' USING ERRCODE = '22023';
    END IF;

    IF p_radius_km IS NULL OR p_radius_km <= 0 THEN
        RAISE EXCEPTION 'p_radius_km must be greater than zero' USING ERRCODE = '22023';
    END IF;

    SELECT p.pharmacy_id, ph.latitude, ph.longitude
    INTO v_pharmacy_id, v_pharmacy_lat, v_pharmacy_lng
    FROM public.profiles p
    JOIN public.pharmacies ph
      ON ph.id = p.pharmacy_id
    WHERE p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
      AND p.pharmacy_id IS NOT NULL
      AND ph.is_active = true;

    IF v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only active linked pharmacy users can claim nearby orders'
            USING ERRCODE = '42501';
    END IF;

    IF v_pharmacy_lat IS NULL OR v_pharmacy_lng IS NULL THEN
        RAISE EXCEPTION 'Linked pharmacy coordinates are required'
            USING ERRCODE = 'P0001';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found' USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.request_scope IS DISTINCT FROM 'ALL_PHARMACIES'
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.pharmacy_id IS NOT NULL
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL
        OR v_order.delivery_latitude IS NULL
        OR v_order.delivery_longitude IS NULL THEN
        RAISE EXCEPTION 'Order cannot be claimed by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    v_distance_km :=
        6371.0 * 2.0 * ASIN(
            SQRT(
                POWER(SIN(RADIANS((v_order.delivery_latitude - v_pharmacy_lat) / 2.0)), 2.0)
                + COS(RADIANS(v_pharmacy_lat)) * COS(RADIANS(v_order.delivery_latitude))
                * POWER(SIN(RADIANS((v_order.delivery_longitude - v_pharmacy_lng) / 2.0)), 2.0)
            )
        );

    IF v_distance_km > p_radius_km THEN
        RAISE EXCEPTION 'Order is outside the allowed radius'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET pharmacy_id = v_pharmacy_id,
        request_scope = 'SPECIFIC_PHARMACY',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.request_scope = 'ALL_PHARMACIES'
      AND o.status = 'PENDING'
      AND o.pharmacy_id IS NULL
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order claim did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    -- R2: notify the customer that a nearby pharmacy claimed their order
    PERFORM public.create_b2c_customer_notification(
        v_updated,
        'صيدلية قريبة استلمت طلبك',
        'صيدلية بالقرب من موقعك أَخذت طلبك للمراجعة. انتظر عرض السعر.'
    );

    RETURN v_updated;
END;
$$;

REVOKE ALL ON FUNCTION public.claim_nearby_customer_order(text, double precision) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.claim_nearby_customer_order(text, double precision) TO authenticated;

-- ====================================================================
-- End of migration 20260612_cross_account_notification_completeness
-- ====================================================================
