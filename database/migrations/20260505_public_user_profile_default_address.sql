-- ============================================
-- MIGRATION: PUBLIC_USER Profile Default Address
-- Date: 2026-05-05
-- Phase: 4.6 PUBLIC_USER
-- Purpose: Add default_address field for PUBLIC_USER customer profiles
-- ============================================

-- Step 1: Add default_address column if not exists
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS default_address text;

-- Step 2: Grant UPDATE permission for PUBLIC_USER safe fields only
-- Note: This uses column-level grants to restrict what PUBLIC_USER can update
GRANT UPDATE (
    full_name,
    phone_number,
    default_address
) ON public.profiles TO authenticated;

-- Step 3: Ensure RLS policy allows PUBLIC_USER to update own profile safe fields
-- The existing update_own_profile policy should already cover this
-- But we verify it exists and is correct
DROP POLICY IF EXISTS update_own_profile ON public.profiles;
CREATE POLICY update_own_profile ON public.profiles
    FOR UPDATE TO authenticated
    USING (id = auth.uid())
    WITH CHECK (
        id = auth.uid()
        -- Prevent PUBLIC_USER from changing critical fields
        AND account_type = (SELECT account_type FROM public.profiles WHERE id = auth.uid())
        AND pharmacy_id IS NOT DISTINCT FROM (SELECT pharmacy_id FROM public.profiles WHERE id = auth.uid())
        AND warehouse_id IS NOT DISTINCT FROM (SELECT warehouse_id FROM public.profiles WHERE id = auth.uid())
        AND is_active = (SELECT is_active FROM public.profiles WHERE id = auth.uid())
    );

-- ============================================
-- MIGRATION COMPLETE
-- ============================================
