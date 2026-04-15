# 🧪 دليل الاختبار والتصحيح - PharmaLink Auth

## ✅ أضف هذا في أي ViewModel للتطوير فقط (احذفه في الإنتاج)

```kotlin
// في أي ViewModel (مثل SplashViewModel أو SignUpViewModel)
init {
    Log.d("AuthDebug", "=== Auth Debug Info ===")
    Log.d("AuthDebug", "Supabase URL: ${BuildConfig.SUPABASE_URL.take(25)}...")
    Log.d("AuthDebug", "Internal email domain: @pharmalink.app")
}

// ✅ في UI، راقب الأخطاء:
LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { error ->
        Log.e("AuthUI", "❌ Error shown: $error")
        // يمكن عرض Snackbar هنا
    }
}

LaunchedEffect(uiState.isSuccess) {
    if (uiState.isSuccess) {
        Log.d("AuthUI", "✅ Success! Navigating to MainTabs")
    }
}
```

## 🔍 استكشاف الأخطاء السريعة

| الخطأ | السبب المحتمل | الحل |
|-------|----------------|------|
| `Sign up succeeded but no session` | Email Confirmation مفعّل | عطّله في Supabase Dashboard |
| `Invalid phone number` | SyrianPhone validation | جرّب `912345678` (9 أرقام) |
| `Network error` | لا يوجد إنترنت أو URL خطأ | تحقق من `local.properties` |
| `Permission denied` | RLS Policy تمنع الكتابة | تأكد من `users_insert_own_profile` |
| `Session not restored` | مشكلة في الحفظ | تحقق من إذن الإنترنت وعدم وجود Proguard |

## 📝 Logcat Filters

استخدم هذه الفلاتر في Android Studio Logcat:

```
Tag: Auth
Tag: AuthUI  
Tag: AuthDebug
Tag: SignUpVM
```

## 🛠️ خطوات التنفيذ النهائية

1. **تنفيذ SQL**:
   ```sql
   -- انسخ والصق في Supabase Dashboard → SQL Editor
   -- من ملف navigation_updates.md أو السكريبت في المرحلة 5
   ```

2. **تعطيل تأكيد البريد**:
   - Supabase Dashboard → Authentication → Settings
   - Email Confirmation: **Disable**

3. **التحقق من local.properties**:
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_KEY=your-anon-key
   ```

4. **إعادة بناء المشروع**:
   ```bash
   ./gradlew clean build
   ```

## 🚀 اختبار السيناريو الكامل

### 1. التسجيل:
- إدخال: `phone=912345678`, `password=123456`, `fullName=أحمد`, `accountType=PHARMACY`
- المتوقع: إنشاء حساب + سجل profiles + انتقال لـ MainTabs

### 2. تسجيل الدخول:
- إدخال: `phone=912345678`, `password=123456`
- المتوقع: نجاح الدخول + استعادة الجلسة

### 3. إعادة التشغيل:
- إغلاق وفتح التطبيق
- المتوقع: الانتقال التلقائي لـ MainTabs (بدون طلب تسجيل دخول)

### 4. الأخطاء:
- محاولة تسجيل برقم موجود → "هذا الرقم مسجل مسبقاً"
- كلمة مرور قصيرة → "كلمة المرور يجب أن تكون 6 أحرف على الأقل"

## ⚠️ ملاحظات هامة

- **الـ Logging**: جميع الأكواد تحتوي على `Log.d/e` لتتبع العملية
- **معالجة الأخطاء**: جميع الرسائل مترجمة للعربية وواضحة للمستخدم
- **الـ Navigation**: تم تحديث `PharmaNavHost.kt` ليدعم Splash screen
- **الـ RLS**: مفعّل بالكامل مع سياسات الأمان المناسبة
- **الـ Performance**: تم إضافة indexes للبحث السريع في جدول profiles

## 🎯 نقاط التحقق النهائية

- [ ] ✅ تنفيذ سكريبت SQL في Supabase Dashboard
- [ ] ✅ تعطيل **Email Confirmation**: Authentication → Settings → Email
- [ ] ✅ التحقق من `local.properties`: يحتوي على `SUPABASE_URL` و `SUPABASE_KEY` 
- [ ] ✅ إعادة بناء المشروع: `./gradlew clean build` 
- [ ] ✅ تشغيل التطبيق واختبار السيناريو الكامل
- [ ] ✅ التحقق من Logcat للأخطاء والنجاحات
