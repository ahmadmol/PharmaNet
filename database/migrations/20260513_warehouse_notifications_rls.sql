-- ============================================
-- Migration: Warehouse notifications RLS policies
-- Date: 2026-05-13
-- Purpose:
--   - Allow WAREHOUSE users to read/update/delete their own notifications
--   - Scope by app_notifications.user_id = auth.uid()
--   - Keep existing PHARMACY/ADMIN behavior unchanged
-- ============================================

ALTER TABLE public.app_notifications ENABLE ROW LEVEL SECURITY;

-- Drop legacy/alternate warehouse policy names defensively.
DROP POLICY IF EXISTS notifications_read_own_warehouse ON public.app_notifications;
DROP POLICY IF EXISTS notifications_update_own_warehouse ON public.app_notifications;
DROP POLICY IF EXISTS notifications_delete_own_warehouse ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_read_own_warehouse" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_update_own_warehouse" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_delete_own_warehouse" ON public.app_notifications;

CREATE POLICY "notifications_read_own_warehouse"
ON public.app_notifications
FOR SELECT
TO authenticated
USING (
    app_notifications.user_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE profiles.id = auth.uid()
          AND profiles.account_type = 'WAREHOUSE'
          AND profiles.is_active = true
    )
);

CREATE POLICY "notifications_update_own_warehouse"
ON public.app_notifications
FOR UPDATE
TO authenticated
USING (
    app_notifications.user_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE profiles.id = auth.uid()
          AND profiles.account_type = 'WAREHOUSE'
          AND profiles.is_active = true
    )
)
WITH CHECK (
    app_notifications.user_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE profiles.id = auth.uid()
          AND profiles.account_type = 'WAREHOUSE'
          AND profiles.is_active = true
    )
);

CREATE POLICY "notifications_delete_own_warehouse"
ON public.app_notifications
FOR DELETE
TO authenticated
USING (
    app_notifications.user_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.profiles
        WHERE profiles.id = auth.uid()
          AND profiles.account_type = 'WAREHOUSE'
          AND profiles.is_active = true
    )
);
