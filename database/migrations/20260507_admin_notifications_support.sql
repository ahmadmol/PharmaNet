-- ====================================================================
-- Admin Notifications Support
-- Purpose: Extend app_notifications to support ADMIN users
-- Date: 2026-05-07
-- ====================================================================

-- ============================================
-- Step 1: Add user_id column to app_notifications
-- ============================================

-- Add user_id column if it doesn't exist (for ADMIN and other non-pharmacy users)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'app_notifications' 
        AND column_name = 'user_id'
    ) THEN
        ALTER TABLE public.app_notifications 
        ADD COLUMN user_id UUID REFERENCES auth.users(id);
    END IF;
END $$;

-- ============================================
-- Step 2: Add RLS policy for ADMIN users
-- ============================================

-- ADMIN can read their own notifications (user_id based)
CREATE POLICY IF NOT EXISTS "notifications_read_own_admin"
ON public.app_notifications
FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.profiles
        WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
        AND app_notifications.user_id = auth.uid()
    )
);

-- ADMIN can update their own notifications (mark as read)
CREATE POLICY IF NOT EXISTS "notifications_update_own_admin"
ON public.app_notifications
FOR UPDATE
USING (
    EXISTS (
        SELECT 1 FROM public.profiles
        WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
        AND app_notifications.user_id = auth.uid()
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.profiles
        WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
        AND app_notifications.user_id = auth.uid()
    )
);

-- ADMIN can delete their own notifications
CREATE POLICY IF NOT EXISTS "notifications_delete_own_admin"
ON public.app_notifications
FOR DELETE
USING (
    EXISTS (
        SELECT 1 FROM public.profiles
        WHERE profiles.id = auth.uid()
        AND profiles.account_type = 'ADMIN'
        AND profiles.is_active = true
        AND app_notifications.user_id = auth.uid()
    )
);

-- ============================================
-- Step 3: Add index for user_id lookups
-- ============================================

CREATE INDEX IF NOT EXISTS idx_notifications_user_id 
    ON public.app_notifications(user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read 
    ON public.app_notifications(user_id, read);

-- ============================================
-- Step 4: Add helper RPC for creating admin notifications (optional)
-- ============================================

-- This RPC can be used by system/backend to create notifications for admin users
CREATE OR REPLACE FUNCTION create_admin_notification(
    p_user_id UUID,
    p_title TEXT,
    p_body TEXT,
    p_type TEXT DEFAULT 'INFO',
    p_category TEXT DEFAULT 'SUPPORT',
    p_requires_action BOOLEAN DEFAULT false,
    p_destination TEXT DEFAULT NULL,
    p_destination_id TEXT DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_notification_id UUID;
BEGIN
    -- Verify target user is ADMIN
    IF NOT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = p_user_id
        AND account_type = 'ADMIN'
    ) THEN
        RAISE EXCEPTION 'Target user % is not an ADMIN', p_user_id;
    END IF;
    
    -- Insert notification
    INSERT INTO public.app_notifications (
        user_id,
        title,
        body,
        type,
        category,
        requires_action,
        destination,
        destination_id,
        read,
        created_at
    ) VALUES (
        p_user_id,
        p_title,
        p_body,
        p_type,
        p_category,
        p_requires_action,
        p_destination,
        p_destination_id,
        false,
        now()
    )
    RETURNING id INTO v_notification_id;
    
    RETURN v_notification_id;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION create_admin_notification TO authenticated;

-- ============================================
-- Migration Complete
-- ============================================

-- Note: Existing pharmacy_id based notifications remain unchanged
-- ADMIN notifications use user_id column
-- Both can coexist in the same table
