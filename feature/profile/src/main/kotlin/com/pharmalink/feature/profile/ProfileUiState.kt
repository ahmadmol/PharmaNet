package com.pharmalink.feature.profile

import com.pharmalink.domain.model.UserSnapshot

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val accountType: String = "",
    val pharmacyName: String = "",
    val pharmacyAddress: String = "",
    val profileImageUrl: String? = null,
    val settingsOptions: List<SettingItem> = listOf(
        SettingItem("الإشعارات", "تعديل إعدادات الإشعارات"),
        SettingItem("الأمان والخصوصية", "تغيير كلمة المرور، المصادقة الثنائية"),
        SettingItem("اللغة", "العربية"),
        SettingItem("المساعدة والدعم", "الأسئلة الشائعة، التواصل معنا"),
        SettingItem("حول التطبيق", "الإصدار، سياسة الخصوصية"),
    ),
) {
    companion object {
        fun fromSnapshot(snapshot: UserSnapshot?): ProfileUiState =
            ProfileUiState(
                userName = snapshot?.displayName.orEmpty().ifBlank { "مستخدم" },
                userEmail = snapshot?.email.orEmpty(),
                userPhone = snapshot?.phoneNumber.orEmpty(),
                accountType = snapshot?.accountType?.name?.replace('_', ' ') ?: "",
                pharmacyName = snapshot?.pharmacyName.orEmpty(),
                pharmacyAddress = snapshot?.pharmacyId.orEmpty(),
            )
    }
}

data class SettingItem(
    val title: String,
    val subtitle: String,
)
