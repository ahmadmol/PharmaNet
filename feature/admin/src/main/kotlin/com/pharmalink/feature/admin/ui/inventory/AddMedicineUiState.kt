package com.pharmalink.feature.admin.ui.inventory

import android.net.Uri

data class AddMedicineUiState(
    val name: String = "",
    val brand: String = "",
    val strength: String = "",
    val description: String = "",
    val specs: String = "",
    val price: String = "",
    val stockQuantity: String = "",
    val isVisible: Boolean = true,
    val imageUri: Uri? = null,
    val isUploading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
