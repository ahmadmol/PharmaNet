-- ====================================================================
-- Admin Phase 1: Dashboard Stats & Orders Management
-- Purpose: Enable admin to view comprehensive stats and all orders
-- Date: 2026-05-06
-- ====================================================================

-- ============================================
-- 1. Admin RPC: Get Dashboard Statistics
-- ============================================

CREATE OR REPLACE FUNCTION admin_get_dashboard_stats()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_stats JSONB;
BEGIN
  -- Verify admin
  IF NOT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid()
      AND account_type = 'ADMIN'
      AND is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view dashboard stats';
  END IF;

  -- Build stats object
  SELECT jsonb_build_object(
    'totalUsers', (SELECT COUNT(*) FROM public.profiles),
    'totalPharmacies', (SELECT COUNT(*) FROM public.pharmacies),
    'totalWarehouses', (SELECT COUNT(*) FROM public.warehouses),
    'totalOrders', (SELECT COUNT(*) FROM public.orders),
    'b2cOrdersCount', (SELECT COUNT(*) FROM public.orders WHERE order_type = 'CUSTOMER_PHARMACY'),
    'b2bOrdersCount', (SELECT COUNT(*) FROM public.orders WHERE order_type = 'PHARMACY_WAREHOUSE'),
    'urgentOrdersCount', (SELECT COUNT(*) FROM public.orders WHERE is_urgent = true),
    'pendingOrdersCount', (SELECT COUNT(*) FROM public.orders WHERE status = 'PENDING'),
    'confirmedOrdersCount', (SELECT COUNT(*) FROM public.orders WHERE status = 'CONFIRMED'),
    'deliveredOrdersCount', (SELECT COUNT(*) FROM public.orders WHERE status = 'DELIVERED'),
    'activePharmacies', (SELECT COUNT(*) FROM public.pharmacies WHERE is_active = true),
    'activeWarehouses', (SELECT COUNT(*) FROM public.warehouses WHERE is_active = true)
  ) INTO v_stats;

  RETURN v_stats;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_get_dashboard_stats TO authenticated;

-- ============================================
-- 2. Admin RPC: Get All Orders with Filters
-- ============================================

CREATE OR REPLACE FUNCTION admin_get_all_orders(
  p_order_type TEXT DEFAULT NULL,
  p_status TEXT DEFAULT NULL,
  p_is_urgent BOOLEAN DEFAULT NULL,
  p_search TEXT DEFAULT NULL,
  p_limit INT DEFAULT 100,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  id UUID,
  order_type TEXT,
  status TEXT,
  medicine_name TEXT,
  quantity INT,
  unit TEXT,
  pharmacy_id UUID,
  pharmacy_name TEXT,
  warehouse_id UUID,
  warehouse_name TEXT,
  customer_id UUID,
  customer_name TEXT,
  is_urgent BOOLEAN,
  total_price_cents BIGINT,
  currency TEXT,
  fulfillment_type TEXT,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ,
  confirmed_at TIMESTAMPTZ,
  fulfilled_at TIMESTAMPTZ
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
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view all orders';
  END IF;

  -- Return filtered orders
  RETURN QUERY
  SELECT
    o.id,
    o.order_type,
    o.status,
    o.medicine_name,
    o.quantity,
    o.unit,
    o.pharmacy_id,
    ph.name as pharmacy_name,
    o.warehouse_id,
    w.name as warehouse_name,
    o.customer_id,
    p.full_name as customer_name,
    o.is_urgent,
    o.total_price_cents,
    o.currency,
    o.fulfillment_type,
    o.created_at,
    o.updated_at,
    o.confirmed_at,
    o.fulfilled_at
  FROM public.orders o
  LEFT JOIN public.pharmacies ph ON ph.id = o.pharmacy_id
  LEFT JOIN public.warehouses w ON w.id = o.warehouse_id
  LEFT JOIN public.profiles p ON p.id = o.customer_id
  WHERE
    (p_order_type IS NULL OR o.order_type = p_order_type)
    AND (p_status IS NULL OR o.status = p_status)
    AND (p_is_urgent IS NULL OR o.is_urgent = p_is_urgent)
    AND (p_search IS NULL OR p_search = '' OR LOWER(o.medicine_name) LIKE LOWER('%' || p_search || '%'))
  ORDER BY o.created_at DESC
  LIMIT p_limit
  OFFSET p_offset;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_get_all_orders TO authenticated;

-- ============================================
-- 3. Performance Indexes
-- ============================================

-- Index for order_type filtering
CREATE INDEX IF NOT EXISTS idx_orders_order_type 
  ON public.orders(order_type);

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_orders_status 
  ON public.orders(status);

-- Index for urgent filtering
CREATE INDEX IF NOT EXISTS idx_orders_is_urgent 
  ON public.orders(is_urgent) 
  WHERE is_urgent = true;

-- Index for date sorting
CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc 
  ON public.orders(created_at DESC);

-- Composite index for common admin queries
CREATE INDEX IF NOT EXISTS idx_orders_admin_filters 
  ON public.orders(order_type, status, is_urgent, created_at DESC);

-- Index for medicine name search (using trigram for better performance)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_orders_medicine_name_trgm 
  ON public.orders USING gin(medicine_name gin_trgm_ops);

-- ============================================
-- Migration Complete
-- ============================================
