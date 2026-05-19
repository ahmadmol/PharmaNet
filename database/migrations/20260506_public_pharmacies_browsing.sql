-- ====================================================================
-- Migration: PUBLIC_USER Pharmacy Browsing
-- Purpose: Allow PUBLIC_USER to browse pharmacies without medicine context
-- Date: 2026-05-06
-- ====================================================================

-- Step 1: Add pharmacy discovery columns (if not exist)
-- Note: These columns may already exist from previous migrations
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'pharmacies' 
                   AND column_name = 'supports_delivery') THEN
        ALTER TABLE public.pharmacies ADD COLUMN supports_delivery BOOLEAN DEFAULT false;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'pharmacies' 
                   AND column_name = 'supports_pickup') THEN
        ALTER TABLE public.pharmacies ADD COLUMN supports_pickup BOOLEAN DEFAULT true;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'pharmacies' 
                   AND column_name = 'is_on_duty') THEN
        ALTER TABLE public.pharmacies ADD COLUMN is_on_duty BOOLEAN DEFAULT false;
    END IF;
END $$;

-- Step 2: Create RPC for general pharmacy browsing
-- This RPC returns all active pharmacies without requiring a medicine_id
-- It's customer-safe and does not expose admin/private fields
CREATE OR REPLACE FUNCTION public.get_public_pharmacies()
RETURNS TABLE (
    pharmacy_id TEXT,
    pharmacy_name TEXT,
    location TEXT,
    area TEXT,
    city TEXT,
    district TEXT,
    supports_delivery BOOLEAN,
    supports_pickup BOOLEAN,
    is_on_duty BOOLEAN,
    availability_status TEXT,
    distance_label TEXT,
    estimated_time_label TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id::TEXT as pharmacy_id,
        p.name as pharmacy_name,
        p.location,
        NULL::TEXT as area,
        NULL::TEXT as city,
        NULL::TEXT as district,
        COALESCE(p.supports_delivery, false) as supports_delivery,
        COALESCE(p.supports_pickup, true) as supports_pickup,
        COALESCE(p.is_on_duty, false) as is_on_duty,
        'UNKNOWN'::TEXT as availability_status,
        NULL::TEXT as distance_label,
        NULL::TEXT as estimated_time_label
    FROM public.pharmacies p
    WHERE p.is_active = true
    ORDER BY p.name;
END;
$$;

-- Step 3: Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION public.get_public_pharmacies() TO authenticated;

-- Step 4: Create index for on-duty filtering (if not exists)
CREATE INDEX IF NOT EXISTS idx_pharmacies_on_duty ON public.pharmacies(is_on_duty) WHERE is_on_duty = true;

-- Step 5: Create index for supports_delivery filtering (if not exists)
CREATE INDEX IF NOT EXISTS idx_pharmacies_supports_delivery ON public.pharmacies(supports_delivery) WHERE supports_delivery = true;

-- Migration complete
-- Note: This migration is safe to run multiple times (idempotent)
