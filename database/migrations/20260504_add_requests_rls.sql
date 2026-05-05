-- ============================================
-- Migration: Add RLS policies to requests table
-- Date: 2026-05-04
-- Phase: Security Hardening
-- ============================================
--
-- Purpose: Protect requests table with Row Level Security
-- - PHARMACY can read/write their own requests
-- - WAREHOUSE can read incoming requests and update status
-- - ADMIN has full access

-- ============================================
-- Step 1: Enable RLS on requests table
-- ============================================

ALTER TABLE public.requests ENABLE ROW LEVEL SECURITY;

-- ============================================
-- Step 2: PHARMACY can read their own requests
-- ============================================

CREATE POLICY "requests_pharmacy_read_own"
ON public.requests
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
-- Step 3: WAREHOUSE can read incoming requests
-- ============================================

CREATE POLICY "requests_warehouse_read_incoming"
ON public.requests
FOR SELECT
TO authenticated
USING (
    warehouse_id = (
        SELECT warehouse_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'WAREHOUSE'
    )
);

-- ============================================
-- Step 4: PHARMACY can INSERT new requests
-- ============================================

CREATE POLICY "requests_pharmacy_insert"
ON public.requests
FOR INSERT
TO authenticated
WITH CHECK (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
    AND status IN ('DRAFT', 'PENDING')
);

-- ============================================
-- Step 5: PHARMACY can UPDATE their own DRAFT/PENDING requests
-- ============================================

CREATE POLICY "requests_pharmacy_update_own"
ON public.requests
FOR UPDATE
TO authenticated
USING (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
    AND status IN ('DRAFT', 'PENDING')
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
-- Step 6: PHARMACY can DELETE their own DRAFT requests
-- ============================================

CREATE POLICY "requests_pharmacy_delete_draft"
ON public.requests
FOR DELETE
TO authenticated
USING (
    pharmacy_id = (
        SELECT pharmacy_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'PHARMACY'
    )
    AND status = 'DRAFT'
);

-- ============================================
-- Step 7: WAREHOUSE can UPDATE status on incoming requests
-- ============================================

CREATE POLICY "requests_warehouse_update_status"
ON public.requests
FOR UPDATE
TO authenticated
USING (
    warehouse_id = (
        SELECT warehouse_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'WAREHOUSE'
    )
)
WITH CHECK (
    warehouse_id = (
        SELECT warehouse_id 
        FROM public.profiles
        WHERE id = auth.uid() 
          AND account_type = 'WAREHOUSE'
    )
);

-- ============================================
-- Step 8: ADMIN full access
-- ============================================

CREATE POLICY "requests_admin_all"
ON public.requests
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
-- Step 9: Grant permissions
-- ============================================

GRANT SELECT, INSERT, UPDATE, DELETE ON public.requests TO authenticated;

-- Migration complete
