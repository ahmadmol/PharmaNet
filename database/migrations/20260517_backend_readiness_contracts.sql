-- ====================================================================
-- Backend readiness contracts
-- Date: 2026-05-17
-- Purpose:
--   - Add the audit-log detail RPC called by SupabasePharmaRepository.
--   - Expose warehouse_inventory as a read-only view over medicines.
--   - Provision storage buckets and RLS policies for prescription and medicine image uploads.
-- ====================================================================

-- --------------------------------------------------------------------
-- Admin audit-log detail RPC
-- --------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.admin_get_audit_log_detail(p_log_id UUID)
RETURNS TABLE (
  id UUID,
  admin_id UUID,
  admin_email TEXT,
  action TEXT,
  target_user_id UUID,
  target_user_email TEXT,
  old_value JSONB,
  new_value JSONB,
  created_at TIMESTAMPTZ,
  ip_address TEXT,
  user_agent TEXT,
  transaction_id TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type = 'ADMIN'
      AND profiles.is_active = true
  ) THEN
    RAISE EXCEPTION 'Unauthorized: Only active ADMIN can view audit log detail';
  END IF;

  RETURN QUERY
  SELECT
    al.id,
    al.admin_id,
    al.admin_email,
    al.action,
    al.target_user_id,
    al.target_user_email,
    al.old_value,
    al.new_value,
    al.created_at,
    NULL::TEXT AS ip_address,
    NULL::TEXT AS user_agent,
    replace(al.id::TEXT, '-', '') AS transaction_id
  FROM public.audit_logs al
  WHERE al.id = p_log_id;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_get_audit_log_detail(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_get_audit_log_detail(UUID) TO authenticated;

-- --------------------------------------------------------------------
-- Warehouse inventory contract
-- --------------------------------------------------------------------
CREATE OR REPLACE VIEW public.warehouse_inventory AS
SELECT
  concat(m.warehouse_id::TEXT, '_', m.id::TEXT) AS id,
  m.id AS medicine_id,
  m.name AS medicine_name,
  m.image_url,
  m.warehouse_id,
  COALESCE(m.stock_quantity, 0) AS quantity,
  'box'::TEXT AS unit,
  CASE
    WHEN COALESCE(m.stock_quantity, 0) <= 0 THEN 'OUT_OF_STOCK'
    WHEN COALESCE(m.stock_quantity, 0) < 10 THEN 'LOW_STOCK'
    ELSE 'IN_STOCK'
  END AS stock_status,
  COALESCE(m.updated_at, m.created_at, now()) AS last_updated
FROM public.medicines m
WHERE m.warehouse_id IS NOT NULL;

GRANT SELECT ON public.warehouse_inventory TO authenticated;

-- --------------------------------------------------------------------
-- Storage buckets and upload/read policies
-- --------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
  ('prescriptions', 'prescriptions', true, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf']),
  ('medicines', 'medicines', true, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE
SET
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS prescriptions_public_read ON storage.objects;
CREATE POLICY prescriptions_public_read
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'prescriptions');

DROP POLICY IF EXISTS prescriptions_user_upload ON storage.objects;
CREATE POLICY prescriptions_user_upload
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'prescriptions'
  AND owner = auth.uid()
);

DROP POLICY IF EXISTS prescriptions_user_update ON storage.objects;
CREATE POLICY prescriptions_user_update
ON storage.objects
FOR UPDATE
TO authenticated
USING (
  bucket_id = 'prescriptions'
  AND owner = auth.uid()
)
WITH CHECK (
  bucket_id = 'prescriptions'
  AND owner = auth.uid()
);

DROP POLICY IF EXISTS medicines_public_read ON storage.objects;
CREATE POLICY medicines_public_read
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'medicines');

DROP POLICY IF EXISTS medicines_admin_or_warehouse_upload ON storage.objects;
CREATE POLICY medicines_admin_or_warehouse_upload
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'medicines'
  AND EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type IN ('ADMIN', 'WAREHOUSE')
      AND profiles.is_active = true
  )
);

DROP POLICY IF EXISTS medicines_admin_or_warehouse_update ON storage.objects;
CREATE POLICY medicines_admin_or_warehouse_update
ON storage.objects
FOR UPDATE
TO authenticated
USING (
  bucket_id = 'medicines'
  AND EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type IN ('ADMIN', 'WAREHOUSE')
      AND profiles.is_active = true
  )
)
WITH CHECK (
  bucket_id = 'medicines'
  AND EXISTS (
    SELECT 1
    FROM public.profiles
    WHERE profiles.id = auth.uid()
      AND profiles.account_type IN ('ADMIN', 'WAREHOUSE')
      AND profiles.is_active = true
  )
);
