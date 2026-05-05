-- ============================================
-- Migration: Add RLS policies to medicines table
-- Date: 2026-05-04
-- Phase: Security Hardening
-- ============================================
--
-- Purpose: Protect medicines table with Row Level Security
-- - All authenticated users can READ medicines (public catalog)
-- - Only ADMIN can INSERT/UPDATE/DELETE medicines

-- ============================================
-- Step 1: Enable RLS on medicines table
-- ============================================

ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;

-- ============================================
-- Step 2: Allow all authenticated users to READ medicines
-- ============================================

CREATE POLICY "medicines_read_authenticated"
ON public.medicines
FOR SELECT
TO authenticated
USING (true);

-- ============================================
-- Step 3: Only ADMIN can INSERT/UPDATE/DELETE medicines
-- ============================================

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

-- ============================================
-- Step 4: Grant permissions
-- ============================================

GRANT SELECT ON public.medicines TO authenticated;

-- Migration complete
