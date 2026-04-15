package com.pharmalink.core.common.error

class MissingPharmacyLinkageException(
    userId: String,
    customMessage: String? = null
) : IllegalStateException(
    customMessage ?: "تعذر إكمال ربط الحساب بالصيدلية لأن profiles.pharmacy_id مفقود للمستخدم $userId في Supabase. اربط الحساب بصيدلية أولا ثم أعد تسجيل الدخول.",
)
