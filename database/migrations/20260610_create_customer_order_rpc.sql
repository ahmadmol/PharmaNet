-- ============================================
-- Migration: create_customer_order RPC (B2C order creation)
-- Date: 2026-06-10
-- Purpose:
--   Move PUBLIC_USER customer-order creation from a direct table INSERT
--   (RLS policy customer_create_order) to a validated SECURITY DEFINER RPC,
--   matching the rest of the order lifecycle which is RPC-driven.
--
--   Behaviour is preserved 1:1 with the previous direct insert:
--     - order_type     = 'CUSTOMER_PHARMACY'
--     - status         = 'PENDING'
--     - total_price_cents = NULL (priced later by the pharmacy)
--     - warehouse_id   = NULL, request_id = NULL
--     - customer_id    = auth.uid()
--   The existing AFTER INSERT trigger (notify_pharmacy_on_new_order) and the
--   check_b2c_order / check_specific_pharmacy_has_id constraints continue to
--   apply because the insert targets public.orders directly.
-- ============================================

CREATE OR REPLACE FUNCTION public.create_customer_order(
    p_medicine_id text,
    p_medicine_name text,
    p_quantity integer,
    p_unit text,
    p_pharmacy_id text DEFAULT NULL,
    p_urgency text DEFAULT 'URGENT',
    p_request_scope text DEFAULT 'SPECIFIC_PHARMACY',
    p_fulfillment_type text DEFAULT 'PICKUP',
    p_delivery_address text DEFAULT NULL,
    p_delivery_latitude double precision DEFAULT NULL,
    p_delivery_longitude double precision DEFAULT NULL,
    p_delivery_phone text DEFAULT NULL,
    p_notes text DEFAULT NULL,
    p_prescription_url text DEFAULT NULL
)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_account_type public.profiles.account_type%TYPE;
    v_customer_id uuid := auth.uid();
    v_pharmacy_id text := NULLIF(btrim(COALESCE(p_pharmacy_id, '')), '');
    v_order public.orders%ROWTYPE;
BEGIN
    IF v_customer_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized: no authenticated user'
            USING ERRCODE = '42501';
    END IF;

    SELECT p.account_type
    INTO v_account_type
    FROM public.profiles p
    WHERE p.id = v_customer_id;

    IF v_account_type IS DISTINCT FROM 'PUBLIC_USER' THEN
        RAISE EXCEPTION 'Only PUBLIC_USER can create customer orders'
            USING ERRCODE = '42501';
    END IF;

    IF p_medicine_id IS NULL OR length(btrim(p_medicine_id)) = 0 THEN
        RAISE EXCEPTION 'medicine_id is required'
            USING ERRCODE = '22023';
    END IF;

    IF p_quantity IS NULL OR p_quantity <= 0 THEN
        RAISE EXCEPTION 'quantity must be greater than zero'
            USING ERRCODE = '22023';
    END IF;

    IF p_request_scope NOT IN ('SPECIFIC_PHARMACY', 'ALL_PHARMACIES') THEN
        RAISE EXCEPTION 'Invalid request_scope: %', p_request_scope
            USING ERRCODE = '22023';
    END IF;

    IF p_request_scope = 'SPECIFIC_PHARMACY' AND v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'pharmacy_id is required for SPECIFIC_PHARMACY'
            USING ERRCODE = '22023';
    END IF;

    -- ALL_PHARMACIES requests are open (pharmacy_id stays NULL) so the radar can surface them.
    IF p_request_scope = 'ALL_PHARMACIES' THEN
        v_pharmacy_id := NULL;
    END IF;

    INSERT INTO public.orders (
        medicine_id,
        medicine_name,
        quantity,
        unit,
        pharmacy_id,
        customer_id,
        order_type,
        status,
        urgency,
        request_scope,
        fulfillment_type,
        delivery_address,
        delivery_latitude,
        delivery_longitude,
        delivery_phone,
        notes,
        prescription_url,
        total_price_cents,
        currency,
        created_at,
        updated_at
    ) VALUES (
        p_medicine_id::uuid,
        p_medicine_name,
        p_quantity,
        p_unit,
        v_pharmacy_id,
        v_customer_id,
        'CUSTOMER_PHARMACY',
        'PENDING',
        COALESCE(p_urgency, 'URGENT'),
        p_request_scope,
        COALESCE(p_fulfillment_type, 'PICKUP'),
        p_delivery_address,
        p_delivery_latitude,
        p_delivery_longitude,
        p_delivery_phone,
        p_notes,
        p_prescription_url,
        NULL,
        'SAR',
        now(),
        now()
    )
    RETURNING * INTO v_order;

    RETURN v_order;
END;
$$;

REVOKE ALL ON FUNCTION public.create_customer_order(
    text, text, integer, text, text, text, text, text, text,
    double precision, double precision, text, text, text
) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION public.create_customer_order(
    text, text, integer, text, text, text, text, text, text,
    double precision, double precision, text, text, text
) TO authenticated;
