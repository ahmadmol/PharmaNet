-- ============================================
-- MIGRATION: PUBLIC_USER Pharmacy Discovery
-- Date: 2026-05-05
-- Phase: 4.6 PUBLIC_USER
-- Purpose: Enable PUBLIC_USER to discover pharmacies for medicine orders
-- ============================================

-- Step 1: Create RPC for PUBLIC_USER pharmacy discovery
-- Returns customer-safe pharmacy fields only
CREATE OR REPLACE FUNCTION public.get_public_pharmacies_for_medicine(p_medicine_id uuid)
RETURNS TABLE (
    pharmacy_id text,
    pharmacy_name text,
    location text,
    supports_delivery boolean,
    supports_pickup boolean,
    is_on_duty boolean,
    availability_status text,
    estimated_time_label text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Return all active pharmacies with customer-safe fields
    -- In future, this can be enhanced with medicine-pharmacy inventory linkage
    RETURN QUERY
    SELECT
        ph.id AS pharmacy_id,
        ph.name AS pharmacy_name,
        ph.location,
        true AS supports_delivery,  -- Default: all pharmacies support delivery
        true AS supports_pickup,    -- Default: all pharmacies support pickup
        false AS is_on_duty,        -- TODO: Implement on-duty logic
        'NEEDS_CONFIRMATION'::text AS availability_status,  -- Default status
        'يرجى الانتظار لتأكيد الصيدلية'::text AS estimated_time_label
    FROM public.pharmacies ph
    WHERE ph.is_active = true
    ORDER BY ph.name;
END;
$$;

-- Step 2: Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION public.get_public_pharmacies_for_medicine(uuid) TO authenticated;

-- Step 3: Add index for pharmacy active status
CREATE INDEX IF NOT EXISTS idx_pharmacies_active ON public.pharmacies(is_active) WHERE is_active = true;

-- ============================================
-- MIGRATION COMPLETE
-- ============================================
