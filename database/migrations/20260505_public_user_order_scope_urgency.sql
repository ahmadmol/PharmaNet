-- ============================================
-- MIGRATION: PUBLIC_USER Order Scope and Urgency
-- Date: 2026-05-05
-- Phase: 4.6 PUBLIC_USER
-- Purpose: Support URGENT/NORMAL and SPECIFIC_PHARMACY/ALL_PHARMACIES for customer orders
-- ============================================

-- Step 1: Add urgency and request_scope columns if not exist
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS urgency text NOT NULL DEFAULT 'URGENT';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS request_scope text NOT NULL DEFAULT 'SPECIFIC_PHARMACY';

-- Step 2: Add CHECK constraints for valid enum values
DO $$
BEGIN
    ALTER TABLE public.orders ADD CONSTRAINT check_urgency_valid
        CHECK (urgency IN ('URGENT', 'NORMAL'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE public.orders ADD CONSTRAINT check_request_scope_valid
        CHECK (request_scope IN ('SPECIFIC_PHARMACY', 'ALL_PHARMACIES'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Step 3: Add constraint for SPECIFIC_PHARMACY requiring pharmacy_id
DO $$
BEGIN
    ALTER TABLE public.orders ADD CONSTRAINT check_specific_pharmacy_has_id
        CHECK (
            request_scope != 'SPECIFIC_PHARMACY' OR
            (request_scope = 'SPECIFIC_PHARMACY' AND pharmacy_id IS NOT NULL)
        );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Step 4: Update customer_create_order RLS policy to support both scopes
DROP POLICY IF EXISTS customer_create_order ON public.orders;
CREATE POLICY customer_create_order ON public.orders
    FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = customer_id
        AND order_type = 'CUSTOMER_PHARMACY'
        AND status = 'PENDING'
        AND warehouse_id IS NULL
        AND request_id IS NULL
        AND urgency IN ('URGENT', 'NORMAL')
        AND request_scope IN ('SPECIFIC_PHARMACY', 'ALL_PHARMACIES')
        AND (
            (request_scope = 'SPECIFIC_PHARMACY' AND pharmacy_id IS NOT NULL)
            OR
            (request_scope = 'ALL_PHARMACIES' AND pharmacy_id IS NULL)
        )
        AND total_price_cents IS NULL
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = auth.uid()
              AND p.account_type = 'PUBLIC_USER'
        )
    );

-- Step 5: Create RPC to get customer's own orders with pharmacy details
CREATE OR REPLACE FUNCTION public.get_my_customer_orders()
RETURNS TABLE (
    id text,
    medicine_id text,
    medicine_name text,
    quantity integer,
    unit text,
    status text,
    order_type text,
    fulfillment_type text,
    pharmacy_id text,
    warehouse_id text,
    customer_id uuid,
    request_id text,
    total_price_cents bigint,
    currency text,
    delivery_address text,
    delivery_phone text,
    notes text,
    created_at timestamptz,
    updated_at timestamptz,
    confirmed_at timestamptz,
    fulfilled_at timestamptz,
    warehouse_name text,
    supplier_name text,
    eta_label text,
    is_urgent boolean,
    urgency text,
    request_scope text,
    pharmacy_name text,
    pharmacy_location text
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        o.id,
        o.medicine_id::text,
        o.medicine_name,
        o.quantity,
        o.unit,
        o.status,
        o.order_type,
        o.fulfillment_type,
        o.pharmacy_id,
        o.warehouse_id,
        o.customer_id,
        o.request_id,
        o.total_price_cents,
        o.currency,
        o.delivery_address,
        o.delivery_phone,
        o.notes,
        o.created_at,
        o.updated_at,
        o.confirmed_at,
        o.fulfilled_at,
        NULL::text AS warehouse_name,
        NULL::text AS supplier_name,
        NULL::text AS eta_label,
        (o.urgency = 'URGENT') AS is_urgent,
        o.urgency,
        o.request_scope,
        ph.name AS pharmacy_name,
        ph.location AS pharmacy_location
    FROM public.orders o
    LEFT JOIN public.pharmacies ph ON ph.id = o.pharmacy_id
    WHERE o.customer_id = auth.uid()
      AND o.order_type = 'CUSTOMER_PHARMACY'
    ORDER BY o.created_at DESC;
$$;

-- Step 6: Update cancel_customer_order RPC to validate urgency/scope
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

    RETURN v_updated;
END;
$$;

-- Step 7: Add performance indexes
CREATE INDEX IF NOT EXISTS idx_orders_customer_created_at
    ON public.orders(customer_id, created_at DESC)
    WHERE order_type = 'CUSTOMER_PHARMACY';

CREATE INDEX IF NOT EXISTS idx_orders_status_request_scope
    ON public.orders(status, request_scope);

CREATE INDEX IF NOT EXISTS idx_orders_pharmacy_status
    ON public.orders(pharmacy_id, status)
    WHERE pharmacy_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_open_all_pharmacies
    ON public.orders(request_scope, status)
    WHERE order_type = 'CUSTOMER_PHARMACY'
      AND request_scope = 'ALL_PHARMACIES';

-- Step 8: Grant execute permissions
REVOKE ALL ON FUNCTION public.get_my_customer_orders() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_my_customer_orders() TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_customer_order(text) TO authenticated;

-- ============================================
-- MIGRATION COMPLETE
-- ============================================
