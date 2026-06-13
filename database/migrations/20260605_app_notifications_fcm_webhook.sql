-- Send Android FCM pushes after app_notifications inserts.
-- Backend-only layer: app_notifications schema, Android client, and token lifecycle remain unchanged.

CREATE EXTENSION IF NOT EXISTS pg_net WITH SCHEMA extensions;

CREATE TABLE IF NOT EXISTS public.app_runtime_settings (
  key text PRIMARY KEY,
  value text NOT NULL
);

REVOKE ALL ON public.app_runtime_settings FROM PUBLIC;
REVOKE ALL ON public.app_runtime_settings FROM anon;
REVOKE ALL ON public.app_runtime_settings FROM authenticated;

INSERT INTO public.app_runtime_settings (key, value)
VALUES
  ('supabase_url', 'https://zispernfhcsbjnhymepc.supabase.co'),
  ('fcm_webhook_secret', 'CHANGE_ME_FCM_WEBHOOK_SECRET')
ON CONFLICT (key) DO NOTHING;

CREATE OR REPLACE FUNCTION public.notify_app_notification_insert_fcm()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
  v_supabase_url text;
  v_webhook_secret text;
  v_function_url text;
BEGIN
  IF NEW.user_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT value
  INTO v_supabase_url
  FROM public.app_runtime_settings
  WHERE key = 'supabase_url';

  SELECT value
  INTO v_webhook_secret
  FROM public.app_runtime_settings
  WHERE key = 'fcm_webhook_secret';

  v_supabase_url := nullif(btrim(coalesce(v_supabase_url, '')), '');
  v_webhook_secret := nullif(btrim(coalesce(v_webhook_secret, '')), '');

  IF v_supabase_url IS NULL OR v_webhook_secret IS NULL THEN
    RAISE LOG 'FCM webhook skipped: supabase_url or fcm_webhook_secret runtime setting is missing for notification %', NEW.id;
    RETURN NEW;
  END IF;

  v_function_url := regexp_replace(v_supabase_url, '/+$', '') || '/functions/v1/send-fcm-notification';

  PERFORM net.http_post(
    url := v_function_url,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || v_webhook_secret
    ),
    body := jsonb_build_object(
      'type', 'INSERT',
      'schema', TG_TABLE_SCHEMA,
      'table', TG_TABLE_NAME,
      'record', to_jsonb(NEW)
    ),
    timeout_milliseconds := 5000
  );

  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    RAISE LOG 'FCM webhook enqueue failed for notification %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS app_notifications_fcm_after_insert ON public.app_notifications;
CREATE TRIGGER app_notifications_fcm_after_insert
AFTER INSERT ON public.app_notifications
FOR EACH ROW
EXECUTE FUNCTION public.notify_app_notification_insert_fcm();
