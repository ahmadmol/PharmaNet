-- ====================================================================
-- Seed Data: Medicines for PUBLIC_USER Testing
-- Purpose: Add common medicines for search and order testing
-- Date: 2026-05-06
-- Note: This is TEST DATA ONLY - adjust for production use
-- ====================================================================

-- Insert common medicines (only if table is empty or specific IDs don't exist)
INSERT INTO public.medicines (id, name, brand, strength, price, image_url)
VALUES
    ('MED-001', 'باراسيتامول', 'بانادول', '500mg', 5.00, NULL),
    ('MED-002', 'إيبوبروفين', 'بروفين', '400mg', 8.50, NULL),
    ('MED-003', 'أموكسيسيلين', 'أوجمنتين', '500mg', 25.00, NULL),
    ('MED-004', 'أوميبرازول', 'لوسيك', '20mg', 15.00, NULL),
    ('MED-005', 'ميتفورمين', 'جلوكوفاج', '500mg', 12.00, NULL),
    ('MED-006', 'أملوديبين', 'نورفاسك', '5mg', 18.00, NULL),
    ('MED-007', 'سيتريزين', 'زيرتك', '10mg', 10.00, NULL),
    ('MED-008', 'لوراتادين', 'كلاريتين', '10mg', 9.00, NULL),
    ('MED-009', 'أزيثروميسين', 'زيثروماكس', '250mg', 30.00, NULL),
    ('MED-010', 'ديكلوفيناك', 'فولتارين', '50mg', 7.50, NULL),
    ('MED-011', 'أسبرين', 'أسبرين', '100mg', 3.00, NULL),
    ('MED-012', 'فيتامين د', 'فيتامين د3', '1000 IU', 20.00, NULL),
    ('MED-013', 'فيتامين سي', 'فيتامين سي', '500mg', 15.00, NULL),
    ('MED-014', 'كالسيوم', 'كالسيوم', '600mg', 18.00, NULL),
    ('MED-015', 'حديد', 'فيروجلوبين', '14mg', 22.00, NULL)
ON CONFLICT (id) DO NOTHING;

-- Verify insertion
SELECT COUNT(*) as medicines_count FROM public.medicines;

-- Migration complete
