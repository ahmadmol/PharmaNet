ALTER TABLE public.pharmacies
    RENAME COLUMN location TO formatted_address;

ALTER TABLE public.pharmacies
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE public.warehouses
    RENAME COLUMN location TO formatted_address;

ALTER TABLE public.warehouses
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

CREATE OR REPLACE FUNCTION public.admin_create_pharmacy(
  p_name TEXT,
  p_location TEXT,
  p_contact_number TEXT,
  p_license_number TEXT,
  p_latitude DOUBLE PRECISION,
  p_longitude DOUBLE PRECISION
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_new_pharmacy RECORD;
  v_admin_email TEXT;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'ADMIN'
      AND is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can create pharmacies';
  END IF;

  SELECT email INTO v_admin_email FROM auth.users WHERE id = auth.uid();

  INSERT INTO public.pharmacies (
    name,
    formatted_address,
    contact_number,
    license_number,
    latitude,
    longitude
  )
  VALUES (
    p_name,
    p_location,
    p_contact_number,
    p_license_number,
    p_latitude,
    p_longitude
  )
  RETURNING * INTO v_new_pharmacy;

  INSERT INTO public.audit_logs (
    admin_id,
    admin_email,
    action,
    target_pharmacy_id,
    new_value
  ) VALUES (
    auth.uid(),
    v_admin_email,
    'PHARMACY_CREATE',
    v_new_pharmacy.id,
    to_jsonb(v_new_pharmacy)
  );

  RETURN to_jsonb(v_new_pharmacy);
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_create_warehouse(
  p_name TEXT,
  p_location TEXT,
  p_contact_number TEXT,
  p_latitude DOUBLE PRECISION,
  p_longitude DOUBLE PRECISION
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_new_warehouse RECORD;
  v_admin_email TEXT;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'ADMIN'
      AND is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can create warehouses';
  END IF;

  SELECT email INTO v_admin_email FROM auth.users WHERE id = auth.uid();

  INSERT INTO public.warehouses (
    name,
    formatted_address,
    contact_number,
    latitude,
    longitude
  )
  VALUES (
    p_name,
    p_location,
    p_contact_number,
    p_latitude,
    p_longitude
  )
  RETURNING * INTO v_new_warehouse;

  INSERT INTO public.audit_logs (
    admin_id,
    admin_email,
    action,
    target_warehouse_id,
    new_value
  ) VALUES (
    auth.uid(),
    v_admin_email,
    'WAREHOUSE_CREATE',
    v_new_warehouse.id,
    to_jsonb(v_new_warehouse)
  );

  RETURN to_jsonb(v_new_warehouse);
END;
$$;

GRANT EXECUTE ON FUNCTION public.admin_create_pharmacy(
  TEXT,
  TEXT,
  TEXT,
  TEXT,
  DOUBLE PRECISION,
  DOUBLE PRECISION
) TO authenticated;

GRANT EXECUTE ON FUNCTION public.admin_create_warehouse(
  TEXT,
  TEXT,
  TEXT,
  DOUBLE PRECISION,
  DOUBLE PRECISION
) TO authenticated;
