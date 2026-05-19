-- ============================================
-- Migration: Add B2C Price Suggested Notification
-- Date: 2026-05-09
-- Purpose: Send notification to PUBLIC_USER when pharmacy confirms order with price
-- ============================================

-- Update confirm_customer_order to send notification
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
    v_pharmacy_name text;
    v_customer_id uuid;
    v_medicine_name text;
    v_price_sar numeric;
BEGIN
    IF p_total_price_cents IS NULL OR p_total_price_cents < 0 THEN
        RAISE EXCEPTION 'Confirmed price must be non-null and non-negative'
            USING ERRCODE = 'P0001';
    END IF;

    -- Get pharmacy info
    SELECT p.account_type, p.pharmacy_id, ph.name
    INTO v_account_type, v_pharmacy_id, v_pharmacy_name
    FROM public.profiles p
    LEFT JOIN public.pharmacies ph ON ph.id = p.pharmacy_id
    WHERE p.id = auth.uid();

    IF v_account_type IS DISTINCT FROM 'PHARMACY' OR v_pharmacy_id IS NULL THEN
        RAISE EXCEPTION 'Only pharmacies can confirm customer orders'
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

    -- Validate order
    IF v_order.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
        OR v_order.pharmacy_id IS DISTINCT FROM v_pharmacy_id
        OR v_order.status IS DISTINCT FROM 'PENDING'
        OR v_order.customer_id IS NULL
        OR v_order.warehouse_id IS NOT NULL
        OR v_order.request_id IS NOT NULL THEN
        RAISE EXCEPTION 'Order cannot be confirmed by this pharmacy'
            USING ERRCODE = '42501';
    END IF;

    -- Store values for notification
    v_customer_id := v_order.customer_id;
    v_medicine_name := v_order.medicine_name;
    v_price_sar := p_total_price_cents / 100.0;

    -- Update order
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

    -- Send notification to PUBLIC_USER
    -- Note: app_notifications table has user_id column for PUBLIC_USER notifications
    INSERT INTO public.app_notifications (
        user_id,
        title,
        body,
        read,
        created_at
    ) VALUES (
        v_customer_id,
        'تم تأكيد طلبك',
        format('صيدلية %s أكدت طلبك %s بسعر %s ريال', 
            COALESCE(v_pharmacy_name, 'الصيدلية'), 
            v_medicine_name, 
            v_price_sar::text
        ),
        false,
        NOW()
    );

    RETURN v_updated;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION public.confirm_customer_order(text, bigint) TO authenticated;

-- ============================================
-- Verification Query
-- ============================================
-- Run this to verify the function was updated:
-- SELECT pg_get_functiondef('public.confirm_customer_order'::regproc);
