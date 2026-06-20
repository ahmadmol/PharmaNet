package com.pharmalink.feature.admin.ui.inventory

import android.net.Uri

data class EditMedicineUiState(
    val name: String = "",
    val brand: String = "",
    val strength: String = "",
    val description: String = "",
    val specs: String = "",
    val price: String = "",
    val stockQuantity: String = "",
    val isVisible: Boolean = true,
    val isActive: Boolean = true,
    val imageUri: Uri? = null,
    val existingImageUrl: String? = null,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSuccess: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)
