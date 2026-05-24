-- ====================================================================
-- Warehouse Product Catalog Phase 1
-- Date: 2026-06-02
-- Scope:
--   - DB/RPC/RLS only.
--   - Reuse public.medicines as warehouse products.
--   - Add product catalog fields and PHARMACY-safe read RPC.
--   - Preserve existing medicines table read/write policies.
-- ====================================================================

-- --------------------------------------------------------------------
-- Product catalog fields
-- --------------------------------------------------------------------
ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS description text;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS specs jsonb;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS is_visible boolean NOT NULL DEFAULT true;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS currency text NOT NULL DEFAULT 'SYP';

-- Keep price nullable/optional. Do not add or alter price here.

-- --------------------------------------------------------------------
-- Product catalog indexes
-- --------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_medicines_warehouse_active_visible
ON public.medicines (warehouse_id, is_active, is_visible)
WHERE warehouse_id IS NOT NULL;

-- --------------------------------------------------------------------
-- PHARMACY-safe warehouse product gallery RPC
-- --------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_warehouse_visible_products(
  p_warehouse_id uuid
)
RETURNS TABLE (
  id uuid,
  warehouse_id uuid,
  name text,
  brand text,
  strength text,
  description text,
  specs jsonb,
  price numeric,
  currency text,
  image_url text,
  stock_quantity integer,
  created_at timestamptz,
  updated_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF p_warehouse_id IS NULL THEN
    RAISE EXCEPTION 'warehouse_id is required' USING ERRCODE = '22023';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'PHARMACY'
      AND p.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: only active PHARMACY users can read warehouse products'
      USING ERRCODE = '42501';
  END IF;

  RETURN QUERY
  SELECT
    m.id,
    m.warehouse_id,
    m.name::text,
    m.brand::text,
    m.strength::text,
    m.description::text,
    m.specs,
    m.price::numeric,
    m.currency::text,
    m.image_url::text,
    m.stock_quantity::integer,
    m.created_at,
    m.updated_at
  FROM public.medicines m
  JOIN public.warehouses w ON w.id = m.warehouse_id
  WHERE m.warehouse_id = p_warehouse_id
    AND w.is_active = true
    AND m.is_active = true
    AND m.is_visible = true
  ORDER BY
    COALESCE(m.updated_at, m.created_at) DESC,
    m.name ASC;
END;
$$;

REVOKE ALL ON FUNCTION public.get_warehouse_visible_products(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_warehouse_visible_products(uuid) TO authenticated;

COMMENT ON FUNCTION public.get_warehouse_visible_products(uuid)
IS 'Returns visible active products for an active warehouse. Intended PHARMACY gallery read path.';
