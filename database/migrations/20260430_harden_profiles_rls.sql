-- ============================================
-- Migration: Harden profiles RLS and protected fields
-- Date: 2026-04-30
-- Phase: 4.5.1B
-- ============================================
--
-- public.profiles is an authorization source for orders RLS/RPC logic.
-- Normal authenticated clients must not control role, tenant, activation,
-- approval, or admin-controlled fields.
--
-- This migration intentionally denies direct authenticated profile inserts.
-- Public user profile creation must use the whitelisted SECURITY DEFINER RPC.

-- ============================================
-- Step 1: Enable RLS
-- ============================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- FORCE ROW LEVEL SECURITY is intentionally not enabled here. Service/admin
-- maintenance paths are not fully documented locally and must be inventoried
-- before forcing owner-level RLS behavior.

-- ============================================
-- Step 2: Reset existing profiles policies
-- ============================================

DO $$
DECLARE
    policy_record RECORD;
BEGIN
    FOR policy_record IN
        SELECT policyname
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'profiles'
    LOOP
        EXECUTE format(
            'DROP POLICY IF EXISTS %I ON public.profiles',
            policy_record.policyname
        );
    END LOOP;
END;
$$;

-- ============================================
-- Step 3: Authenticated own-row SELECT
-- ============================================

CREATE POLICY profiles_select_own
ON public.profiles
FOR SELECT
TO authenticated
USING (id = auth.uid());

-- ============================================
-- Step 4: Authenticated own-row safe UPDATE
-- ============================================

CREATE POLICY profiles_update_own_safe_fields
ON public.profiles
FOR UPDATE
TO authenticated
USING (id = auth.uid())
WITH CHECK (id = auth.uid());

-- ============================================
-- Step 5: Protected-field immutability trigger
-- ============================================

CREATE OR REPLACE FUNCTION public.prevent_profiles_protected_field_update()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Block normal authenticated clients from changing profile trust fields.
    -- Service-role/admin maintenance must use trusted server-side paths.
    IF auth.role() = 'authenticated' THEN
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'profiles.id cannot be changed'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.account_type IS DISTINCT FROM OLD.account_type THEN
            RAISE EXCEPTION 'profiles.account_type is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.pharmacy_id IS DISTINCT FROM OLD.pharmacy_id THEN
            RAISE EXCEPTION 'profiles.pharmacy_id is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.warehouse_id IS DISTINCT FROM OLD.warehouse_id THEN
            RAISE EXCEPTION 'profiles.warehouse_id is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.is_active IS DISTINCT FROM OLD.is_active THEN
            RAISE EXCEPTION 'profiles.is_active is admin controlled'
                USING ERRCODE = '42501';
        END IF;
    END IF;

    IF to_jsonb(NEW) ? 'updated_at' THEN
        NEW.updated_at := now();
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS prevent_profiles_protected_field_update
ON public.profiles;

CREATE TRIGGER prevent_profiles_protected_field_update
BEFORE UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.prevent_profiles_protected_field_update();

-- ============================================
-- Step 6: Trusted public user profile creation RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.create_public_user_profile(
    p_full_name text DEFAULT NULL,
    p_phone_number text DEFAULT NULL,
    p_pharmacy_name text DEFAULT NULL,
    p_pharmacy_location text DEFAULT NULL,
    p_warehouse_name text DEFAULT NULL,
    p_warehouse_location text DEFAULT NULL
)
RETURNS public.profiles
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_profile public.profiles%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Authentication required'
            USING ERRCODE = '42501';
    END IF;

    INSERT INTO public.profiles (
        id,
        phone_number,
        full_name,
        account_type,
        pharmacy_name,
        pharmacy_location,
        warehouse_name,
        warehouse_location,
        pharmacy_id,
        warehouse_id,
        is_active
    )
    VALUES (
        auth.uid(),
        p_phone_number,
        p_full_name,
        'PUBLIC_USER',
        p_pharmacy_name,
        p_pharmacy_location,
        p_warehouse_name,
        p_warehouse_location,
        NULL,
        NULL,
        TRUE
    )
    RETURNING *
    INTO v_profile;

    RETURN v_profile;
END;
$$;

-- ============================================
-- Step 7: Grants and revokes
-- ============================================

REVOKE INSERT ON public.profiles FROM authenticated;
REVOKE UPDATE ON public.profiles FROM authenticated;
REVOKE DELETE ON public.profiles FROM authenticated;

GRANT SELECT ON public.profiles TO authenticated;

GRANT UPDATE (
    full_name,
    phone_number,
    pharmacy_name,
    pharmacy_location,
    warehouse_name,
    warehouse_location
) ON public.profiles TO authenticated;

GRANT EXECUTE ON FUNCTION public.create_public_user_profile(
    text,
    text,
    text,
    text,
    text,
    text
) TO authenticated;

-- Intentionally no authenticated INSERT policy.
-- Intentionally no authenticated DELETE policy.
