-- ====================================================================
-- B2B Phase 1 Manual Backfill
-- Purpose: Manually map SUBMITTED requests to correct medicine_id and create orders
-- Date: 2026-05-06
-- ====================================================================

-- IMPORTANT: This script should be run AFTER verifying medicine_id mappings
-- and BEFORE 20260505_b2b_core_stabilization.sql

-- Step 1: Verify required medicines exist
-- If medicines are missing, add them first

DO $$
DECLARE
  v_paracetamol_500_exists BOOLEAN;
  v_amoxicillin_250_exists BOOLEAN;
  v_vitamin_c_1000_exists BOOLEAN;
  v_ibuprofen_400_exists BOOLEAN;
BEGIN
  RAISE NOTICE '=== Checking Required Medicines ===';
  
  -- Check باراسيتامول 500mg
  SELECT EXISTS (
    SELECT 1 FROM public.medicines
    WHERE name ILIKE '%باراسيتامول%'
      AND strength ILIKE '%500%'
  ) INTO v_paracetamol_500_exists;
  
  RAISE NOTICE 'باراسيتامول 500mg exists: %', v_paracetamol_500_exists;
  
  -- Check أموكسيسيلين 250mg (NOT 500mg!)
  SELECT EXISTS (
    SELECT 1 FROM public.medicines
    WHERE name ILIKE '%أموكسيسيلين%'
      AND strength ILIKE '%250%'
  ) INTO v_amoxicillin_250_exists;
  
  RAISE NOTICE 'أموكسيسيلين 250mg exists: %', v_amoxicillin_250_exists;
  
  -- Check فيتامين سي 1000mg
  SELECT EXISTS (
    SELECT 1 FROM public.medicines
    WHERE name ILIKE '%فيتامين%سي%'
      AND strength ILIKE '%1000%'
  ) INTO v_vitamin_c_1000_exists;
  
  RAISE NOTICE 'فيتامين سي 1000mg exists: %', v_vitamin_c_1000_exists;
  
  -- Check ايبوبروفين 400mg
  SELECT EXISTS (
    SELECT 1 FROM public.medicines
    WHERE name ILIKE '%ايبوبروفين%'
      AND strength ILIKE '%400%'
  ) INTO v_ibuprofen_400_exists;
  
  RAISE NOTICE 'ايبوبروفين 400mg exists: %', v_ibuprofen_400_exists;
  
  IF NOT (v_paracetamol_500_exists AND v_amoxicillin_250_exists AND v_vitamin_c_1000_exists AND v_ibuprofen_400_exists) THEN
    RAISE EXCEPTION 'Missing required medicines. Please add them first using the INSERT statements below.';
  END IF;
  
  RAISE NOTICE 'All required medicines exist ✓';
END;
$$;

-- Step 2: Add missing medicines if needed
-- UNCOMMENT AND ADJUST IDs AFTER CHECKING WHAT'S MISSING

/*
-- Add أموكسيسيلين 250mg (if missing)
INSERT INTO public.medicines (name, brand, strength, price, image_url)
VALUES ('أموكسيسيلين', 'أموكسيل', '250mg', 20.00, NULL)
ON CONFLICT (id) DO NOTHING;

-- Add فيتامين سي 1000mg (if missing)
INSERT INTO public.medicines (name, brand, strength, price, image_url)
VALUES ('فيتامين سي', 'فيتامين سي', '1000mg', 25.00, NULL)
ON CONFLICT (id) DO NOTHING;

-- Note: باراسيتامول 500mg and ايبوبروفين 400mg should already exist
*/

-- Step 3: Manual Medicine ID Mapping
-- CRITICAL: Fill in the correct medicine_id for each request_id
-- DO NOT use fuzzy matching - verify each mapping manually

DO $$
DECLARE
  v_mapping RECORD;
  v_request RECORD;
  v_order_id UUID;
  v_updated_count INTEGER := 0;
  v_created_count INTEGER := 0;
BEGIN
  RAISE NOTICE '';
  RAISE NOTICE '=== Starting Manual Backfill ===';
  RAISE NOTICE '';

  -- Manual mapping table
  -- FORMAT: (request_id, medicine_id, medicine_name_for_verification)
  -- INSTRUCTIONS:
  -- 1. Run preflight check to get list of request_ids
  -- 2. For each request, find the correct medicine_id from medicines table
  -- 3. Fill in the VALUES below with exact UUIDs
  -- 4. Verify medicine_name matches what's in the request
  
  FOR v_mapping IN (
    SELECT * FROM (VALUES
      -- EXAMPLE (REPLACE WITH ACTUAL DATA):
      -- ('request-uuid-1'::uuid, 'medicine-uuid-1'::uuid, 'باراسيتامول 500 مجم'),
      -- ('request-uuid-2'::uuid, 'medicine-uuid-2'::uuid, 'أموكسيسيلين 250 مجم'),
      -- ('request-uuid-3'::uuid, 'medicine-uuid-3'::uuid, 'فيتامين سي 1000 مجم'),
      -- ('request-uuid-4'::uuid, 'medicine-uuid-4'::uuid, 'ايبوبروفين 400 مجم')
      
      -- ⚠️ FILL IN ACTUAL MAPPINGS HERE ⚠️
      -- Leave empty if no SUBMITTED requests exist
      (NULL::uuid, NULL::uuid, NULL::text)
      
    ) AS mapping(request_id, medicine_id, medicine_name_verify)
    WHERE request_id IS NOT NULL
  ) LOOP
    
    -- Verify request exists and is SUBMITTED
    SELECT * INTO v_request
    FROM public.requests
    WHERE id = v_mapping.request_id
      AND status = 'SUBMITTED';
    
    IF v_request IS NULL THEN
      RAISE WARNING 'Request % not found or not SUBMITTED, skipping', v_mapping.request_id;
      CONTINUE;
    END IF;
    
    -- Verify medicine exists
    IF NOT EXISTS (SELECT 1 FROM public.medicines WHERE id = v_mapping.medicine_id) THEN
      RAISE WARNING 'Medicine % not found, skipping request %', v_mapping.medicine_id, v_mapping.request_id;
      CONTINUE;
    END IF;
    
    -- Verify no order exists yet
    IF EXISTS (
      SELECT 1 FROM public.orders
      WHERE request_id = v_mapping.request_id
        AND order_type = 'PHARMACY_WAREHOUSE'
    ) THEN
      RAISE WARNING 'Order already exists for request %, skipping', v_mapping.request_id;
      CONTINUE;
    END IF;
    
    RAISE NOTICE 'Processing request %: % → medicine %', 
      v_mapping.request_id, 
      v_mapping.medicine_name_verify,
      v_mapping.medicine_id;
    
    -- Update request with medicine_id
    UPDATE public.requests
    SET medicine_id = v_mapping.medicine_id,
        updated_at = now()
    WHERE id = v_mapping.request_id;
    
    v_updated_count := v_updated_count + 1;
    
    -- Create order for this request
    INSERT INTO public.orders (
      order_type,
      request_id,
      pharmacy_id,
      warehouse_id,
      medicine_id,
      medicine_name,
      quantity,
      unit,
      status,
      fulfillment_type,
      customer_id,
      total_price_cents,
      currency,
      notes,
      created_at,
      updated_at,
      warehouse_name,
      supplier_name,
      eta_label,
      is_urgent
    )
    SELECT
      'PHARMACY_WAREHOUSE',
      v_request.id,
      v_request.pharmacy_id,
      v_request.warehouse_id,
      v_mapping.medicine_id,
      v_request.medicine_name,
      v_request.quantity,
      v_request.unit,
      'PENDING',
      'DELIVERY',
      NULL,
      NULL,
      'SAR',
      v_request.notes,
      now(),
      now(),
      v_request.warehouse_name,
      v_request.supplier_name,
      v_request.eta_label,
      v_request.priority = 'URGENT'
    RETURNING id INTO v_order_id;
    
    v_created_count := v_created_count + 1;
    
    -- Update request with related_order_id and change status to PENDING
    UPDATE public.requests
    SET related_order_id = v_order_id,
        status = 'PENDING',
        updated_at = now()
    WHERE id = v_mapping.request_id;
    
    RAISE NOTICE '  ✓ Created order % and updated request to PENDING', v_order_id;
    
  END LOOP;
  
  RAISE NOTICE '';
  RAISE NOTICE '=== Backfill Complete ===';
  RAISE NOTICE 'Requests updated: %', v_updated_count;
  RAISE NOTICE 'Orders created: %', v_created_count;
  RAISE NOTICE '';
  
  IF v_updated_count = 0 THEN
    RAISE NOTICE 'No requests were backfilled. This is OK if:';
    RAISE NOTICE '- No SUBMITTED requests exist';
    RAISE NOTICE '- All SUBMITTED requests already have orders';
    RAISE NOTICE '- Mapping table was left empty intentionally';
  ELSE
    RAISE NOTICE 'Next step: Run 20260505_b2b_core_stabilization.sql';
  END IF;
  
END;
$$;

-- Step 4: Verification
DO $$
DECLARE
  v_remaining_submitted INTEGER;
  v_requests_without_medicine INTEGER;
  v_requests_without_orders INTEGER;
BEGIN
  RAISE NOTICE '';
  RAISE NOTICE '=== Post-Backfill Verification ===';
  
  -- Check remaining SUBMITTED requests
  SELECT COUNT(*) INTO v_remaining_submitted
  FROM public.requests
  WHERE status = 'SUBMITTED';
  
  RAISE NOTICE 'Remaining SUBMITTED requests: %', v_remaining_submitted;
  
  -- Check requests without medicine_id
  SELECT COUNT(*) INTO v_requests_without_medicine
  FROM public.requests
  WHERE status IN ('SUBMITTED', 'PENDING')
    AND medicine_id IS NULL;
  
  RAISE NOTICE 'SUBMITTED/PENDING requests without medicine_id: %', v_requests_without_medicine;
  
  -- Check PENDING requests without orders
  SELECT COUNT(*) INTO v_requests_without_orders
  FROM public.requests
  WHERE status = 'PENDING'
    AND NOT EXISTS (
      SELECT 1 FROM public.orders
      WHERE orders.request_id = requests.id
        AND orders.order_type = 'PHARMACY_WAREHOUSE'
    );
  
  RAISE NOTICE 'PENDING requests without orders: %', v_requests_without_orders;
  
  RAISE NOTICE '';
  
  IF v_remaining_submitted > 0 OR v_requests_without_medicine > 0 OR v_requests_without_orders > 0 THEN
    RAISE WARNING 'Some requests still need attention. Review the counts above.';
  ELSE
    RAISE NOTICE '✅ All checks passed. Safe to proceed with core stabilization migration.';
  END IF;
  
END;
$$;
