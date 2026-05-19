-- ====================================================================
-- Admin Dashboard Completion: Real Data Integration
-- Purpose: Add RPCs for pending requests, recent activities, system health
-- Date: 2026-05-07
-- ====================================================================

-- ============================================
-- 1. Admin RPC: Get Pending Requests
-- ============================================

CREATE OR REPLACE FUNCTION admin_get_pending_requests(
  p_limit INT DEFAULT 5
)
RETURNS TABLE (
  id TEXT,
  title TEXT,
  subtitle TEXT,
  timestamp TEXT,
  request_type TEXT
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
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view pending requests';
  END IF;

  -- Return pending orders as requests
  RETURN QUERY
  SELECT
    o.id::TEXT as id,
    CASE 
      WHEN o.order_type = 'CUSTOMER_PHARMACY' THEN 'طلب عميل جديد'
      WHEN o.order_type = 'PHARMACY_WAREHOUSE' THEN 'طلب صيدلية جديد'
      ELSE 'طلب جديد'
    END as title,
    CONCAT(o.medicine_name, ' - ', o.quantity, ' ', o.unit) as subtitle,
    to_char(o.created_at, 'YYYY-MM-DD HH24:MI') as timestamp,
    'ORDER' as request_type
  FROM public.orders o
  WHERE o.status = 'PENDING'
  ORDER BY o.created_at DESC
  LIMIT p_limit;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_get_pending_requests TO authenticated;

-- ============================================
-- 2. Admin RPC: Get Recent Activities
-- ============================================

CREATE OR REPLACE FUNCTION admin_get_recent_activities(
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
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view recent activities';
  END IF;

  -- Return recent audit logs
  RETURN QUERY
  SELECT
    al.id::TEXT as id,
    al.action as action,
    COALESCE(p.full_name, 'نظام') as user_name,
    to_char(al.created_at, 'YYYY-MM-DD HH24:MI') as timestamp,
    CASE 
      WHEN al.action LIKE '%نجح%' OR al.action LIKE '%تم%' THEN 'SUCCESS'
      WHEN al.action LIKE '%فشل%' OR al.action LIKE '%خطأ%' THEN 'FAILED'
      ELSE 'PENDING'
    END as status
  FROM public.audit_logs al
  LEFT JOIN public.profiles p ON p.id = al.admin_id
  ORDER BY al.created_at DESC
  LIMIT p_limit;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_get_recent_activities TO authenticated;

-- ============================================
-- 3. Admin RPC: Get System Health
-- ============================================

CREATE OR REPLACE FUNCTION admin_get_system_health()
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
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view system health';
  END IF;

  -- Calculate facility health
  SELECT 
    COUNT(*),
    COUNT(*) FILTER (WHERE is_active = true)
  INTO v_total_facilities, v_active_facilities
  FROM (
    SELECT is_active FROM public.pharmacies
    UNION ALL
    SELECT is_active FROM public.warehouses
  ) facilities;

  -- Calculate order success rate (last 100 orders)
  SELECT 
    COUNT(*),
    COUNT(*) FILTER (WHERE status IN ('DELIVERED', 'CONFIRMED'))
  INTO v_total_orders, v_successful_orders
  FROM (
    SELECT status FROM public.orders
    ORDER BY created_at DESC
    LIMIT 100
  ) recent_orders;

  -- Calculate overall health percentage
  v_health_percent := CASE 
    WHEN v_total_facilities > 0 AND v_total_orders > 0 THEN
      ((v_active_facilities::FLOAT / v_total_facilities * 50) + 
       (v_successful_orders::FLOAT / v_total_orders * 50))::INT
    WHEN v_total_facilities > 0 THEN
      (v_active_facilities::FLOAT / v_total_facilities * 100)::INT
    ELSE 95
  END;

  -- Determine health status
  v_health_status := CASE 
    WHEN v_health_percent >= 90 THEN 'ممتاز'
    WHEN v_health_percent >= 75 THEN 'جيد'
    WHEN v_health_percent >= 50 THEN 'متوسط'
    ELSE 'يحتاج تحسين'
  END;

  -- Build health object (activeConnections removed - not available in current schema)
  SELECT jsonb_build_object(
    'healthPercent', v_health_percent,
    'healthStatus', v_health_status
  ) INTO v_health;

  RETURN v_health;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_get_system_health TO authenticated;

-- ============================================
-- Migration Complete
-- ============================================
