-- ============================================
-- Migration: Extend orders table for B2C support
-- Date: 2025-04-25
-- Phase: 4.3A
-- ============================================

-- ============================================
-- Step 1: Add New Columns (Backward Compatible)
-- ============================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS medicine_id UUID REFERENCES medicines(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type TEXT DEFAULT 'PHARMACY_WAREHOUSE';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_id UUID REFERENCES auth.users(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fulfillment_type TEXT DEFAULT 'DELIVERY';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_price_cents BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS currency TEXT DEFAULT 'SAR';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_phone TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fulfilled_at TIMESTAMPTZ;

-- ============================================
-- Step 2: Relax Constraints (make nullable)
-- ============================================

ALTER TABLE orders ALTER COLUMN warehouse_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN request_id DROP NOT NULL;

-- ============================================
-- Step 3: Backfill Existing Orders (B2B only)
-- ============================================

-- Only migrate orders that have valid request linkage
UPDATE orders o
SET 
    order_type = 'PHARMACY_WAREHOUSE',
    medicine_id = (
        SELECT m.id FROM medicines m 
        WHERE m.name = o.medicine_name 
        LIMIT 1
    ),
    pharmacy_id = (
        SELECT r.pharmacy_id FROM requests r 
        WHERE r.id = o.request_id
    ),
    fulfillment_type = 'DELIVERY'
WHERE o.request_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM requests r WHERE r.id = o.request_id);

-- ============================================
-- Step 4: Add Indexes for Performance
-- ============================================

-- Customer queries (B2C)
CREATE INDEX IF NOT EXISTS idx_orders_customer_type 
    ON orders(customer_id, order_type) 
    WHERE order_type = 'CUSTOMER_PHARMACY';

CREATE INDEX IF NOT EXISTS idx_orders_customer_status 
    ON orders(customer_id, status) 
    WHERE order_type = 'CUSTOMER_PHARMACY';

-- Pharmacy queries (both B2B and B2C)
CREATE INDEX IF NOT EXISTS idx_orders_pharmacy_type 
    ON orders(pharmacy_id, order_type);

CREATE INDEX IF NOT EXISTS idx_orders_pharmacy_status 
    ON orders(pharmacy_id, status);

-- Warehouse queries (B2B only)
CREATE INDEX IF NOT EXISTS idx_orders_warehouse 
    ON orders(warehouse_id) 
    WHERE order_type = 'PHARMACY_WAREHOUSE';

-- Medicine analytics
CREATE INDEX IF NOT EXISTS idx_orders_medicine 
    ON orders(medicine_id);

-- ============================================
-- Step 5: Add CHECK Constraints
-- ============================================

-- B2B invariant
ALTER TABLE orders ADD CONSTRAINT check_b2b_order
    CHECK (
        order_type != 'PHARMACY_WAREHOUSE' OR 
        (
            pharmacy_id IS NOT NULL 
            AND warehouse_id IS NOT NULL 
            AND request_id IS NOT NULL 
            AND customer_id IS NULL
        )
    );

-- B2C invariant
ALTER TABLE orders ADD CONSTRAINT check_b2c_order
    CHECK (
        order_type != 'CUSTOMER_PHARMACY' OR 
        (
            customer_id IS NOT NULL 
            AND pharmacy_id IS NOT NULL 
            AND warehouse_id IS NULL 
            AND request_id IS NULL
        )
    );

-- Medicine reference required
ALTER TABLE orders ADD CONSTRAINT check_medicine_required
    CHECK (medicine_id IS NOT NULL);

-- ============================================
-- Step 6: Enable RLS
-- ============================================

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- ============================================
-- Step 7: RLS Policies (Corrected - using pharmacy_id directly)
-- ============================================

-- PUBLIC_USER Policies (B2C only)
CREATE POLICY "customer_view_own_orders" ON orders
    FOR SELECT TO authenticated
    USING (
        auth.uid() = customer_id 
        AND order_type = 'CUSTOMER_PHARMACY'
    );

CREATE POLICY "customer_create_order" ON orders
    FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = customer_id
        AND order_type = 'CUSTOMER_PHARMACY'
        AND pharmacy_id IS NOT NULL
        AND warehouse_id IS NULL
        AND request_id IS NULL
        AND status = 'PENDING'
    );

CREATE POLICY "customer_cancel_pending" ON orders
    FOR UPDATE TO authenticated
    USING (
        auth.uid() = customer_id
        AND order_type = 'CUSTOMER_PHARMACY'
        AND status = 'PENDING'
    )
    WITH CHECK (
        auth.uid() = customer_id
        AND order_type = 'CUSTOMER_PHARMACY'
    );

-- PHARMACY Policies (B2B + B2C) - Using pharmacy_id directly for B2B
CREATE POLICY "pharmacy_view_orders" ON orders
    FOR SELECT TO authenticated
    USING (
        -- B2C: Orders placed at this pharmacy
        (
            order_type = 'CUSTOMER_PHARMACY' 
            AND pharmacy_id = (SELECT pharmacy_id FROM profiles WHERE id = auth.uid())
        )
        OR
        -- B2B: Orders where pharmacy_id matches directly
        (
            order_type = 'PHARMACY_WAREHOUSE' 
            AND pharmacy_id = (SELECT pharmacy_id FROM profiles WHERE id = auth.uid())
        )
    );

CREATE POLICY "pharmacy_manage_b2c" ON orders
    FOR UPDATE TO authenticated
    USING (
        order_type = 'CUSTOMER_PHARMACY'
        AND pharmacy_id = (SELECT pharmacy_id FROM profiles WHERE id = auth.uid())
    );

-- WAREHOUSE Policies (B2B only - natural filter via USING clause)
CREATE POLICY "warehouse_view_b2b" ON orders
    FOR SELECT TO authenticated
    USING (
        order_type = 'PHARMACY_WAREHOUSE'
        AND warehouse_id = (SELECT warehouse_id FROM profiles WHERE id = auth.uid())
    );

CREATE POLICY "warehouse_create_b2b" ON orders
    FOR INSERT TO authenticated
    WITH CHECK (
        order_type = 'PHARMACY_WAREHOUSE'
        AND warehouse_id = (SELECT warehouse_id FROM profiles WHERE id = auth.uid())
        AND customer_id IS NULL
    );

CREATE POLICY "warehouse_update_b2b" ON orders
    FOR UPDATE TO authenticated
    USING (
        order_type = 'PHARMACY_WAREHOUSE'
        AND warehouse_id = (SELECT warehouse_id FROM profiles WHERE id = auth.uid())
    );

-- ============================================
-- Step 8: Updated Timestamp Trigger
-- ============================================

CREATE OR REPLACE FUNCTION update_order_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_orders_updated
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_order_timestamp();
