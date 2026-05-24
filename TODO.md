# TODO - Profile/EditProfile Avatar Persistence Update

## Step 1: Inspect & confirm current avatar wiring (UI + state + repository)
- [x] Read: `core/common/domain/model/PharmacyProfile.kt`
- [x] Read: `core/common/data/repository/SupabasePharmaRepository.kt` (targeted avatar mapping + delete extractor verified)
- [x] Read: `core/common/data/repository/PharmaRepository.kt`
- [x] Read: `feature/profile/ProfileScreen.kt`
- [x] Read: `feature/profile/ProfileUiState.kt`
- [x] Read: `feature/profile/ProfileViewModel.kt`
- [x] Read: `feature/profile/EditProfileScreen.kt` (no avatar picker wiring found in captured content)
- [x] Re-inspect in `SupabasePharmaRepository.kt`:
  - [x] `ProfileDetailsDto` / `ProfileUpdateDto` / `WarehouseProfileUpdateDto` include `avatar_url`
  - [x] `toDomain(...)` maps `avatarUrl` into `PharmacyProfile`
  - [x] `extractOwnedProfileAvatarStoragePath(...)` exists and is path-safe

## Step 2: Migration + backend storage support
- [x] Add migration SQL:
  - [x] `public.profiles.avatar_url` column
  - [x] `profile-avatars` storage bucket
  - [x] RLS/storage policies enforcing:
    - [x] `bucket_id = 'profile-avatars' AND (storage.foldername(name))[1] = auth.uid()::text`
  - [x] End with `NOTIFY pgrst, 'reload schema';`
- [x] Verify migration status (file added/updated correctly)

## Step 3: UI state propagation (Profile UI shows persisted avatar)
- [x] Fix `ProfileUiState.fromSnapshot(...)` to not hard-set `profileImageUrl = null`
- [x] Fix `feature/profile/ProfileViewModel` combine mapping to populate `ProfileUiState.profileImageUrl`

## Step 4: EditProfile avatar picker + local preview + save/refresh behavior
- [x] Implement image picker (`image/*`) and local preview only inside `EditProfileScreen`
- [ ] Update save flow:
  - [x] Preserve existing `PharmacyProfile` and copy only editable fields + avatarUrl
  - [x] Upload avatar first using `uploadProfileAvatar(uri)`
  - [x] Only after successful upload, call `updateProfile` with returned URL
  - [x] Trigger refresh after successful save
  - [x] If `updateProfile` fails after upload, best-effort delete uploaded avatar via `deleteProfileAvatar`

## Step 5: Profile avatar rendering
- [x] Update `ProfileScreen` hero:
  - [x] remote image if persisted url loads
  - [x] fallback to initials from `userName`
  - [x] fallback to Person icon

## Step 6: Storage delete path safety
- [x] Ensure `deleteProfileAvatar` can only delete:
  - [x] objects in `profile-avatars`
  - [x] path first folder == auth.uid()
  - [x] derived path is rejected if URL is external/unknown

## Step 7: Build + checks
- [x] Run `git diff --check`
- [ ] Run:
  - [x] `.\gradlew :feature:profile:compileDebugKotlin`
  - [x] `.\gradlew :core:common:compileDebugKotlin`
  - [x] `.\gradlew :app:compileDebugKotlin`
  - [x] `.\gradlew :app:assembleDebug -x lint`
