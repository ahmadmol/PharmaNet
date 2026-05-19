-- Pharmacy radar safe claim/detail support.
-- Allows linked active pharmacies to inspect and atomically claim nearby open
-- ALL_PHARMACIES customer orders before the existing confirm/reject lifecycle.

CREATE INDEX IF NOT EXISTS idx_orders_radar_open_with_location
ON public.orders (request_scope, status, order_type)
WHERE order_type = 'CUSTOMER_PHARMACY'
  AND request_scope = 'ALL_PHARMACIES'
  AND status = 'PENDING'
  AND delivery_latitude IS NOT NULL
  AND delivery_longitude IS NOT NULL;

DROP FUNCTION IF EXISTS public.get_nearby_order_detail(text, double precision);

CREATE OR REPLACE FUNCTION public.get_nearby_order_detail(
    p_order_id text,
    p_radius_km double precision DEFAULT 10.0
)
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
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
    v_pharmacy_lat double precision;
    v_pharmacy_lng double precision;
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
        RAISE EXCEPTION 'Only active linked pharmacy users can read nearby order detail'
            USING ERRCODE = '42501';
    END IF;

    IF v_pharmacy_lat IS NULL OR v_pharmacy_lng IS NULL THEN
        RAISE EXCEPTION 'Linked pharmacy coordinates are required'
            USING ERRCODE = 'P0001';
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
      AND o.customer_id IS NOT NULL
      AND o.warehouse_id IS NULL
      AND o.request_id IS NULL
      AND (
          (
              o.request_scope = 'SPECIFIC_PHARMACY'
              AND o.pharmacy_id = v_pharmacy_id
          )
          OR
          (
              o.request_scope = 'ALL_PHARMACIES'
              AND o.status = 'PENDING'
              AND o.pharmacy_id IS NULL
              AND o.delivery_latitude IS NOT NULL
              AND o.delivery_longitude IS NOT NULL
              AND (
                  6371.0 * 2.0 * ASIN(
                      SQRT(
                          POWER(SIN(RADIANS((o.delivery_latitude - v_pharmacy_lat) / 2.0)), 2.0)
                          + COS(RADIANS(v_pharmacy_lat)) * COS(RADIANS(o.delivery_latitude))
                          * POWER(SIN(RADIANS((o.delivery_longitude - v_pharmacy_lng) / 2.0)), 2.0)
                      )
                  )
              ) <= p_radius_km
          )
      );
END;
$$;

REVOKE ALL ON FUNCTION public.get_nearby_order_detail(text, double precision) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_nearby_order_detail(text, double precision) TO authenticated;

DROP FUNCTION IF EXISTS public.claim_nearby_customer_order(text, double precision);

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

    RETURN v_updated;
END;
$$;

REVOKE ALL ON FUNCTION public.claim_nearby_customer_order(text, double precision) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.claim_nearby_customer_order(text, double precision) TO authenticated;
