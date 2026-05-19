-- ============================================
-- Migration: Warehouse self location update RPC
-- Date: 2026-05-16
-- Purpose:
--   - Allow active WAREHOUSE users to update only their own warehouse location fields
--   - Scope ownership through profiles.warehouse_id
-- ============================================

CREATE OR REPLACE FUNCTION public.update_my_warehouse_location(
  p_address TEXT,
  p_lat DOUBLE PRECISION,
  p_lng DOUBLE PRECISION
)
RETURNS public.warehouses
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_profile public.profiles%ROWTYPE;
  v_warehouse public.warehouses%ROWTYPE;
BEGIN
  SELECT *
  INTO v_profile
  FROM public.profiles
  WHERE id = auth.uid()
    AND account_type = 'WAREHOUSE'
    AND is_active = true;

  IF v_profile IS NULL OR v_profile.warehouse_id IS NULL THEN
    RAISE EXCEPTION 'Unauthorized: only active WAREHOUSE users can update their own warehouse location';
  END IF;

  IF p_address IS NULL OR btrim(p_address) = '' THEN
    RAISE EXCEPTION 'Invalid warehouse address';
  END IF;

  IF p_lat IS NULL
     OR p_lng IS NULL
     OR p_lat = 'NaN'::DOUBLE PRECISION
     OR p_lng = 'NaN'::DOUBLE PRECISION
     OR p_lat < -90
     OR p_lat > 90
     OR p_lng < -180
     OR p_lng > 180 THEN
    RAISE EXCEPTION 'Invalid warehouse coordinates';
  END IF;

  UPDATE public.warehouses
  SET
    formatted_address = btrim(p_address),
    latitude = p_lat,
    longitude = p_lng
  WHERE id = v_profile.warehouse_id
  RETURNING * INTO v_warehouse;

  IF v_warehouse IS NULL THEN
    RAISE EXCEPTION 'Warehouse not found';
  END IF;

  RETURN v_warehouse;
END;
$$;

GRANT EXECUTE ON FUNCTION public.update_my_warehouse_location(
  TEXT,
  DOUBLE PRECISION,
  DOUBLE PRECISION
) TO authenticated;
