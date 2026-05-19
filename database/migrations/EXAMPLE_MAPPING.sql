-- ====================================================================
-- EXAMPLE: Manual Mapping for B2B Backfill
-- This is an EXAMPLE ONLY - DO NOT RUN THIS FILE
-- ====================================================================

-- This file shows what the mapping table should look like after filling it in
-- Copy this format into 20260506_b2b_manual_backfill.sql

-- EXAMPLE with fake UUIDs (replace with real ones from preflight check):

FOR v_mapping IN (
  SELECT * FROM (VALUES
    -- Request 1: باراسيتامول 500 مجم
    ('a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 
     '2877d1e3-896e-4b0d-8c5d-9fcceb271970'::uuid, 
     'باراسيتامول 500 مجم'),
    
    -- Request 2: أموكسيسيلين 250 مجم (NOT 500mg!)
    ('b2c3d4e5-f6a7-4b5c-9d0e-1f2a3b4c5d6e'::uuid, 
     'f1e2d3c4-b5a6-4978-8c9d-0e1f2a3b4c5d'::uuid, 
     'أموكسيسيلين 250 مجم'),
    
    -- Request 3: فيتامين سي 1000 مجم
    ('c3d4e5f6-a7b8-4c5d-0e1f-2a3b4c5d6e7f'::uuid, 
     'e2d3c4b5-a697-4089-9c8d-1e2f3a4b5c6d'::uuid, 
     'فيتامين سي 1000 مجم'),
    
    -- Request 4: ايبوبروفين 400 مجم
    ('d4e5f6a7-b8c9-4d5e-1f2a-3b4c5d6e7f8a'::uuid, 
     'd3c4b5a6-9788-4190-8c9d-2e3f4a5b6c7d'::uuid, 
     'ايبوبروفين 400 مجم'),
    
    -- Request 5: باراسيتامول 500 مجم (another one)
    ('e5f6a7b8-c9d0-4e5f-2a3b-4c5d6e7f8a9b'::uuid, 
     '2877d1e3-896e-4b0d-8c5d-9fcceb271970'::uuid, 
     'باراسيتامول 500 مجم'),
    
    -- Request 6: فيتامين سي 1000 مجم (another one)
    ('f6a7b8c9-d0e1-4f5a-3b4c-5d6e7f8a9b0c'::uuid, 
     'e2d3c4b5-a697-4089-9c8d-1e2f3a4b5c6d'::uuid, 
     'فيتامين سي 1000 مجم'),
    
    -- Request 7: ايبوبروفين 400 مجم (another one)
    ('a7b8c9d0-e1f2-4a5b-4c5d-6e7f8a9b0c1d'::uuid, 
     'd3c4b5a6-9788-4190-8c9d-2e3f4a5b6c7d'::uuid, 
     'ايبوبروفين 400 مجم')
    
  ) AS mapping(request_id, medicine_id, medicine_name_verify)
  WHERE request_id IS NOT NULL
) LOOP
  -- ... rest of the backfill logic
END LOOP;

-- ====================================================================
-- HOW TO FILL THIS IN:
-- ====================================================================

-- Step 1: Run preflight check
--   \i database/migrations/20260506_b2b_preflight_check.sql
--   
--   Copy the request_id from the output:
--   Request ID                            | Medicine Name
--   <uuid-1>                              | باراسيتامول 500 مجم
--   <uuid-2>                              | أموكسيسيلين 250 مجم
--   ...

-- Step 2: Run add_missing_medicines
--   \i database/migrations/20260506_add_missing_medicines.sql
--   
--   Copy the medicine_id from the output:
--   Medicine IDs for manual mapping:
--   باراسيتامول 500mg: <uuid-a>
--   أموكسيسيلين 250mg: <uuid-b>
--   ...

-- Step 3: Match request_id to medicine_id
--   For each request:
--   1. Find the request_id from Step 1
--   2. Find the medicine_id from Step 2 that matches the medicine_name
--   3. Add a line to the VALUES clause:
--      ('<request-id>'::uuid, '<medicine-id>'::uuid, '<medicine-name>')

-- Step 4: Verify each line
--   - Is the request_id correct?
--   - Is the medicine_id correct?
--   - Does the medicine_name match?
--   - Is the strength correct? (250 ≠ 500)

-- Step 5: Copy to 20260506_b2b_manual_backfill.sql
--   Replace the empty mapping table with your filled version

-- ====================================================================
-- COMMON MISTAKES TO AVOID:
-- ====================================================================

-- ❌ WRONG: Using 500mg medicine_id for 250mg request
-- ('request-for-250mg'::uuid, 'medicine-id-for-500mg'::uuid, 'أموكسيسيلين 250 مجم')

-- ✅ CORRECT: Using 250mg medicine_id for 250mg request
-- ('request-for-250mg'::uuid, 'medicine-id-for-250mg'::uuid, 'أموكسيسيلين 250 مجم')

-- ❌ WRONG: Typo in medicine_name_verify
-- ('request-id'::uuid, 'medicine-id'::uuid, 'باراسيتامل 500 مجم')  -- missing ي

-- ✅ CORRECT: Exact medicine_name from request
-- ('request-id'::uuid, 'medicine-id'::uuid, 'باراسيتامول 500 مجم')

-- ❌ WRONG: Swapped request_id and medicine_id
-- ('medicine-id'::uuid, 'request-id'::uuid, 'medicine-name')

-- ✅ CORRECT: request_id first, medicine_id second
-- ('request-id'::uuid, 'medicine-id'::uuid, 'medicine-name')
