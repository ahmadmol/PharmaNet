-- ============================================
-- Migration: Secure support requests via SECURITY DEFINER RPC
-- Date: 2026-05-08
-- ============================================

-- 1) Support requests table
CREATE TABLE IF NOT EXISTS public.support_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_user_id uuid NOT NULL REFERENCES auth.users(id),
    sender_account_type text NOT NULL,
    subject text NOT NULL,
    message text NOT NULL,
    category text,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 2) Enable RLS
ALTER TABLE public.support_requests ENABLE ROW LEVEL SECURITY;

-- 3) Sender can read only own support requests
DROP POLICY IF EXISTS support_requests_select_own ON public.support_requests;
CREATE POLICY support_requests_select_own
ON public.support_requests
FOR SELECT
TO authenticated
USING (sender_user_id = auth.uid());

-- 4) Avoid direct insert/update/delete by normal authenticated users
REVOKE INSERT, UPDATE, DELETE ON public.support_requests FROM authenticated;
GRANT SELECT ON public.support_requests TO authenticated;

-- 5) RPC for secure support submission + admin fan-out notifications
CREATE OR REPLACE FUNCTION public.submit_support_request(
    p_subject text,
    p_message text,
    p_category text DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_sender public.profiles%ROWTYPE;
    v_support_request_id uuid;
    v_subject text;
    v_message text;
    v_category text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Authentication required'
            USING ERRCODE = '42501';
    END IF;

    SELECT * INTO v_sender
    FROM public.profiles
    WHERE id = auth.uid();

    IF v_sender.id IS NULL THEN
        RAISE EXCEPTION 'Profile not found for authenticated user'
            USING ERRCODE = '42501';
    END IF;

    IF v_sender.account_type = 'ADMIN' THEN
        RAISE EXCEPTION 'Support requests are intended for non-admin accounts'
            USING ERRCODE = '42501';
    END IF;

    v_subject := trim(coalesce(p_subject, ''));
    v_message := trim(coalesce(p_message, ''));
    v_category := nullif(trim(coalesce(p_category, '')), '');

    IF v_subject = '' THEN
        RAISE EXCEPTION 'Subject must not be blank'
            USING ERRCODE = '22023';
    END IF;

    IF v_message = '' THEN
        RAISE EXCEPTION 'Message must not be blank'
            USING ERRCODE = '22023';
    END IF;

    INSERT INTO public.support_requests (
        sender_user_id,
        sender_account_type,
        subject,
        message,
        category
    ) VALUES (
        auth.uid(),
        v_sender.account_type::text,
        v_subject,
        v_message,
        v_category
    )
    RETURNING id INTO v_support_request_id;

    INSERT INTO public.app_notifications (
        user_id,
        title,
        body,
        read,
        created_at
    )
    SELECT
        p.id,
        'طلب دعم: ' || v_subject,
        'المرسل: ' || coalesce(nullif(trim(v_sender.full_name), ''), auth.uid()::text) || E'\n'
            || 'الفئة: ' || coalesce(v_category, 'عام') || E'\n'
            || v_message,
        false,
        now()
    FROM public.profiles p
    WHERE p.account_type = 'ADMIN'
      AND p.is_active = true;

    RETURN v_support_request_id;
END;
$$;

-- 6) Allow authenticated clients to call the RPC
GRANT EXECUTE ON FUNCTION public.submit_support_request(text, text, text) TO authenticated;
