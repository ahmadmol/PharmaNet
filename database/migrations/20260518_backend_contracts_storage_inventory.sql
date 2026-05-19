-- ====================================================================
-- Backend contract hardening
-- Date: 2026-05-18
-- Purpose:
--   - Confirm admin_get_audit_log_detail RPC.
--   - Confirm warehouse_inventory contract relation.
--   - Confirm Supabase Storage buckets and policies for prescriptions.
--   - Confirm Supabase Storage buckets and policies for medicine images.
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
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type = 'ADMIN'
      AND p.is_active = true
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
ALTER TABLE public.medicines
ADD COLUMN IF NOT EXISTS stock_quantity integer NOT NULL DEFAULT 0;

ALTER TABLE public.medicines
ADD CONSTRAINT medicines_stock_quantity_non_negative
CHECK (stock_quantity >= 0);

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
WHERE m.warehouse_id IS NOT NULL
  AND (
    EXISTS (
      SELECT 1
      FROM public.profiles p
      WHERE p.id = auth.uid()
        AND p.account_type = 'ADMIN'
        AND p.is_active = true
    )
    OR EXISTS (
      SELECT 1
      FROM public.profiles p
      WHERE p.id = auth.uid()
        AND p.account_type = 'WAREHOUSE'
        AND p.is_active = true
        AND p.warehouse_id = m.warehouse_id
    )
  );

GRANT SELECT ON public.warehouse_inventory TO authenticated;

-- --------------------------------------------------------------------
-- Storage buckets
-- --------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
  (
    'prescriptions',
    'prescriptions',
    true,
    10485760,
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
  ),
  (
    'medicines',
    'medicines',
    true,
    10485760,
    ARRAY['image/jpeg', 'image/png', 'image/webp']
  )
ON CONFLICT (id) DO UPDATE
SET
  name = EXCLUDED.name,
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

-- --------------------------------------------------------------------
-- Prescription storage policies
-- --------------------------------------------------------------------
DROP POLICY IF EXISTS prescriptions_public_read ON storage.objects;
CREATE POLICY prescriptions_public_read
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'prescriptions');

DROP POLICY IF EXISTS prescriptions_authenticated_upload ON storage.objects;
DROP POLICY IF EXISTS prescriptions_user_upload ON storage.objects;
CREATE POLICY prescriptions_authenticated_upload
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'prescriptions'
  AND name LIKE auth.uid()::TEXT || '\_%' ESCAPE '\'
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type IN ('PUBLIC_USER', 'PHARMACY')
      AND p.is_active = true
  )
);

DROP POLICY IF EXISTS prescriptions_authenticated_update ON storage.objects;
DROP POLICY IF EXISTS prescriptions_user_update ON storage.objects;
CREATE POLICY prescriptions_authenticated_update
ON storage.objects
FOR UPDATE
TO authenticated
USING (
  bucket_id = 'prescriptions'
  AND name LIKE auth.uid()::TEXT || '\_%' ESCAPE '\'
)
WITH CHECK (
  bucket_id = 'prescriptions'
  AND name LIKE auth.uid()::TEXT || '\_%' ESCAPE '\'
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type IN ('PUBLIC_USER', 'PHARMACY')
      AND p.is_active = true
  )
);

-- --------------------------------------------------------------------
-- Medicine image storage policies
-- --------------------------------------------------------------------
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
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type IN ('ADMIN', 'WAREHOUSE')
      AND p.is_active = true
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
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type IN ('ADMIN', 'WAREHOUSE')
      AND p.is_active = true
  )
)
WITH CHECK (
  bucket_id = 'medicines'
  AND EXISTS (
    SELECT 1
    FROM public.profiles p
    WHERE p.id = auth.uid()
      AND p.account_type IN ('ADMIN', 'WAREHOUSE')
      AND p.is_active = true
  )
);
