-- ============================================
-- Migration: Add RLS policies to app_notifications table
-- Date: 2026-05-04
-- Phase: Security Hardening
-- ============================================
--
-- Purpose: Protect app_notifications table with Row Level Security
-- - PHARMACY can read/update/delete their own notifications
-- - ADMIN has full access
-- - Note: Currently notifications are pharmacy-scoped only
--   Future: Add support for WAREHOUSE and PUBLIC_USER notifications

-- ============================================
-- Step 1: Enable RLS on app_notifications table
-- ============================================

ALTER TABLE public.app_notifications ENABLE ROW LEVEL SECURITY;

-- ============================================
-- Step 2: PHARMACY can read their own notifications
-- ============================================

CREATE POLICY "notifications_read_own_pharmacy"
ON public.app_notifications
FOR SELECT
TO authenticated
USING (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
);

-- ============================================
-- Step 3: PHARMACY can update their own notifications (mark as read)
-- ============================================

CREATE POLICY "notifications_update_own_pharmacy"
ON public.app_notifications
FOR UPDATE
TO authenticated
USING (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
)
WITH CHECK (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
);

-- ============================================
-- Step 4: PHARMACY can delete their own notifications
-- ============================================

CREATE POLICY "notifications_delete_own_pharmacy"
ON public.app_notifications
FOR DELETE
TO authenticated
USING (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
);

-- ============================================
-- Step 5: ADMIN full access
-- ============================================

CREATE POLICY "notifications_admin_all"
ON public.app_notifications
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

-- ============================================
-- Step 6: Grant permissions
-- ============================================

GRANT SELECT, UPDATE, DELETE ON public.app_notifications TO authenticated;

-- ============================================
-- Step 7: Create indexes for performance
-- ============================================

CREATE INDEX IF NOT EXISTS idx_notifications_pharmacy_id 
    ON public.app_notifications(pharmacy_id);

CREATE INDEX IF NOT EXISTS idx_notifications_read 
    ON public.app_notifications(pharmacy_id, read);

-- Migration complete
