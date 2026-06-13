-- Store Android FCM registration tokens for authenticated users.
-- Client scope only: no Edge Function, no push sending, no trigger/webhook.

CREATE TABLE IF NOT EXISTS public.user_fcm_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  token text NOT NULL UNIQUE,
  platform text NOT NULL DEFAULT 'android',
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT user_fcm_tokens_platform_check
    CHECK (platform IN ('android'))
);

CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user_id
  ON public.user_fcm_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_active_seen
  ON public.user_fcm_tokens(is_active, last_seen_at DESC);

CREATE OR REPLACE FUNCTION public.set_user_fcm_tokens_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS set_user_fcm_tokens_updated_at ON public.user_fcm_tokens;
CREATE TRIGGER set_user_fcm_tokens_updated_at
BEFORE UPDATE ON public.user_fcm_tokens
FOR EACH ROW
EXECUTE FUNCTION public.set_user_fcm_tokens_updated_at();

ALTER TABLE public.user_fcm_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_fcm_tokens_select_own ON public.user_fcm_tokens;
DROP POLICY IF EXISTS user_fcm_tokens_insert_own ON public.user_fcm_tokens;
DROP POLICY IF EXISTS user_fcm_tokens_update_own ON public.user_fcm_tokens;
DROP POLICY IF EXISTS user_fcm_tokens_delete_own ON public.user_fcm_tokens;

CREATE POLICY user_fcm_tokens_select_own
ON public.user_fcm_tokens
FOR SELECT
TO authenticated
USING (user_id = auth.uid());

CREATE POLICY user_fcm_tokens_insert_own
ON public.user_fcm_tokens
FOR INSERT
TO authenticated
WITH CHECK (user_id = auth.uid());

CREATE POLICY user_fcm_tokens_update_own
ON public.user_fcm_tokens
FOR UPDATE
TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

CREATE POLICY user_fcm_tokens_delete_own
ON public.user_fcm_tokens
FOR DELETE
TO authenticated
USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_fcm_tokens TO authenticated;

CREATE OR REPLACE FUNCTION public.sync_user_fcm_token(
  p_token text,
  p_platform text DEFAULT 'android'
)
RETURNS public.user_fcm_tokens
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_user_id uuid := auth.uid();
  v_token text := btrim(coalesce(p_token, ''));
  v_platform text := btrim(coalesce(p_platform, 'android'));
  v_row public.user_fcm_tokens;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'sync_user_fcm_token requires an authenticated user'
      USING ERRCODE = '28000';
  END IF;

  IF v_token = '' THEN
    RAISE EXCEPTION 'sync_user_fcm_token requires a non-empty token'
      USING ERRCODE = '22023';
  END IF;

  IF v_platform <> 'android' THEN
    RAISE EXCEPTION 'unsupported FCM platform: %', v_platform
      USING ERRCODE = '22023';
  END IF;

  UPDATE public.user_fcm_tokens
  SET
    is_active = false,
    updated_at = now()
  WHERE token = v_token
    AND user_id <> v_user_id
    AND is_active = true;

  INSERT INTO public.user_fcm_tokens (
    user_id,
    token,
    platform,
    is_active,
    last_seen_at
  )
  VALUES (
    v_user_id,
    v_token,
    v_platform,
    true,
    now()
  )
  ON CONFLICT (token) DO UPDATE
  SET
    user_id = EXCLUDED.user_id,
    platform = EXCLUDED.platform,
    is_active = true,
    last_seen_at = now(),
    updated_at = now()
  RETURNING * INTO v_row;

  RETURN v_row;
END;
$$;

REVOKE ALL ON FUNCTION public.sync_user_fcm_token(text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sync_user_fcm_token(text, text) TO authenticated;
