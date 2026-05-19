-- Batch B safe fix: include prescription_url in pharmacy customer order reads
-- and provide an exact detail RPC instead of broad list filtering.

DROP FUNCTION IF EXISTS public.get_pharmacy_customer_orders();

CREATE OR REPLACE FUNCTION public.get_pharmacy_customer_orders()
RETURNS TABLE (
    id text,
    customer_id text,
    customer_name text,
    medicine_id text,
    medicine_name text,
    quantity integer,
    unit text,
    status text,
    fulfillment_type text,
    delivery_address text,
    delivery_phone text,
    prescription_url text,
    notes text,
    total_price_cents bigint,
    currency text,
    urgency text,
    request_scope text,
    created_at timestamptz,
    updated_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_pharmacy_id text;
BEGIN
    SELECT p.pharmacy_id
    INTO v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.pharmacy_id IS NOT NULL;

    IF v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only linked pharmacy users can read pharmacy customer orders'
            USING ERRCODE = '42501';
    END IF;

    RETURN QUERY
    SELECT
        o.id::text,
        o.customer_id::text,
        COALESCE(NULLIF(cp.full_name, ''), NULLIF(cp.phone_number, ''), NULL)::text AS customer_name,
        o.medicine_id::text,
        o.medicine_name,
        o.quantity,
        o.unit,
        o.status,
        o.fulfillment_type,
        o.delivery_address,
        o.delivery_phone,
        o.prescription_url,
        o.notes,
        o.total_price_cents,
        o.currency,
        o.urgency,
        o.request_scope,
        o.created_at,
        o.updated_at
    FROM public.orders o
    LEFT JOIN public.profiles cp
      ON cp.id = o.customer_id
    WHERE o.order_type = 'CUSTOMER_PHARMACY'
      AND o.request_scope = 'SPECIFIC_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    ORDER BY o.created_at DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.get_pharmacy_customer_orders() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_pharmacy_customer_orders() TO authenticated;

CREATE OR REPLACE FUNCTION public.get_pharmacy_customer_order_detail(p_order_id text)
RETURNS TABLE (
    id text,
    customer_id text,
    customer_name text,
    medicine_id text,
    medicine_name text,
    quantity integer,
    unit text,
    status text,
    fulfillment_type text,
    delivery_address text,
    delivery_phone text,
    prescription_url text,
    notes text,
    total_price_cents bigint,
    currency text,
    urgency text,
    request_scope text,
    created_at timestamptz,
    updated_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_pharmacy_id text;
BEGIN
    SELECT p.pharmacy_id
    INTO v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.pharmacy_id IS NOT NULL;

    IF v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only linked pharmacy users can read pharmacy customer order details'
            USING ERRCODE = '42501';
    END IF;

    RETURN QUERY
    SELECT
        o.id::text,
        o.customer_id::text,
        COALESCE(NULLIF(cp.full_name, ''), NULLIF(cp.phone_number, ''), NULL)::text AS customer_name,
        o.medicine_id::text,
        o.medicine_name,
        o.quantity,
        o.unit,
        o.status,
        o.fulfillment_type,
        o.delivery_address,
        o.delivery_phone,
        o.prescription_url,
        o.notes,
        o.total_price_cents,
        o.currency,
        o.urgency,
        o.request_scope,
        o.created_at,
        o.updated_at
    FROM public.orders o
    LEFT JOIN public.profiles cp
      ON cp.id = o.customer_id
    WHERE o.id = p_order_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.request_scope = 'SPECIFIC_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id;
END;
$$;

REVOKE ALL ON FUNCTION public.get_pharmacy_customer_order_detail(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_pharmacy_customer_order_detail(text) TO authenticated;

DROP POLICY IF EXISTS prescriptions_authenticated_delete ON storage.objects;
CREATE POLICY prescriptions_authenticated_delete
ON storage.objects
FOR DELETE
TO authenticated
USING (
  bucket_id = 'prescriptions'
  AND name LIKE auth.uid()::TEXT || '\_%' ESCAPE '\'
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'PUBLIC_USER'
      AND p.is_active = true
  )
);
