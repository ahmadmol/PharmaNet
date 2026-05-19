-- ============================================
-- Migration: Warehouse medicines ownership write access
-- Date: 2026-05-12
-- Purpose:
--   - Keep ADMIN full medicines access unchanged
--   - Allow WAREHOUSE to INSERT/UPDATE/DELETE only own warehouse medicines
--   - Validate ownership server-side via profiles.warehouse_id
-- ============================================

ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;

-- Keep read policy behavior unchanged (all authenticated users can read medicines)
-- Existing policy expected from 20260504_add_medicines_rls.sql:
--   medicines_read_authenticated

-- Existing admin policy remains valid; recreate defensively for idempotence.
DROP POLICY IF EXISTS medicines_write_admin_only ON public.medicines;
DROP POLICY IF EXISTS "medicines_write_admin_only" ON public.medicines;
CREATE POLICY "medicines_write_admin_only"
ON public.medicines
FOR ALL
TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND account_type = 'ADMIN'
          AND is_active = true
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND account_type = 'ADMIN'
          AND is_active = true
    )
);

-- Warehouse can insert medicines only for own warehouse_id.
DROP POLICY IF EXISTS medicines_insert_own_warehouse ON public.medicines;
CREATE POLICY medicines_insert_own_warehouse
ON public.medicines
FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND account_type = 'WAREHOUSE'
          AND is_active = true
          AND warehouse_id IS NOT NULL
          AND medicines.warehouse_id = profiles.warehouse_id
    )
);

-- Warehouse can update only medicines that belong to own warehouse.
DROP POLICY IF EXISTS medicines_update_own_warehouse ON public.medicines;
CREATE POLICY medicines_update_own_warehouse
ON public.medicines
FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND account_type = 'WAREHOUSE'
          AND is_active = true
          AND warehouse_id IS NOT NULL
          AND medicines.warehouse_id = profiles.warehouse_id
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND account_type = 'WAREHOUSE'
          AND is_active = true
          AND warehouse_id IS NOT NULL
          AND medicines.warehouse_id = profiles.warehouse_id
    )
);

-- Warehouse can delete only medicines that belong to own warehouse.
DROP POLICY IF EXISTS medicines_delete_own_warehouse ON public.medicines;
CREATE POLICY medicines_delete_own_warehouse
ON public.medicines
FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND account_type = 'WAREHOUSE'
          AND is_active = true
          AND warehouse_id IS NOT NULL
          AND medicines.warehouse_id = profiles.warehouse_id
    )
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.medicines TO authenticated;
