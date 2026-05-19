-- ============================================
-- MIGRATION: Add Maps Coordinates (Phase 1)
-- Date: 2026-05-11
-- Purpose: Add latitude and longitude to profiles and orders to support Maps integration.
-- ============================================

-- Step 1: Add latitude and longitude to profiles
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Step 2: Add delivery_latitude and delivery_longitude to orders
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS delivery_latitude DOUBLE PRECISION;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS delivery_longitude DOUBLE PRECISION;

-- ============================================
-- FUTURE RPC / REPOSITORY UPDATE NOTES:
-- 1. `public.create_public_user_profile` RPC must be updated to accept `p_latitude` and `p_longitude` DOUBLE PRECISION parameters, and store them.
-- 2. Profile update queries in `SupabasePharmaRepository` must support sending coordinates.
-- 3. Customer order creation logic must be updated to pass `delivery_latitude` and `delivery_longitude` when `FulfillmentType == DELIVERY`.
-- ============================================
