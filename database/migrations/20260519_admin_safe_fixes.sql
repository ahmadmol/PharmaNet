-- Batch D safe admin fixes:
-- - repair admin dashboard completion RPCs
-- - persist full_name through admin_update_user_profile

CREATE OR REPLACE FUNCTION public.admin_get_recent_activities(
  p_limit INT DEFAULT 5
)
RETURNS TABLE (
  id TEXT,
  action TEXT,
  user_name TEXT,
  "timestamp" TEXT,
  status TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view recent activities';
  END IF;

  RETURN QUERY
  SELECT
    al.id::TEXT AS id,
    al.action AS action,
    COALESCE(NULLIF(p.full_name, ''), al.admin_email, 'System') AS user_name,
    to_char(al.created_at, 'YYYY-MM-DD HH24:MI') AS "timestamp",
    CASE
      WHEN al.action LIKE '%SUCCESS%' OR al.action LIKE '%CREATE%' OR al.action LIKE '%UPDATE%' THEN 'SUCCESS'
      WHEN al.action LIKE '%FAIL%' OR al.action LIKE '%ERROR%' THEN 'FAILED'
      ELSE 'PENDING'
    END AS status
  FROM public.audit_logs al
  LEFT JOIN public.profiles p ON p.id = al.admin_id
  ORDER BY al.created_at DESC
  LIMIT p_limit;
END;
$$;

GRANT EXECUTE ON FUNCTION public.admin_get_recent_activities(INT) TO authenticated;

CREATE OR REPLACE FUNCTION public.admin_get_system_health()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_health JSONB;
  v_total_facilities INT;
  v_active_facilities INT;
  v_total_orders INT;
  v_successful_orders INT;
  v_health_percent INT;
  v_health_status TEXT;
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view system health';
  END IF;

  SELECT
    COUNT(*),
    COUNT(*) FILTER (WHERE is_active = true)
  INTO v_total_facilities, v_active_facilities
  FROM (
    SELECT is_active FROM public.pharmacies
    UNION ALL
    SELECT is_active FROM public.warehouses
  ) facilities;

  SELECT
    COUNT(*),
    COUNT(*) FILTER (WHERE status IN ('DELIVERED', 'CONFIRMED'))
  INTO v_total_orders, v_successful_orders
  FROM (
    SELECT status
    FROM public.orders
    ORDER BY created_at DESC
    LIMIT 100
  ) recent_orders;

  v_health_percent := CASE
    WHEN v_total_facilities > 0 AND v_total_orders > 0 THEN
      ((v_active_facilities::FLOAT / v_total_facilities * 50) +
       (v_successful_orders::FLOAT / v_total_orders * 50))::INT
    WHEN v_total_facilities > 0 THEN
      (v_active_facilities::FLOAT / v_total_facilities * 100)::INT
    ELSE 95
  END;

  v_health_status := CASE
    WHEN v_health_percent >= 90 THEN 'Excellent'
    WHEN v_health_percent >= 75 THEN 'Good'
    WHEN v_health_percent >= 50 THEN 'Fair'
    ELSE 'Needs attention'
  END;

  SELECT jsonb_build_object(
    'healthPercent', v_health_percent,
    'healthStatus', v_health_status
  ) INTO v_health;

  RETURN v_health;
END;
$$;

GRANT EXECUTE ON FUNCTION public.admin_get_system_health() TO authenticated;

DROP FUNCTION IF EXISTS public.admin_update_user_profile(UUID, TEXT, UUID, UUID, BOOLEAN);

CREATE OR REPLACE FUNCTION public.admin_update_user_profile(
  p_target_user_id UUID,
  p_account_type TEXT,
  p_pharmacy_id UUID DEFAULT NULL,
  p_warehouse_id UUID DEFAULT NULL,
  p_is_active BOOLEAN DEFAULT true,
  p_full_name TEXT DEFAULT NULL
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
  SELECT * INTO v_admin_profile
  FROM public.profiles
  WHERE id = auth.uid();

  IF v_admin_profile IS NULL
      OR v_admin_profile.account_type != 'ADMIN'
      OR v_admin_profile.is_active != true THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can update user profiles';
  END IF;

  SELECT * INTO v_old_profile
  FROM public.profiles
  WHERE id = p_target_user_id;

  IF v_old_profile IS NULL THEN
    RAISE EXCEPTION 'User profile not found';
  END IF;

  IF p_account_type NOT IN ('PUBLIC_USER', 'PHARMACY', 'WAREHOUSE', 'ADMIN') THEN
    RAISE EXCEPTION 'Invalid account_type: %', p_account_type;
  END IF;

  IF p_account_type = 'PHARMACY' AND p_pharmacy_id IS NULL THEN
    RAISE EXCEPTION 'PHARMACY account requires pharmacy_id';
  END IF;

  IF p_account_type = 'WAREHOUSE' AND p_warehouse_id IS NULL THEN
    RAISE EXCEPTION 'WAREHOUSE account requires warehouse_id';
  END IF;

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

  SELECT email INTO v_target_email FROM auth.users WHERE id = p_target_user_id;
  SELECT email INTO v_admin_email FROM auth.users WHERE id = auth.uid();

  UPDATE public.profiles
  SET
    full_name = COALESCE(NULLIF(trim(p_full_name), ''), full_name),
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
      'full_name', v_old_profile.full_name,
      'account_type', v_old_profile.account_type,
      'pharmacy_id', v_old_profile.pharmacy_id,
      'warehouse_id', v_old_profile.warehouse_id,
      'is_active', v_old_profile.is_active
    ),
    jsonb_build_object(
      'full_name', COALESCE(NULLIF(trim(p_full_name), ''), v_old_profile.full_name),
      'account_type', p_account_type,
      'pharmacy_id', p_pharmacy_id,
      'warehouse_id', p_warehouse_id,
      'is_active', p_is_active
    )
  );

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

GRANT EXECUTE ON FUNCTION public.admin_update_user_profile(UUID, TEXT, UUID, UUID, BOOLEAN, TEXT) TO authenticated;
