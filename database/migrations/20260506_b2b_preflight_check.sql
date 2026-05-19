-- ====================================================================
-- B2B Phase 1 Preflight Check
-- Purpose: Verify data state before applying b2b_core_stabilization
-- Date: 2026-05-06
-- ====================================================================

-- This script should be run BEFORE 20260505_b2b_core_stabilization.sql
-- It checks for data issues that need manual resolution

DO $$
DECLARE
  v_submitted_count INTEGER;
  v_missing_medicine_count INTEGER;
  v_orphan_orders_count INTEGER;
  v_error_messages TEXT := '';
BEGIN
  RAISE NOTICE '=== B2B Phase 1 Preflight Check ===';
  RAISE NOTICE '';

  -- Check 1: Count SUBMITTED requests without orders
  SELECT COUNT(*) INTO v_submitted_count
  FROM public.requests
  WHERE status = 'SUBMITTED'
    AND NOT EXISTS (
      SELECT 1 FROM public.orders
      WHERE orders.request_id = requests.id
        AND orders.order_type = 'PHARMACY_WAREHOUSE'
    );

  RAISE NOTICE 'Check 1: SUBMITTED requests without orders: %', v_submitted_count;

  IF v_submitted_count > 0 THEN
    RAISE NOTICE '';
    RAISE NOTICE 'Details of SUBMITTED requests:';
    RAISE NOTICE '%-38s | %-30s | %-20s | %-10s', 'Request ID', 'Medicine Name', 'Warehouse Name', 'Quantity';
    RAISE NOTICE '%', repeat('-', 110);
    
    FOR rec IN (
      SELECT 
        id,
        medicine_name,
        warehouse_name,
        quantity,
        unit
      FROM public.requests
      WHERE status = 'SUBMITTED'
        AND NOT EXISTS (
          SELECT 1 FROM public.orders
          WHERE orders.request_id = requests.id
            AND orders.order_type = 'PHARMACY_WAREHOUSE'
        )
      ORDER BY created_at
    ) LOOP
      RAISE NOTICE '% | %-30s | %-20s | % %', 
        rec.id, 
        rec.medicine_name, 
        rec.warehouse_name,
        rec.quantity,
        rec.unit;
    END LOOP;
    
    v_error_messages := v_error_messages || E'\n- Found ' || v_submitted_count || ' SUBMITTED requests without orders';
  END IF;

  RAISE NOTICE '';

  -- Check 2: Count requests with missing medicine_id
  SELECT COUNT(*) INTO v_missing_medicine_count
  FROM public.requests
  WHERE status = 'SUBMITTED'
    AND medicine_id IS NULL;

  RAISE NOTICE 'Check 2: SUBMITTED requests with NULL medicine_id: %', v_missing_medicine_count;

  IF v_missing_medicine_count > 0 THEN
    v_error_messages := v_error_messages || E'\n- Found ' || v_missing_medicine_count || ' requests with NULL medicine_id';
  END IF;

  RAISE NOTICE '';

  -- Check 3: Verify medicines table has required medicines
  RAISE NOTICE 'Check 3: Available medicines in database:';
  RAISE NOTICE '%-38s | %-30s | %-15s', 'Medicine ID', 'Name', 'Strength';
  RAISE NOTICE '%', repeat('-', 90);
  
  FOR rec IN (
    SELECT id, name, strength
    FROM public.medicines
    ORDER BY name, strength
  ) LOOP
    RAISE NOTICE '% | %-30s | %-15s', rec.id, rec.name, rec.strength;
  END LOOP;

  RAISE NOTICE '';

  -- Check 4: Check for orphan orders (orders without requests)
  SELECT COUNT(*) INTO v_orphan_orders_count
  FROM public.orders
  WHERE order_type = 'PHARMACY_WAREHOUSE'
    AND request_id IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM public.requests
      WHERE requests.id = orders.request_id
    );

  RAISE NOTICE 'Check 4: Orphan B2B orders (orders without requests): %', v_orphan_orders_count;

  IF v_orphan_orders_count > 0 THEN
    v_error_messages := v_error_messages || E'\n- Found ' || v_orphan_orders_count || ' orphan B2B orders';
  END IF;

  RAISE NOTICE '';
  RAISE NOTICE '=== Preflight Check Complete ===';
  RAISE NOTICE '';

  -- Final decision
  IF v_error_messages != '' THEN
    RAISE NOTICE 'RESULT: ❌ PREFLIGHT FAILED';
    RAISE NOTICE '';
    RAISE NOTICE 'Issues found:%', v_error_messages;
    RAISE NOTICE '';
    RAISE NOTICE 'ACTION REQUIRED:';
    RAISE NOTICE '1. Review the SUBMITTED requests listed above';
    RAISE NOTICE '2. Either:';
    RAISE NOTICE '   Option A: Run 20260506_b2b_manual_backfill.sql with manual medicine_id mapping';
    RAISE NOTICE '   Option B: Delete or revert these requests to DRAFT if they are test data';
    RAISE NOTICE '3. Then run 20260505_b2b_core_stabilization.sql';
    RAISE NOTICE '';
    RAISE EXCEPTION 'Preflight check failed. Manual intervention required before migration.';
  ELSE
    RAISE NOTICE 'RESULT: ✅ PREFLIGHT PASSED';
    RAISE NOTICE 'Safe to proceed with 20260505_b2b_core_stabilization.sql';
  END IF;
END;
$$;
