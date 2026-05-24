-- ====================================================================
-- Warehouse inventory product management fields
-- Date: 2026-06-03
-- Scope:
--   - Extend warehouse_inventory view with product catalog fields.
--   - Preserve existing ownership/read behavior.
-- ====================================================================

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS description text;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS is_visible boolean NOT NULL DEFAULT true;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;

ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS currency text NOT NULL DEFAULT 'SYP';

CREATE OR REPLACE VIEW public.warehouse_inventory AS
SELECT
  concat(m.warehouse_id::TEXT, '_', m.id::TEXT) AS id,
  m.id AS medicine_id,
  m.name AS medicine_name,
  m.image_url,
  m.warehouse_id,
  COALESCE(m.stock_quantity, 0) AS quantity,
  'box'::TEXT AS unit,
  CASE
    WHEN COALESCE(m.stock_quantity, 0) <= 0 THEN 'OUT_OF_STOCK'
    WHEN COALESCE(m.stock_quantity, 0) < 10 THEN 'LOW_STOCK'
    ELSE 'IN_STOCK'
  END AS stock_status,
  COALESCE(m.updated_at, m.created_at, now()) AS last_updated,
  m.description,
  m.price,
  m.currency,
  m.is_visible,
  m.is_active
FROM public.medicines m
WHERE m.warehouse_id IS NOT NULL
  AND (
    EXISTS (
      SELECT 1
      FROM public.profiles p
      WHERE p.id = auth.uid()
        AND p.account_type = 'ADMIN'
        AND p.is_active = true
    )
    OR EXISTS (
      SELECT 1
      FROM public.profiles p
      WHERE p.id = auth.uid()
        AND p.account_type = 'WAREHOUSE'
        AND p.is_active = true
        AND p.warehouse_id = m.warehouse_id
    )
  );

GRANT SELECT ON public.warehouse_inventory TO authenticated;
