-- ====================================================================
-- Profile avatar persistence (profiles.avatar_url + profile-avatars storage)
-- Date: 2026-06-01
-- Purpose:
--   - Add profiles.avatar_url column
--   - Create profile-avatars storage bucket
--   - Enforce user-owned object paths via storage.foldername(name)[1] = auth.uid()::text
--   - Make Supabase PostgREST schema reload occur for contracts
-- ====================================================================

-- --------------------------------------------------------------------
-- profiles.avatar_url
-- --------------------------------------------------------------------
ALTER TABLE public.profiles
ADD COLUMN IF NOT EXISTS avatar_url text;

-- --------------------------------------------------------------------
-- Storage bucket: profile-avatars
-- --------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
  (
    'profile-avatars',
    'profile-avatars',
    true,
    1048576,
    ARRAY['image/jpeg', 'image/png', 'image/webp']
  )
ON CONFLICT (id) DO UPDATE
SET
  name = EXCLUDED.name,
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

-- --------------------------------------------------------------------
-- Storage policies: profile-avatars (user-owned uploads)
-- --------------------------------------------------------------------
-- Public read (needed because avatarUrl is treated as a direct public URL)
DROP POLICY IF EXISTS profile_avatars_public_read ON storage.objects;
CREATE POLICY profile_avatars_public_read
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'profile-avatars');

-- Authenticated upload/delete/update constraints:
-- Path MUST be under the authenticated user's folder.
-- We constrain by foldername(name)[1] = auth.uid()::text
DROP POLICY IF EXISTS profile_avatars_user_upload ON storage.objects;
DROP POLICY IF EXISTS profile_avatars_user_update ON storage.objects;
DROP POLICY IF EXISTS profile_avatars_user_delete ON storage.objects;

CREATE POLICY profile_avatars_user_upload
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'profile-avatars'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY profile_avatars_user_update
ON storage.objects
FOR UPDATE
TO authenticated
USING (
  bucket_id = 'profile-avatars'
  AND (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
  bucket_id = 'profile-avatars'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY profile_avatars_user_delete
ON storage.objects
FOR DELETE
TO authenticated
USING (
  bucket_id = 'profile-avatars'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

GRANT UPDATE (avatar_url) ON public.profiles TO authenticated;

NOTIFY pgrst, 'reload schema';
