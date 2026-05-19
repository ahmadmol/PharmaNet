-- ============================================
-- MIGRATION: Pharmacy Radar Nearby Orders RPC
-- Date: 2026-05-13
-- Purpose: Provide nearby open customer orders for PHARMACY radar tab.
-- ============================================

CREATE OR REPLACE FUNCTION public.get_nearby_orders(
    p_latitude DOUBLE PRECISION,
    p_longitude DOUBLE PRECISION,
    p_radius_km DOUBLE PRECISION
)
RETURNS TABLE (
    id text,
    user_name text,
    medicine_name text,
    distance_km double precision
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_pharmacy_id text;
BEGIN
    IF p_radius_km IS NULL OR p_radius_km <= 0 THEN
        RAISE EXCEPTION 'p_radius_km must be greater than zero' USING ERRCODE = '22023';
    END IF;

    IF p_latitude IS NULL OR p_longitude IS NULL THEN
        RAISE EXCEPTION 'coordinates are required' USING ERRCODE = '22023';
    END IF;

    SELECT p.pharmacy_id
    INTO v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.pharmacy_id IS NOT NULL;

    IF v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only linked pharmacy users can read nearby orders'
            USING ERRCODE = '42501';
    END IF;

    RETURN QUERY
    SELECT
        o.id::text,
        COALESCE(NULLIF(cp.full_name, ''), NULLIF(cp.phone_number, ''), 'مستخدم')::text AS user_name,
        o.medicine_name,
        (
            6371.0 * 2.0 * ASIN(
                SQRT(
                    POWER(SIN(RADIANS((o.delivery_latitude - p_latitude) / 2.0)), 2.0)
                    + COS(RADIANS(p_latitude)) * COS(RADIANS(o.delivery_latitude))
                    * POWER(SIN(RADIANS((o.delivery_longitude - p_longitude) / 2.0)), 2.0)
                )
            )
        ) AS distance_km
    FROM public.orders o
    LEFT JOIN public.profiles cp
      ON cp.id = o.customer_id
    WHERE o.order_type = 'CUSTOMER_PHARMACY'
      AND o.status = 'PENDING'
      AND o.request_scope = 'ALL_PHARMACIES'
      AND o.delivery_latitude IS NOT NULL
      AND o.delivery_longitude IS NOT NULL
      AND (
          6371.0 * 2.0 * ASIN(
              SQRT(
                  POWER(SIN(RADIANS((o.delivery_latitude - p_latitude) / 2.0)), 2.0)
                  + COS(RADIANS(p_latitude)) * COS(RADIANS(o.delivery_latitude))
                  * POWER(SIN(RADIANS((o.delivery_longitude - p_longitude) / 2.0)), 2.0)
              )
          )
      ) <= p_radius_km
    ORDER BY distance_km ASC, o.created_at DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.get_nearby_orders(double precision, double precision, double precision) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_nearby_orders(double precision, double precision, double precision) TO authenticated;
