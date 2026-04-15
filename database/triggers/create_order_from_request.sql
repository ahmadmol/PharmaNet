-- =====================================================
-- Supabase Database Trigger: Auto Order Creation
-- =====================================================
-- Trigger: create_order_from_request
-- Table: requests
-- Timing: AFTER INSERT
-- Purpose: Automatically create an order when a request is created

-- First, enable the required extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the trigger function
CREATE OR REPLACE FUNCTION create_order_from_request()
RETURNS TRIGGER AS $$
DECLARE
    new_order_id TEXT;
    order_created BOOLEAN := FALSE;
BEGIN
    -- Log the trigger execution
    RAISE LOG '=== AUTO ORDER CREATION TRIGGER FIRED ===';
    RAISE LOG 'New request ID: %', NEW.id;
    RAISE LOG 'Request medicine: %', NEW.medicine_name;
    RAISE LOG 'Request quantity: % %', NEW.quantity, NEW.unit;
    RAISE LOG 'Request warehouse: %', NEW.warehouse_name;
    RAISE LOG 'Request pharmacy_id: %', NEW.pharmacy_id;
    
    -- Check if an order already exists for this request to prevent duplicates
    IF EXISTS (
        SELECT 1 FROM orders 
        WHERE request_id = NEW.id
    ) THEN
        RAISE LOG 'Order already exists for request %, skipping creation', NEW.id;
        RETURN NEW;
    END IF;
    
    -- Generate a unique order ID
    new_order_id := 'order_' || substr(md5(NEW.id || clock_timestamp()::text), 1, 8);
    
    -- Insert the new order
    BEGIN
        INSERT INTO orders (
            id,
            request_id,
            pharmacy_id,
            warehouse_id,
            medicine_name,
            quantity,
            unit,
            status,
            warehouse_name,
            supplier_name,
            created_at,
            eta_label,
            last_update_label,
            is_urgent
        ) VALUES (
            new_order_id,
            NEW.id,
            NEW.pharmacy_id,
            NEW.warehouse_id,
            NEW.medicine_name,
            NEW.quantity,
            NEW.unit,
            'PENDING',
            NEW.warehouse_name,
            NEW.supplier_name,
            NEW.created_at,
            'Estimated: 2-3 business days',
            'Just created',
            CASE 
                WHEN NEW.priority = 'URGENT' THEN true 
                ELSE false 
            END
        );
        
        order_created := TRUE;
        
        RAISE LOG '=== AUTO ORDER CREATION SUCCESS ===';
        RAISE LOG 'Created order ID: %', new_order_id;
        RAISE LOG 'Order request_id: %', NEW.id;
        RAISE LOG 'Order status: PENDING';
        RAISE LOG 'Order warehouse: %', NEW.warehouse_name;
        
    EXCEPTION
        WHEN OTHERS THEN
            RAISE LOG '=== AUTO ORDER CREATION FAILED ===';
            RAISE LOG 'Error: %', SQLERRM;
            RAISE LOG 'SQLSTATE: %', SQLSTATE;
            
            -- Don't fail the request creation, just log the error
            -- This ensures request creation always succeeds even if order creation fails
            order_created := FALSE;
    END;
    
    -- Update the request to reference the created order
    IF order_created THEN
        BEGIN
            UPDATE requests 
            SET related_order_id = new_order_id,
                updated_at = clock_timestamp()
            WHERE id = NEW.id;
            
            RAISE LOG 'Updated request % with related_order_id: %', NEW.id, new_order_id;
            
        EXCEPTION
            WHEN OTHERS THEN
                RAISE LOG 'Failed to update request with related_order_id: %', SQLERRM;
                -- Continue anyway, the order was created successfully
        END;
    END IF;
    
    RAISE LOG '=== TRIGGER COMPLETED ===';
    RAISE LOG 'Order created: %', order_created;
    
    -- Always return the new request to ensure request creation succeeds
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create the trigger
DROP TRIGGER IF EXISTS create_order_from_request ON requests;
CREATE TRIGGER create_order_from_request
AFTER INSERT ON requests
FOR EACH ROW
EXECUTE FUNCTION create_order_from_request();

-- =====================================================
-- Additional Helper Function: Manual Order Creation
-- =====================================================
-- This function can be used to manually create orders for existing requests
CREATE OR REPLACE FUNCTION create_order_for_existing_request(request_id_param TEXT)
RETURNS TEXT AS $$
DECLARE
    request_record RECORD;
    new_order_id TEXT;
BEGIN
    -- Get the request details
    SELECT * INTO request_record 
    FROM requests 
    WHERE id = request_id_param;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Request with ID % not found', request_id_param;
    END IF;
    
    -- Check if order already exists
    IF EXISTS (
        SELECT 1 FROM orders 
        WHERE request_id = request_id_param
    ) THEN
        RAISE EXCEPTION 'Order already exists for request %', request_id_param;
    END IF;
    
    -- Generate order ID
    new_order_id := 'order_' || substr(md5(request_id_param || clock_timestamp()::text), 1, 8);
    
    -- Create the order
    INSERT INTO orders (
        id,
        request_id,
        pharmacy_id,
        warehouse_id,
        medicine_name,
        quantity,
        unit,
        status,
        warehouse_name,
        supplier_name,
        created_at,
        eta_label,
        last_update_label,
        is_urgent
    ) VALUES (
        new_order_id,
        request_record.id,
        request_record.pharmacy_id,
        request_record.warehouse_id,
        request_record.medicine_name,
        request_record.quantity,
        request_record.unit,
        'PENDING',
        request_record.warehouse_name,
        request_record.supplier_name,
        request_record.created_at,
        'Estimated: 2-3 business days',
        'Created manually',
        CASE 
            WHEN request_record.priority = 'URGENT' THEN true 
            ELSE false 
        END
    );
    
    -- Update the request
    UPDATE requests 
    SET related_order_id = new_order_id,
        updated_at = clock_timestamp()
    WHERE id = request_id_param;
    
    RETURN new_order_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Testing Queries
-- =====================================================
-- Test the trigger with a sample request
/*
-- Sample test request (run this to test the trigger):
INSERT INTO requests (
    id,
    pharmacy_id,
    warehouse_id,
    medicine_name,
    quantity,
    unit,
    status,
    priority,
    warehouse_name,
    supplier_name,
    created_at
) VALUES (
    'test_request_' || substr(md5(clock_timestamp()::text), 1, 8),
    'pharmacy_test_123',
    'warehouse_test_456',
    'Test Medicine',
    10,
    'units',
    'SUBMITTED',
    'NORMAL',
    'Test Warehouse',
    'Test Supplier',
    clock_timestamp()
);

-- Check if the order was created:
SELECT * FROM orders WHERE request_id LIKE 'test_request_%';

-- Check if the request was updated:
SELECT id, related_order_id FROM requests WHERE id LIKE 'test_request_%';
*/
