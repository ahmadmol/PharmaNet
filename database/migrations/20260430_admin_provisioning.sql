-- ====================================================================
-- Phase 4.5.6: Admin Provisioning System
-- Purpose: Secure admin-controlled user/pharmacy/warehouse management
-- Author: System Security Team
-- Date: 2026-04-30
-- ====================================================================

-- 1. Create pharmacies table (if not exists)
CREATE TABLE IF NOT EXISTS public.pharmacies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  location TEXT,
  contact_number TEXT,
  license_number TEXT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Create warehouses table (if not exists)
CREATE TABLE IF NOT EXISTS public.warehouses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  location TEXT,
  contact_number TEXT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Create audit_logs table
CREATE TABLE IF NOT EXISTS public.audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id UUID NOT NULL REFERENCES auth.users(id),
  admin_email TEXT,
  action TEXT NOT NULL,
  target_user_id UUID REFERENCES auth.users(id),
  target_user_email TEXT,
  target_pharmacy_id UUID REFERENCES public.pharmacies(id),
  target_warehouse_id UUID REFERENCES public.warehouses(id),
  old_value JSONB,
  new_value JSONB,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- 4. Enable RLS on all tables
ALTER TABLE public.pharmacies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.warehouses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies for pharmacies

-- Admin: full access
CREATE POLICY admin_manage_pharmacies ON public.pharmacies
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
    )
  );

-- Pharmacy users: read-only their own
CREATE POLICY pharmacy_read_own ON public.pharmacies
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.pharmacy_id = pharmacies.id
        AND profiles.is_active = true
    )
  );

-- 6. RLS Policies for warehouses

-- Admin: full access
CREATE POLICY admin_manage_warehouses ON public.warehouses
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
    )
  );

-- Warehouse users: read-only their own
CREATE POLICY warehouse_read_own ON public.warehouses
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.warehouse_id = warehouses.id
        AND profiles.is_active = true
    )
  );

-- 7. RLS Policy for audit_logs (admin read-only)
CREATE POLICY admin_read_audit_logs ON public.audit_logs
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
    )
  );

-- 8. Admin RPC: Update user profile with role and tenant
CREATE OR REPLACE FUNCTION admin_update_user_profile(
  p_target_user_id UUID,
  p_account_type TEXT,
  p_pharmacy_id UUID DEFAULT NULL,
  p_warehouse_id UUID DEFAULT NULL,
  p_is_active BOOLEAN DEFAULT true
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_admin_profile RECORD;
  v_old_profile RECORD;
  v_target_email TEXT;
  v_admin_email TEXT;
  v_result JSONB;
BEGIN
  -- 1. Verify caller is active ADMIN
  SELECT * INTO v_admin_profile
  FROM public.profiles
  WHERE id = auth.uid();
  
  IF v_admin_profile IS NULL
      OR v_admin_profile.account_type != 'ADMIN'
      OR v_admin_profile.is_active != true THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can update user profiles';
  END IF;
  
  -- 2. Get old profile for audit
  SELECT * INTO v_old_profile
  FROM public.profiles
  WHERE id = p_target_user_id;
  
  IF v_old_profile IS NULL THEN
    RAISE EXCEPTION 'User profile not found';
  END IF;
  
  -- 3. Validate account_type
  IF p_account_type NOT IN ('PUBLIC_USER', 'PHARMACY', 'WAREHOUSE', 'ADMIN') THEN
    RAISE EXCEPTION 'Invalid account_type: %', p_account_type;
  END IF;
  
  -- 4. Validate tenant linkage
  IF p_account_type = 'PHARMACY' AND p_pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'PHARMACY account requires pharmacy_id';
  END IF;
  
  IF p_account_type = 'WAREHOUSE' AND p_warehouse_id IS NULL THEN
    RAISE EXCEPTION 'WAREHOUSE account requires warehouse_id';
  END IF;
  
  -- 5. Verify pharmacy/warehouse exists if provided
  IF p_pharmacy_id IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM public.pharmacies WHERE id = p_pharmacy_id AND is_active = true) THEN
      RAISE EXCEPTION 'Pharmacy not found or inactive: %', p_pharmacy_id;
    END IF;
  END IF;
  
  IF p_warehouse_id IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM public.warehouses WHERE id = p_warehouse_id AND is_active = true) THEN
      RAISE EXCEPTION 'Warehouse not found or inactive: %', p_warehouse_id;
    END IF;
  END IF;
  
  -- 6. Get emails for audit
  SELECT email INTO v_target_email FROM auth.users WHERE id = p_target_user_id;
  SELECT email INTO v_admin_email FROM auth.users WHERE id = auth.uid();
  
  -- 7. Update profile
  UPDATE public.profiles
  SET
    account_type = p_account_type,
    pharmacy_id = CASE
      WHEN p_account_type = 'PHARMACY' THEN p_pharmacy_id
      ELSE NULL
    END,
    warehouse_id = CASE
      WHEN p_account_type = 'WAREHOUSE' THEN p_warehouse_id
      ELSE NULL
    END,
    is_active = p_is_active,
    updated_at = now()
  WHERE id = p_target_user_id;
  
  -- 8. Log to audit
  INSERT INTO public.audit_logs (
    admin_id,
    admin_email,
    action,
    target_user_id,
    target_user_email,
    target_pharmacy_id,
    target_warehouse_id,
    old_value,
    new_value
  ) VALUES (
    auth.uid(),
    v_admin_email,
    'PROFILE_UPDATE',
    p_target_user_id,
    v_target_email,
    p_pharmacy_id,
    p_warehouse_id,
    jsonb_build_object(
      'account_type', v_old_profile.account_type,
      'pharmacy_id', v_old_profile.pharmacy_id,
      'warehouse_id', v_old_profile.warehouse_id,
      'is_active', v_old_profile.is_active
    ),
    jsonb_build_object(
      'account_type', p_account_type,
      'pharmacy_id', p_pharmacy_id,
      'warehouse_id', p_warehouse_id,
      'is_active', p_is_active
    )
  );
  
  -- 9. Return updated profile
  SELECT jsonb_build_object(
    'id', id,
    'account_type', account_type,
    'pharmacy_id', pharmacy_id,
    'warehouse_id', warehouse_id,
    'is_active', is_active,
    'full_name', full_name,
    'phone_number', phone_number
  ) INTO v_result
  FROM public.profiles
  WHERE id = p_target_user_id;
  
  RETURN v_result;
END;
$$;

-- 9. Admin RPC: Create pharmacy
CREATE OR REPLACE FUNCTION admin_create_pharmacy(
  p_name TEXT,
  p_location TEXT,
  p_contact_number TEXT,
  p_license_number TEXT
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
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'ADMIN'
      AND is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can create pharmacies';
  END IF;
  
  -- Get admin email
  SELECT email INTO v_admin_email FROM auth.users WHERE id = auth.uid();
  
  -- Insert pharmacy
  INSERT INTO public.pharmacies (name, location, contact_number, license_number)
  VALUES (p_name, p_location, p_contact_number, p_license_number)
  RETURNING * INTO v_new_pharmacy;
  
  -- Log
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

-- 10. Admin RPC: Create warehouse
CREATE OR REPLACE FUNCTION admin_create_warehouse(
  p_name TEXT,
  p_location TEXT,
  p_contact_number TEXT
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
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'ADMIN'
      AND is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can create warehouses';
  END IF;
  
  -- Get admin email
  SELECT email INTO v_admin_email FROM auth.users WHERE id = auth.uid();
  
  -- Insert warehouse
  INSERT INTO public.warehouses (name, location, contact_number)
  VALUES (p_name, p_location, p_contact_number)
  RETURNING * INTO v_new_warehouse;
  
  -- Log
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

-- 11. Admin RPC: Get all users with profiles (joined with auth.users)
CREATE OR REPLACE FUNCTION admin_get_all_users()
RETURNS TABLE (
  id UUID,
  email TEXT,
  account_type TEXT,
  pharmacy_id UUID,
  warehouse_id UUID,
  is_active BOOLEAN,
  full_name TEXT,
  phone_number TEXT,
  pharmacy_name TEXT,
  warehouse_name TEXT,
  created_at TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view all users';
  END IF;
  
  RETURN QUERY
  SELECT
    p.id,
    u.email,
    p.account_type,
    p.pharmacy_id,
    p.warehouse_id,
    p.is_active,
    p.full_name,
    p.phone_number,
    ph.name as pharmacy_name,
    w.name as warehouse_name,
    p.created_at
  FROM public.profiles p
  JOIN auth.users u ON u.id = p.id
  LEFT JOIN public.pharmacies ph ON ph.id = p.pharmacy_id
  LEFT JOIN public.warehouses w ON w.id = p.warehouse_id
  ORDER BY p.created_at DESC;
END;
$$;

-- 12. Admin RPC: Get audit logs
CREATE OR REPLACE FUNCTION admin_get_audit_logs(
  p_limit INT DEFAULT 100
)
RETURNS TABLE (
  id UUID,
  admin_id UUID,
  admin_email TEXT,
  action TEXT,
  target_user_id UUID,
  target_user_email TEXT,
  old_value JSONB,
  new_value JSONB,
  created_at TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view audit logs';
  END IF;
  
  RETURN QUERY
  SELECT
    al.id,
    al.admin_id,
    al.admin_email,
    al.action,
    al.target_user_id,
    al.target_user_email,
    al.old_value,
    al.new_value,
    al.created_at
  FROM public.audit_logs al
  ORDER BY al.created_at DESC
  LIMIT p_limit;
END;
$$;

-- 13. Grant execute permissions
GRANT EXECUTE ON FUNCTION admin_update_user_profile TO authenticated;
GRANT EXECUTE ON FUNCTION admin_create_pharmacy TO authenticated;
GRANT EXECUTE ON FUNCTION admin_create_warehouse TO authenticated;
GRANT EXECUTE ON FUNCTION admin_get_all_users TO authenticated;
GRANT EXECUTE ON FUNCTION admin_get_audit_logs TO authenticated;

-- 14. Indexes for performance
CREATE INDEX IF NOT EXISTS idx_pharmacies_is_active ON public.pharmacies(is_active);
CREATE INDEX IF NOT EXISTS idx_warehouses_is_active ON public.warehouses(is_active);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON public.audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_admin_id ON public.audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_profiles_account_type ON public.profiles(account_type);

-- Migration complete
