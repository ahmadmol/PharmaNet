package com.pharmalink.feature.admin.ui.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditUserViewModel @Inject constructor(
    private val repository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUserUiState())
    val state: StateFlow<EditUserUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EditUserEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<EditUserEffect> = _effect.asSharedFlow()

    fun loadUser(userId: String, fullName: String, accountType: AccountType, facilityId: String, isActive: Boolean) {
        _state.update {
            it.copy(
                userId = userId,
                fullName = fullName,
                accountType = accountType,
                originalAccountType = accountType,
                facilityId = facilityId,
                isActive = isActive,
                originalIsActive = isActive,
                showSensitiveChangeConfirmation = false,
                sensitiveChangeWarning = "",
            )
        }
    }

    fun onAction(action: EditUserAction) {
        when (action) {
            is EditUserAction.OnFullNameChanged -> updateFullName(action.name)
            is EditUserAction.OnAccountTypeChanged -> updateAccountType(action.type)
            is EditUserAction.OnFacilityIdChanged -> updateFacilityId(action.id)
            is EditUserAction.OnActiveToggled -> updateActive(action.isActive)
            EditUserAction.OnSaveClicked -> saveChanges()
            EditUserAction.OnConfirmSensitiveSave -> saveChanges(skipSensitiveConfirmation = true)
            EditUserAction.OnDismissSensitiveConfirmation -> {
                _state.update {
                    it.copy(
                        showSensitiveChangeConfirmation = false,
                        sensitiveChangeWarning = "",
                    )
                }
            }
            EditUserAction.OnDismiss -> dismiss()
        }
    }

    private fun updateFullName(name: String) {
        _state.update {
            it.copy(
                fullName = name,
                fullNameError = "",
            )
        }
    }

    private fun updateAccountType(type: AccountType) {
        _state.update {
            it.copy(
                accountType = type,
                facilityIdError = "",
            )
        }
    }

    private fun updateFacilityId(id: String) {
        _state.update {
            it.copy(
                facilityId = id,
                facilityIdError = "",
            )
        }
    }

    private fun updateActive(isActive: Boolean) {
        _state.update { it.copy(isActive = isActive) }
    }

    private fun saveChanges(skipSensitiveConfirmation: Boolean = false) {
        val state = _state.value

        // Validate
        val errors = mutableMapOf<String, String>()
        if (state.fullName.isBlank()) {
            errors["fullName"] = "الاسم الكامل مطلوب"
        }
        val needsFacility = state.accountType == AccountType.PHARMACY ||
            state.accountType == AccountType.WAREHOUSE
        if (needsFacility && state.facilityId.isBlank()) {
            errors["facilityId"] = "معرف المنشأة مطلوب"
        }

        if (errors.isNotEmpty()) {
            _state.update {
                it.copy(
                    fullNameError = errors["fullName"] ?: "",
                    facilityIdError = errors["facilityId"] ?: "",
                )
            }
            return
        }

        if (!skipSensitiveConfirmation) {
            val warnings = buildList {
                if (state.accountType == AccountType.ADMIN && state.originalAccountType != AccountType.ADMIN) {
                    add("سيتم منح هذا المستخدم صلاحيات مدير.")
                }
                if (state.originalAccountType == AccountType.ADMIN && state.accountType != AccountType.ADMIN) {
                    add("سيتم سحب صلاحيات المدير من هذا المستخدم.")
                }
                if (state.isActive != state.originalIsActive) {
                    add(if (state.isActive) "سيتم تفعيل الحساب." else "سيتم تعطيل الحساب.")
                }
            }

            if (warnings.isNotEmpty()) {
                _state.update {
                    it.copy(
                        showSensitiveChangeConfirmation = true,
                        sensitiveChangeWarning = warnings.joinToString(separator = "\n"),
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSaving = true,
                    showSensitiveChangeConfirmation = false,
                    sensitiveChangeWarning = "",
                )
            }

            val pharmacyId = if (state.accountType == AccountType.PHARMACY) state.facilityId else null
            val warehouseId = if (state.accountType == AccountType.WAREHOUSE) state.facilityId else null

            repository.adminUpdateUserProfile(
                targetUserId = state.userId,
                fullName = state.fullName.trim(),
                accountType = state.accountType,
                pharmacyId = pharmacyId,
                warehouseId = warehouseId,
                isActive = state.isActive,
            )
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _effect.emit(EditUserEffect.ShowMessage("تم حفظ التغييرات بنجاح"))
                    _effect.emit(EditUserEffect.Dismiss)
                }
                .onFailure { e ->
                    _state.update { it.copy(isSaving = false) }
                    _effect.emit(EditUserEffect.ShowMessage(e.message ?: "فشل حفظ التغييرات"))
                }
        }
    }

    private fun dismiss() {
        viewModelScope.launch {
            _effect.emit(EditUserEffect.Dismiss)
        }
    }
}
