-- ============================================
-- Migration: Customer Price Acceptance/Rejection
-- Date: 2026-05-09
-- Purpose: Allow PUBLIC_USER to accept or reject pharmacy's confirmed price
-- ============================================

-- ============================================
-- RPC: customer_accept_order_price
-- Purpose: Customer accepts the pharmacy's confirmed price
-- Business Rules:
--   - Only PUBLIC_USER can accept
--   - Only CONFIRMED orders can be accepted
--   - Must be order owner (customer_id matches)
--   - After acceptance, order moves to IN_PROGRESS (will be updated by pharmacy to READY_FOR_PICKUP or OUT_FOR_DELIVERY)
-- ============================================
CREATE OR REPLACE FUNCTION public.customer_accept_order_price(
    p_order_id text
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
    v_customer_id uuid;
BEGIN
    -- Get customer info
    SELECT p.account_type, p.id
    INTO v_account_type, v_customer_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PUBLIC_USER' THEN
        RAISE EXCEPTION 'Only PUBLIC_USER can accept order prices'
            USING ERRCODE = '42501';
    END IF;

    -- Get order
    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    -- Validate order ownership and state
    IF v_order.customer_id IS DISTINCT FROM v_customer_id THEN
        RAISE EXCEPTION 'Cannot accept price for order that does not belong to you'
            USING ERRCODE = '42501';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY' THEN
        RAISE EXCEPTION 'Can only accept prices for CUSTOMER_PHARMACY orders'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.status IS DISTINCT FROM 'CONFIRMED' THEN
        RAISE EXCEPTION 'Can only accept price for CONFIRMED orders. Current status: %', v_order.status
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.total_price_cents IS NULL OR v_order.total_price_cents < 0 THEN
        RAISE EXCEPTION 'Cannot accept order without a non-negative confirmed price'
            USING ERRCODE = 'P0001';
    END IF;

    -- Update order to IN_PROGRESS
    -- Pharmacy will later update to READY_FOR_PICKUP or OUT_FOR_DELIVERY based on fulfillment_type
    UPDATE public.orders o
    SET status = 'IN_PROGRESS',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'CONFIRMED'
      AND o.customer_id = v_customer_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order acceptance did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    -- Send notification to pharmacy
    INSERT INTO public.app_notifications (
        pharmacy_id,
        title,
        body,
        read,
        created_at
    ) VALUES (
        v_order.pharmacy_id,
        'تم قبول الطلب',
        format('العميل قبل طلب %s - ابدأ بالتجهيز', v_order.medicine_name),
        false,
        NOW()
    );

    RETURN v_updated;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION public.customer_accept_order_price(text) TO authenticated;

-- ============================================
-- RPC: customer_reject_order_price
-- Purpose: Customer rejects the pharmacy's confirmed price
-- Business Rules:
--   - Only PUBLIC_USER can reject
--   - Only CONFIRMED orders can be rejected
--   - Must be order owner (customer_id matches)
--   - After rejection, order moves to REJECTED status
-- ============================================
CREATE OR REPLACE FUNCTION public.customer_reject_order_price(
    p_order_id text
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
    v_customer_id uuid;
BEGIN
    -- Get customer info
    SELECT p.account_type, p.id
    INTO v_account_type, v_customer_id
    FROM public.profiles p
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PUBLIC_USER' THEN
        RAISE EXCEPTION 'Only PUBLIC_USER can reject order prices'
            USING ERRCODE = '42501';
    END IF;

    -- Get order
    SELECT o.*
    INTO v_order
    FROM public.orders o
    WHERE o.id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order not found'
            USING ERRCODE = 'P0001';
    END IF;

    -- Validate order ownership and state
    IF v_order.customer_id IS DISTINCT FROM v_customer_id THEN
        RAISE EXCEPTION 'Cannot reject price for order that does not belong to you'
            USING ERRCODE = '42501';
    END IF;

    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY' THEN
        RAISE EXCEPTION 'Can only reject prices for CUSTOMER_PHARMACY orders'
            USING ERRCODE = 'P0001';
    END IF;

    IF v_order.status IS DISTINCT FROM 'CONFIRMED' THEN
        RAISE EXCEPTION 'Can only reject price for CONFIRMED orders. Current status: %', v_order.status
            USING ERRCODE = 'P0001';
    END IF;

    -- Update order to REJECTED
    UPDATE public.orders o
    SET status = 'REJECTED',
        updated_at = NOW()
    WHERE o.id = p_order_id
      AND o.status = 'CONFIRMED'
      AND o.customer_id = v_customer_id
      AND o.order_type = 'CUSTOMER_PHARMACY'
    RETURNING o.*
    INTO v_updated;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order rejection did not update a row'
            USING ERRCODE = 'P0001';
    END IF;

    -- Send notification to pharmacy
    INSERT INTO public.app_notifications (
        pharmacy_id,
        title,
        body,
        read,
        created_at
    ) VALUES (
        v_order.pharmacy_id,
        'تم رفض الطلب',
        format('العميل رفض طلب %s', v_order.medicine_name),
        false,
        NOW()
    );

    RETURN v_updated;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION public.customer_reject_order_price(text) TO authenticated;

-- ============================================
-- Verification Queries
-- ============================================
-- Run these to verify the functions were created:
-- SELECT pg_get_functiondef('public.customer_accept_order_price'::regproc);
-- SELECT pg_get_functiondef('public.customer_reject_order_price'::regproc);

-- Test scenario (run as PUBLIC_USER):
-- 1. Create order (status: PENDING)
-- 2. Pharmacy confirms with price (status: CONFIRMED)
-- 3. Customer accepts price: SELECT * FROM customer_accept_order_price('order-id');
--    Expected: status changes to IN_PROGRESS, notification sent to pharmacy
-- 4. OR Customer rejects price: SELECT * FROM customer_reject_order_price('order-id');
--    Expected: status changes to REJECTED, notification sent to pharmacy

