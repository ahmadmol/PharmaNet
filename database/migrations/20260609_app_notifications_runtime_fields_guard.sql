-- Guard app_notifications runtime fields used by Kotlin and lifecycle RPCs.
-- Safe on already-migrated databases; no recipient, FCM, or routing behavior changes.

ALTER TABLE public.app_notifications
  ADD COLUMN IF NOT EXISTS type text NOT NULL DEFAULT 'INFO',
  ADD COLUMN IF NOT EXISTS category text NOT NULL DEFAULT 'ORDERS',
  ADD COLUMN IF NOT EXISTS destination text,
  ADD COLUMN IF NOT EXISTS destination_id text,
  ADD COLUMN IF NOT EXISTS requires_action boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_app_notifications_destination
  ON public.app_notifications(destination, destination_id);
