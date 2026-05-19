-- ====================================================================
-- Add Missing Medicines for B2B Backfill
-- Purpose: Add medicines that are referenced in SUBMITTED requests but missing from medicines table
-- Date: 2026-05-06
-- ====================================================================

-- IMPORTANT: Run this ONLY if preflight check shows missing medicines
-- This script adds medicines that don't exist in the database

DO $$
DECLARE
  v_amoxicillin_250_id UUID;
  v_vitamin_c_1000_id UUID;
  v_paracetamol_500_id UUID;
  v_ibuprofen_400_id UUID;
BEGIN
  RAISE NOTICE '=== Adding Missing Medicines ===';
  RAISE NOTICE '';

  -- Check and add باراسيتامول 500mg
  SELECT id INTO v_paracetamol_500_id
  FROM public.medicines
  WHERE name ILIKE '%باراسيتامول%'
    AND strength ILIKE '%500%'
  LIMIT 1;

  IF v_paracetamol_500_id IS NULL THEN
    INSERT INTO public.medicines (name, brand, strength, price, currency, image_url, created_at, updated_at)
    VALUES ('باراسيتامول', 'بانادول', '500mg', 5.00, 'SAR', NULL, now(), now())
    RETURNING id INTO v_paracetamol_500_id;
    RAISE NOTICE '✓ Added باراسيتامول 500mg: %', v_paracetamol_500_id;
  ELSE
    RAISE NOTICE '✓ باراسيتامول 500mg already exists: %', v_paracetamol_500_id;
  END IF;

  -- Check and add أموكسيسيلين 250mg (NOT 500mg!)
  SELECT id INTO v_amoxicillin_250_id
  FROM public.medicines
  WHERE name ILIKE '%أموكسيسيلين%'
    AND strength ILIKE '%250%'
  LIMIT 1;

  IF v_amoxicillin_250_id IS NULL THEN
    INSERT INTO public.medicines (name, brand, strength, price, currency, image_url, created_at, updated_at)
    VALUES ('أموكسيسيلين', 'أموكسيل', '250mg', 20.00, 'SAR', NULL, now(), now())
    RETURNING id INTO v_amoxicillin_250_id;
    RAISE NOTICE '✓ Added أموكسيسيلين 250mg: %', v_amoxicillin_250_id;
  ELSE
    RAISE NOTICE '✓ أموكسيسيلين 250mg already exists: %', v_amoxicillin_250_id;
  END IF;

  -- Check and add فيتامين سي 1000mg
  SELECT id INTO v_vitamin_c_1000_id
  FROM public.medicines
  WHERE name ILIKE '%فيتامين%سي%'
    AND strength ILIKE '%1000%'
  LIMIT 1;

  IF v_vitamin_c_1000_id IS NULL THEN
    INSERT INTO public.medicines (name, brand, strength, price, currency, image_url, created_at, updated_at)
    VALUES ('فيتامين سي', 'فيتامين سي', '1000mg', 25.00, 'SAR', NULL, now(), now())
    RETURNING id INTO v_vitamin_c_1000_id;
    RAISE NOTICE '✓ Added فيتامين سي 1000mg: %', v_vitamin_c_1000_id;
  ELSE
    RAISE NOTICE '✓ فيتامين سي 1000mg already exists: %', v_vitamin_c_1000_id;
  END IF;

  -- Check and add ايبوبروفين 400mg
  SELECT id INTO v_ibuprofen_400_id
  FROM public.medicines
  WHERE name ILIKE '%ايبوبروفين%'
    AND strength ILIKE '%400%'
  LIMIT 1;

  IF v_ibuprofen_400_id IS NULL THEN
    INSERT INTO public.medicines (name, brand, strength, price, currency, image_url, created_at, updated_at)
    VALUES ('ايبوبروفين', 'بروفين', '400mg', 8.50, 'SAR', NULL, now(), now())
    RETURNING id INTO v_ibuprofen_400_id;
    RAISE NOTICE '✓ Added ايبوبروفين 400mg: %', v_ibuprofen_400_id;
  ELSE
    RAISE NOTICE '✓ ايبوبروفين 400mg already exists: %', v_ibuprofen_400_id;
  END IF;

  RAISE NOTICE '';
  RAISE NOTICE '=== Medicine Addition Complete ===';
  RAISE NOTICE '';
  RAISE NOTICE 'Medicine IDs for manual mapping:';
  RAISE NOTICE 'باراسيتامول 500mg: %', v_paracetamol_500_id;
  RAISE NOTICE 'أموكسيسيلين 250mg: %', v_amoxicillin_250_id;
  RAISE NOTICE 'فيتامين سي 1000mg: %', v_vitamin_c_1000_id;
  RAISE NOTICE 'ايبوبروفين 400mg: %', v_ibuprofen_400_id;
  RAISE NOTICE '';
  RAISE NOTICE 'Next steps:';
  RAISE NOTICE '1. Copy these medicine_id values';
  RAISE NOTICE '2. Run preflight check to get request_id values';
  RAISE NOTICE '3. Fill in the mapping table in 20260506_b2b_manual_backfill.sql';
  RAISE NOTICE '4. Run the backfill script';
  RAISE NOTICE '5. Run 20260505_b2b_core_stabilization.sql';

END;
$$;

-- Verification: Show all medicines with their IDs
SELECT 
  id,
  name,
  brand,
  strength,
  price,
  currency
FROM public.medicines
WHERE 
  (name ILIKE '%باراسيتامول%' AND strength ILIKE '%500%')
  OR (name ILIKE '%أموكسيسيلين%' AND strength ILIKE '%250%')
  OR (name ILIKE '%فيتامين%سي%' AND strength ILIKE '%1000%')
  OR (name ILIKE '%ايبوبروفين%' AND strength ILIKE '%400%')
ORDER BY name, strength;
