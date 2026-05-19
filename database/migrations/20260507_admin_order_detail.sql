-- ====================================================================
-- Admin Phase 1.1: Order Detail RPC
-- Purpose: Enable admin to view single order details
-- Date: 2026-05-07
-- ====================================================================

-- ============================================
-- Admin RPC: Get Order Detail by ID
-- ============================================

CREATE OR REPLACE FUNCTION admin_get_order_detail(
  p_order_id UUID
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
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view order details';
  END IF;

  -- Return single order with all details
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
  WHERE o.id = p_order_id;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_get_order_detail TO authenticated;

-- ============================================
-- Migration Complete
-- ============================================
