-- ============================================
-- Migration: Harden B2C order RLS and lifecycle RPCs
-- Date: 2026-04-29
-- Phase: 4.5.1
-- ============================================
--
-- This migration fixes the Phase 4.5 P0 findings for CUSTOMER_PHARMACY
-- orders only.
--
-- Broad direct B2C UPDATE policies are intentionally removed. Customer
-- cancellation and pharmacy lifecycle transitions must use the validated
-- RPC functions below.
--
-- Existing PHARMACY_WAREHOUSE / warehouse B2B policies are intentionally
-- preserved and are not modified by this migration.

-- ============================================
-- Step 1: Ensure RLS remains enabled
-- ============================================

ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;

-- ============================================
-- Step 2: Drop risky B2C policies
-- ============================================

DROP POLICY IF EXISTS customer_create_order ON public.orders;
DROP POLICY IF EXISTS customer_cancel_pending ON public.orders;
DROP POLICY IF EXISTS pharmacy_manage_b2c ON public.orders;

-- ============================================
-- Step 3: Recreate hardened customer B2C insert policy
-- ============================================

CREATE POLICY customer_create_order ON public.orders
    FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = customer_id
        AND order_type = 'CUSTOMER_PHARMACY'
        AND status = 'PENDING'
        AND warehouse_id IS NULL
        AND request_id IS NULL
        AND pharmacy_id IS NOT NULL
        AND total_price_cents IS NULL
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = auth.uid()
              AND p.account_type = 'PUBLIC_USER'
        )
    );

-- No direct customer UPDATE policy for B2C cancellation is recreated here.
-- No direct pharmacy UPDATE policy for B2C lifecycle management is recreated here.
-- All B2C lifecycle writes must go through the validated RPC functions below.

-- ============================================
-- Step 4: Customer cancellation RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.cancel_customer_order(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
BEGIN
    SELECT p.account_type
    INTO v_account_type
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PUBLIC_USER' THEN
        RAISE EXCEPTION 'Only public users can cancel customer orders'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.customer_id IS DISTINCT FROM auth.uid()
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be cancelled by this user'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'CANCELLED',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'PENDING'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.customer_id = auth.uid()
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order cancellation did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_updated;
END;
$$;

-- ============================================
-- Step 5: Pharmacy confirm RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.confirm_customer_order(
    p_order_id text,
    p_total_price_cents bigint
)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    IF p_total_price_cents IS NULL OR p_total_price_cents < 0 THEN
        RAISE EXCEPTION 'Confirmed price must be non-null and non-negative'
            USING ERRCODE = 'P0001';
    END IF;

    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can confirm customer orders'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be confirmed by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'CONFIRMED',
        total_price_cents = p_total_price_cents,
        confirmed_at = NOW(),
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'PENDING'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order confirmation did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_updated;
END;
$$;

-- ============================================
-- Step 6: Pharmacy reject RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.reject_customer_order(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can reject customer orders'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be rejected by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'REJECTED',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'PENDING'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order rejection did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_updated;
END;
$$;

-- ============================================
-- Step 7: Pharmacy ready-for-pickup RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.mark_customer_order_ready_for_pickup(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can mark customer orders ready for pickup'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'CONFIRMED'
        OR v_order.fulfillment_type IS DISTINCT FROM 'PICKUP'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be marked ready for pickup by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'READY_FOR_PICKUP',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'CONFIRMED'
      AND o.fulfillment_type = 'PICKUP'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Ready-for-pickup transition did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_updated;
END;
$$;

-- ============================================
-- Step 8: Pharmacy out-for-delivery RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.mark_customer_order_out_for_delivery(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can mark customer orders out for delivery'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'CONFIRMED'
        OR v_order.fulfillment_type IS DISTINCT FROM 'DELIVERY'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be marked out for delivery by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'OUT_FOR_DELIVERY',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'CONFIRMED'
      AND o.fulfillment_type = 'DELIVERY'
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Out-for-delivery transition did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_updated;
END;
$$;

-- ============================================
-- Step 9: Pharmacy delivered RPC
-- ============================================

CREATE OR REPLACE FUNCTION public.mark_customer_order_delivered(p_order_id text)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders%ROWTYPE;
    v_updated public.orders%ROWTYPE;
    v_account_type public.profiles.account_type%TYPE;
    v_pharmacy_id public.profiles.pharmacy_id%TYPE;
BEGIN
    SELECT p.account_type, p.pharmacy_id
    INTO v_account_type, v_pharmacy_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can mark customer orders delivered'
            USING ERRCODE = '42501';
    END IF;

    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL
        OR NOT (
            (v_order.status = 'READY_FOR_PICKUP' AND v_order.fulfillment_type = 'PICKUP')
            OR
            (v_order.status = 'OUT_FOR_DELIVERY' AND v_order.fulfillment_type = 'DELIVERY')
        ) THEN
        RAISE EXCEPTION 'Order cannot be marked delivered by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    UPDATE public.orders o
    SET status = 'DELIVERED',
        fulfilled_at = NOW(),
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
      AND o.pharmacy_id = v_pharmacy_id
      AND (
          (o.status = 'READY_FOR_PICKUP' AND o.fulfillment_type = 'PICKUP')
          OR
          (o.status = 'OUT_FOR_DELIVERY' AND o.fulfillment_type = 'DELIVERY')
      )
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Delivered transition did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_updated;
END;
$$;

-- ============================================
-- Step 10: Grants
-- ============================================

GRANT EXECUTE ON FUNCTION public.cancel_customer_order(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.confirm_customer_order(text, bigint) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reject_customer_order(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_ready_for_pickup(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_out_for_delivery(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_customer_order_delivered(text) TO authenticated;
