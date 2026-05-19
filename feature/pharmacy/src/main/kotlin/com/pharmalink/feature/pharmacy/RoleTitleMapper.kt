package com.pharmalink.feature.pharmacy

import com.pharmalink.domain.model.AccountType

fun AccountType.toArabicProfessionalTitle(): String =
    when (this) {
        AccountType.ADMIN -> "مدير النظام"
        AccountType.PHARMACY -> "الصيدلية"
        AccountType.WAREHOUSE -> "المستودع"
        AccountType.PUBLIC_USER -> "مستخدم"
    }

