# PHARMACY Account Fixes — Task Progress

## Phase 0: Read All Target Files
- [ ] Read PharmaNavigator.kt
- [ ] Read PharmacyDashboardScreen.kt
- [ ] Read PharmacyDashboardViewModel.kt
- [ ] Read PharmacyDashboardUiState.kt
- [ ] Read PharmacyRadarScreen.kt
- [ ] Read PharmacyRadarViewModel.kt
- [ ] Read PharmacyCustomerOrdersScreen.kt
- [ ] Read SupabasePharmaRepository.kt

## Phase 1: PH-1 — Notifications tab indicator fix
- [ ] Implement fix in PharmaNavigator.kt
- [ ] Compile check :app

## Phase 2: PH-2 — Dashboard "account not linked" state
- [ ] Update PharmacyDashboardUiState.kt (add isPharmacyLinked)
- [ ] Update PharmacyDashboardViewModel.kt (inject AuthRepository, compute linkage)
- [ ] Update PharmacyDashboardScreen.kt (render notice, gate taps)
- [ ] Compile check :feature:pharmacy

## Phase 3: PH-3 — Radar empty state (location vs no orders)
- [ ] Update PharmacyRadarViewModel.kt (expose coordinates-missing flag)
- [ ] Update PharmacyRadarScreen.kt (split empty state)
- [ ] Compile check :feature:pharmacy

## Phase 4: PH-4 — Customer-order info chips semantics
- [ ] Update PharmacyCustomerOrdersScreen.kt (replace SuggestionChip with labels)
- [ ] Compile check :feature:orders

## Phase 5: PH-5 — Duplicate pharmacy notification removal
- [ ] Update SupabasePharmaRepository.kt (remove sendAppNotification in createCustomerOrder)
- [ ] Compile check :core:common

## Phase 6: PH-6 — Dead customer-notification code removal
- [ ] Update SupabasePharmaRepository.kt (remove return@onSuccess + sendCustomerOrderNotification in 7 methods)
- [ ] Compile check :core:common

## Phase 7: Build Verification
- [ ] Contract test: `gradlew :core:common:testDebugUnitTest --tests com.pharmalink.data.repository.BackendReadinessContractTest`
- [ ] Compile :core:common
- [ ] Compile :feature:pharmacy
- [ ] Compile :feature:orders
- [ ] Compile :app
- [ ] (Optional) `gradlew assembleDebug`
- [ ] Confirm all validation checklist items