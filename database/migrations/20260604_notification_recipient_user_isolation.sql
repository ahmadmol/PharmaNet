-- Enforce notification recipient isolation by user_id.
-- pharmacy_id remains contextual metadata and must not grant notification access.

ALTER TABLE public.app_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS notifications_read_own_pharmacy ON public.app_notifications;
DROP POLICY IF EXISTS notifications_update_own_pharmacy ON public.app_notifications;
DROP POLICY IF EXISTS notifications_delete_own_pharmacy ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_read_own_pharmacy" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_update_own_pharmacy" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_delete_own_pharmacy" ON public.app_notifications;

DROP POLICY IF EXISTS notifications_read_own_warehouse ON public.app_notifications;
DROP POLICY IF EXISTS notifications_update_own_warehouse ON public.app_notifications;
DROP POLICY IF EXISTS notifications_delete_own_warehouse ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_read_own_warehouse" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_update_own_warehouse" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_delete_own_warehouse" ON public.app_notifications;

DROP POLICY IF EXISTS notifications_read_own_public_user ON public.app_notifications;
DROP POLICY IF EXISTS notifications_update_own_public_user ON public.app_notifications;
DROP POLICY IF EXISTS notifications_delete_own_public_user ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_read_own_public_user" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_update_own_public_user" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_delete_own_public_user" ON public.app_notifications;

DROP POLICY IF EXISTS notifications_admin_all ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_admin_all" ON public.app_notifications;

DROP POLICY IF EXISTS notifications_read_own_recipient ON public.app_notifications;
DROP POLICY IF EXISTS notifications_update_own_recipient ON public.app_notifications;
DROP POLICY IF EXISTS notifications_delete_own_recipient ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_read_own_recipient" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_update_own_recipient" ON public.app_notifications;
DROP POLICY IF EXISTS "notifications_delete_own_recipient" ON public.app_notifications;

CREATE POLICY notifications_read_own_recipient
ON public.app_notifications
FOR SELECT
TO authenticated
USING (
  app_notifications.user_id = auth.uid()
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.is_active = true
  )
);

CREATE POLICY notifications_update_own_recipient
ON public.app_notifications
FOR UPDATE
TO authenticated
USING (
  app_notifications.user_id = auth.uid()
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.is_active = true
  )
)
WITH CHECK (
  app_notifications.user_id = auth.uid()
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.is_active = true
  )
);

CREATE POLICY notifications_delete_own_recipient
ON public.app_notifications
FOR DELETE
TO authenticated
USING (
  app_notifications.user_id = auth.uid()
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.is_active = true
  )
);

GRANT SELECT, UPDATE, DELETE ON public.app_notifications TO authenticated;

CREATE INDEX IF NOT EXISTS idx_notifications_user_id_read
  ON public.app_notifications(user_id, read);
