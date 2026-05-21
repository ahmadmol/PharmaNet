# TODO - Profile/EditProfile Avatar Persistence Update

## Step 1: Inspect & confirm current avatar wiring (UI + state + repository)
- [x] Read: `core/common/domain/model/PharmacyProfile.kt`
- [x] Read: `core/common/data/repository/SupabasePharmaRepository.kt` (partial; needs targeted re-inspection for ProfileDetailsDto/avatar mapping + delete extractor)
- [x] Read: `core/common/data/repository/PharmaRepository.kt`
- [x] Read: `feature/profile/ProfileScreen.kt`
- [x] Read: `feature/profile/ProfileUiState.kt`
- [x] Read: `feature/profile/ProfileViewModel.kt`
- [x] Read: `feature/profile/EditProfileScreen.kt` (no avatar picker wiring found in captured content)
- [ ] Re-inspect in `SupabasePharmaRepository.kt`:
  - [ ] `ProfileDetailsDto` / `ProfileUpdateDto` / `WarehouseProfileUpdateDto` include `avatar_url`
  - [ ] `toDomain(...)` maps `avatarUrl` into `PharmacyProfile`
  - [ ] `extractOwnedProfileAvatarStoragePath(...)` exists and is path-safe

## Step 2: Migration + backend storage support
- [ ] Add migration SQL:
  - [ ] `public.profiles.avatar_url` column
  - [ ] `profile-avatars` storage bucket
  - [ ] RLS/storage policies enforcing:
    - [ ] `bucket_id = 'profile-avatars' AND (storage.foldername(name))[1] = auth.uid()::text`
  - [ ] End with `NOTIFY pgrst, 'reload schema';`
- [ ] Verify migration status (file added/updated correctly)

## Step 3: UI state propagation (Profile UI shows persisted avatar)
- [ ] Fix `ProfileUiState.fromSnapshot(...)` to not hard-set `profileImageUrl = null`
- [ ] Fix `feature/profile/ProfileViewModel` combine mapping to populate `ProfileUiState.profileImageUrl`

## Step 4: EditProfile avatar picker + local preview + save/refresh behavior
- [ ] Implement image picker (`image/*`) and local preview only inside `EditProfileScreen`
- [ ] Update save flow:
  - [ ] Preserve existing `PharmacyProfile` and copy only editable fields + avatarUrl
  - [ ] Upload avatar first using `uploadProfileAvatar(uri)`
  - [ ] Only after successful upload, call `updateProfile` with returned URL
  - [ ] Trigger refresh after successful save
  - [ ] If `updateProfile` fails after upload, best-effort delete uploaded avatar via `deleteProfileAvatar`

## Step 5: Profile avatar rendering
- [ ] Update `ProfileScreen` hero:
  - [ ] remote image if persisted url loads
  - [ ] fallback to initials from `userName`
  - [ ] fallback to Person icon

## Step 6: Storage delete path safety
- [ ] Ensure `deleteProfileAvatar` can only delete:
  - [ ] objects in `profile-avatars`
  - [ ] path first folder == auth.uid()
  - [ ] derived path is rejected if URL is external/unknown

## Step 7: Build + checks
- [ ] Run `git diff --check`
- [ ] Run:
  - [ ] `.\gradlew :feature:profile:compileDebugKotlin`
  - [ ] `.\gradlew :core:common:compileDebugKotlin`
  - [ ] `.\gradlew :app:compileDebugKotlin`
  - [ ] `.\gradlew :app:assembleDebug -x lint`
