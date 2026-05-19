# تقرير B2B Phase 1 Manual Medicine Backfill

## التاريخ
2026-05-06

## الخلفية
تم اكتشاف 7 requests بحالة SUBMITTED بدون orders. المطابقة التلقائية (fuzzy matching) أعطت نتائج غير آمنة:
- ✅ باراسيتامول 500 مجم matched صحيح
- ❌ أموكسيسيلين 250 مجم matched خطأ مع 500mg
- ❌ فيتامين سي 1000 مجم لا يوجد medicine_id
- ❌ ايبوبروفين 400 مجم لا يوجد medicine_id

---

## 1. هل نحتاج إضافة medicines ناقصة؟

### ✅ نعم، نحتاج إضافة 3 أدوية

#### الأدوية الناقصة:

| الدواء | القوة | الحالة | الإجراء المطلوب |
|--------|------|--------|-----------------|
| أموكسيسيلين | 250mg | ❌ ناقص | يجب إضافته |
| فيتامين سي | 1000mg | ❌ ناقص | يجب إضافته |
| ايبوبروفين | 400mg | ❓ غير مؤكد | يجب التحقق |
| باراسيتامول | 500mg | ✅ موجود | لا إجراء |

#### ملاحظات مهمة:

**أموكسيسيلين 250mg:**
- ❌ لا يوجد في قاعدة البيانات
- ⚠️ يوجد أموكسيسيلين 500mg بـ ID: `8db98fb3-2461-4216-bc35-05906a884888`
- 🚫 **ممنوع** استخدام 500mg medicine_id لطلب 250mg
- ✅ يجب إضافة medicine جديد بقوة 250mg

**فيتامين سي 1000mg:**
- ❌ لا يوجد في قاعدة البيانات
- ⚠️ seed file يحتوي على فيتامين سي 500mg فقط
- ✅ يجب إضافة medicine جديد بقوة 1000mg

**ايبوبروفين 400mg:**
- ❓ seed file يحتوي على ايبوبروفين 400mg
- ❓ لكن seed file يستخدم IDs نصية ('MED-002') بينما Supabase يستخدم UUID
- ✅ يجب التحقق من وجوده في Supabase الحقيقي
- ✅ إذا لم يكن موجوداً، يجب إضافته

**باراسيتامول 500mg:**
- ✅ موجود بـ ID: `2877d1e3-896e-4b0d-8c5d-9fcceb271970`
- ✅ جاهز للاستخدام

---

## 2. ما request_id لكل دواء؟

### ⚠️ معلومات ناقصة

**المشكلة:**
- المستخدم ذكر "7 requests بحالة SUBMITTED"
- لكن لم يتم توفير قائمة request_id الفعلية
- لم يتم توفير تفاصيل أي request يحتوي أي دواء

**ما نعرفه:**
- يوجد 7 requests
- الأدوية المذكورة: باراسيتامول 500، أموكسيسيلين 250، فيتامين سي 1000، ايبوبروفين 400
- لا نعرف كم request لكل دواء

**الحل:**
يجب تشغيل preflight check للحصول على:
```sql
-- سيعرض:
Request ID                            | Medicine Name              | Warehouse Name | Quantity
<uuid-1>                              | باراسيتامول 500 مجم        | ...            | ...
<uuid-2>                              | أموكسيسيلين 250 مجم        | ...            | ...
<uuid-3>                              | فيتامين سي 1000 مجم        | ...            | ...
<uuid-4>                              | ايبوبروفين 400 مجم         | ...            | ...
...
```

---

## 3. ما medicine_id الصحيح لكل request؟

### الوضع الحالي:

| الدواء | medicine_id الحالي | الحالة | medicine_id الصحيح |
|--------|-------------------|--------|-------------------|
| باراسيتامول 500mg | `2877d1e3-896e-4b0d-8c5d-9fcceb271970` | ✅ صحيح | نفسه |
| أموكسيسيلين 250mg | ❌ لا يوجد | ❌ ناقص | سيتم توليده عند الإضافة |
| فيتامين سي 1000mg | ❌ لا يوجد | ❌ ناقص | سيتم توليده عند الإضافة |
| ايبوبروفين 400mg | ❓ غير معروف | ❓ يحتاج تحقق | يحتاج preflight check |

### خطة الحصول على medicine_id الصحيح:

#### الخطوة 1: تشغيل preflight check
```sql
\i database/migrations/20260506_b2b_preflight_check.sql
```
**النتيجة:** قائمة medicines الموجودة مع UUIDs

#### الخطوة 2: إضافة medicines الناقصة
```sql
\i database/migrations/20260506_add_missing_medicines.sql
```
**النتيجة:** 
```
Medicine IDs for manual mapping:
باراسيتامول 500mg: 2877d1e3-896e-4b0d-8c5d-9fcceb271970
أموكسيسيلين 250mg: <new-uuid-1>
فيتامين سي 1000mg: <new-uuid-2>
ايبوبروفين 400mg: <existing-or-new-uuid>
```

#### الخطوة 3: Manual mapping
بعد الحصول على request_ids و medicine_ids، املأ:

```sql
-- في 20260506_b2b_manual_backfill.sql
FOR v_mapping IN (
  SELECT * FROM (VALUES
    ('<request-id-for-paracetamol>'::uuid, '2877d1e3-896e-4b0d-8c5d-9fcceb271970'::uuid, 'باراسيتامول 500 مجم'),
    ('<request-id-for-amoxicillin>'::uuid, '<new-uuid-1>'::uuid, 'أموكسيسيلين 250 مجم'),
    ('<request-id-for-vitamin-c>'::uuid, '<new-uuid-2>'::uuid, 'فيتامين سي 1000 مجم'),
    ('<request-id-for-ibuprofen>'::uuid, '<existing-or-new-uuid>'::uuid, 'ايبوبروفين 400 مجم')
    -- أضف باقي الـ 7 requests
  ) AS mapping(request_id, medicine_id, medicine_name_verify)
```

---

## 4. هل يمكن backfill الآن؟

### ❌ لا، ليس الآن

**الأسباب:**

1. **معلومات ناقصة:**
   - لا نعرف request_id لكل request
   - لا نعرف كم request لكل دواء
   - لا نعرف إذا كان ايبوبروفين 400mg موجود أم لا

2. **medicines ناقصة:**
   - أموكسيسيلين 250mg غير موجود
   - فيتامين سي 1000mg غير موجود
   - ايبوبروفين 400mg يحتاج تحقق

3. **mapping غير جاهز:**
   - mapping table في backfill script فارغة
   - يحتاج ملء يدوي بـ UUIDs صحيحة

### ✅ متى يمكن backfill؟

**بعد:**
1. ✅ تشغيل preflight check والحصول على request_ids
2. ✅ تشغيل add_missing_medicines والحصول على medicine_ids
3. ✅ ملء mapping table يدوياً في backfill script
4. ✅ مراجعة كل mapping للتأكد من صحته

---

## 5. هل migration تصبح Safe to apply بعد manual mapping؟

### ✅ نعم، ستصبح Safe to apply

**بشرط:**

#### ✅ الشروط الواجب توفرها:

1. **Preflight check passed:**
   - تم تشغيل preflight وتوثيق النتائج
   - تم الحصول على جميع request_ids
   - تم الحصول على جميع medicine_ids الموجودة

2. **Missing medicines added:**
   - تم إضافة أموكسيسيلين 250mg
   - تم إضافة فيتامين سي 1000mg
   - تم التحقق من/إضافة ايبوبروفين 400mg
   - تم توثيق medicine_id لكل دواء

3. **Manual mapping verified:**
   - تم ملء mapping table بـ UUIDs صحيحة
   - تم التحقق من كل mapping يدوياً
   - لا يوجد fuzzy matching
   - لا يوجد strength mismatch (مثل 250mg → 500mg)

4. **Backfill successful:**
   - تم تشغيل backfill script
   - تم إنشاء 7 orders
   - تم تحديث 7 requests
   - تم تحويل status إلى PENDING
   - Post-backfill verification passed

5. **No remaining issues:**
   - لا يوجد SUBMITTED requests متبقية
   - لا يوجد PENDING requests بدون medicine_id
   - لا يوجد PENDING requests بدون orders

#### ⚠️ التحذيرات:

**ممنوع:**
- ❌ استخدام fuzzy matching تلقائي
- ❌ ربط أموكسيسيلين 250mg بـ medicine_id لـ 500mg
- ❌ تخطي preflight check
- ❌ تطبيق core stabilization قبل backfill
- ❌ الاعتماد على seed file IDs النصية

**واجب:**
- ✅ Manual verification لكل mapping
- ✅ Backup قبل التطبيق
- ✅ Test على local/staging أولاً
- ✅ Monitor logs بعد التطبيق

---

## الملفات المنشأة

### 1. Preflight Check
**الملف:** `database/migrations/20260506_b2b_preflight_check.sql`

**الوظيفة:**
- يفحص SUBMITTED requests بدون orders
- يعرض تفاصيل كل request
- يعرض medicines الموجودة
- يقرر PASS/FAIL

**الاستخدام:**
```sql
\i database/migrations/20260506_b2b_preflight_check.sql
```

---

### 2. Add Missing Medicines
**الملف:** `database/migrations/20260506_add_missing_medicines.sql`

**الوظيفة:**
- يفحص وجود الأدوية المطلوبة
- يضيف الأدوية الناقصة فقط
- يعرض medicine_id لكل دواء

**الاستخدام:**
```sql
\i database/migrations/20260506_add_missing_medicines.sql
```

**النتيجة المتوقعة:**
```
✓ باراسيتامول 500mg already exists: 2877d1e3-896e-4b0d-8c5d-9fcceb271970
✓ Added أموكسيسيلين 250mg: <uuid>
✓ Added فيتامين سي 1000mg: <uuid>
✓ Added/exists ايبوبروفين 400mg: <uuid>
```

---

### 3. Manual Backfill
**الملف:** `database/migrations/20260506_b2b_manual_backfill.sql`

**الوظيفة:**
- يستخدم manual mapping table
- يحدّث requests.medicine_id
- ينشئ orders
- يحدّث related_order_id
- يحوّل status إلى PENDING

**⚠️ يحتاج تعديل يدوي:**
```sql
-- ابحث عن هذا القسم وعدّله:
FOR v_mapping IN (
  SELECT * FROM (VALUES
    -- املأ هنا بـ UUIDs الحقيقية
    ('<request-id>'::uuid, '<medicine-id>'::uuid, '<medicine-name>')
  ) AS mapping(request_id, medicine_id, medicine_name_verify)
```

**الاستخدام:**
```sql
-- بعد ملء mapping table
\i database/migrations/20260506_b2b_manual_backfill.sql
```

---

### 4. Migration Guide
**الملف:** `B2B_PHASE1_MIGRATION_GUIDE.md`

**المحتوى:**
- شرح كل ملف
- خطوات التطبيق بالتفصيل
- سيناريوهات مختلفة
- استكشاف الأخطاء
- Rollback plan

---

## الخطوات التالية (بالترتيب)

### 🔍 المرحلة 1: جمع المعلومات

```sql
-- 1. تشغيل preflight check
\i database/migrations/20260506_b2b_preflight_check.sql
```

**احفظ من النتيجة:**
- [ ] قائمة request_id لكل request
- [ ] medicine_name لكل request
- [ ] قائمة medicine_id الموجودة

---

### ➕ المرحلة 2: إضافة الأدوية الناقصة

```sql
-- 2. إضافة medicines
\i database/migrations/20260506_add_missing_medicines.sql
```

**احفظ من النتيجة:**
- [ ] medicine_id لـ أموكسيسيلين 250mg
- [ ] medicine_id لـ فيتامين سي 1000mg
- [ ] medicine_id لـ ايبوبروفين 400mg

---

### ✏️ المرحلة 3: إعداد Manual Mapping

1. افتح `database/migrations/20260506_b2b_manual_backfill.sql`
2. ابحث عن `-- ⚠️ FILL IN ACTUAL MAPPINGS HERE ⚠️`
3. استبدل `(NULL::uuid, NULL::uuid, NULL::text)` بـ:

```sql
('<request-id-1>'::uuid, '<medicine-id-1>'::uuid, 'باراسيتامول 500 مجم'),
('<request-id-2>'::uuid, '<medicine-id-2>'::uuid, 'أموكسيسيلين 250 مجم'),
-- ... باقي الـ 7 requests
```

4. **راجع كل سطر:**
   - [ ] request_id صحيح؟
   - [ ] medicine_id صحيح؟
   - [ ] medicine_name يطابق request؟
   - [ ] strength صحيح؟ (250 ≠ 500)

---

### 🔄 المرحلة 4: تطبيق Backfill

```sql
-- 3. تشغيل backfill
\i database/migrations/20260506_b2b_manual_backfill.sql
```

**تحقق من النتيجة:**
- [ ] Requests updated: 7
- [ ] Orders created: 7
- [ ] Remaining SUBMITTED requests: 0
- [ ] ✅ All checks passed

---

### 🚀 المرحلة 5: Core Stabilization

```sql
-- 4. تطبيق core stabilization
\i database/migrations/20260505_b2b_core_stabilization.sql
```

**تحقق من النتيجة:**
- [ ] Schema changes applied
- [ ] RPCs created
- [ ] RLS policies updated
- [ ] No errors

---

### ✅ المرحلة 6: التحقق النهائي

```sql
-- 5. التحقق النهائي
-- تحقق من RPCs
SELECT routine_name 
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name LIKE '%b2b%';

-- تحقق من status
SELECT status, COUNT(*) 
FROM public.requests 
GROUP BY status;

-- تحقق من B2B orders
SELECT COUNT(*) 
FROM public.orders
WHERE order_type = 'PHARMACY_WAREHOUSE';
```

**يجب أن ترى:**
- [ ] 5 RPCs موجودة
- [ ] لا يوجد SUBMITTED status
- [ ] عدد B2B orders = عدد PENDING requests

---

## الخلاصة

### ✅ ما تم إنجازه:

1. ✅ تحليل المشكلة: fuzzy matching غير آمن
2. ✅ تحديد الأدوية الناقصة: 3 أدوية
3. ✅ إنشاء preflight check script
4. ✅ إنشاء add missing medicines script
5. ✅ إنشاء manual backfill script مع template
6. ✅ إنشاء migration guide شامل
7. ✅ توثيق كل الخطوات

### ❌ ما لم يتم (يحتاج معلومات من Supabase):

1. ❌ قائمة request_id الفعلية
2. ❌ medicine_id للأدوية الناقصة (سيتم توليدها)
3. ❌ ملء mapping table (يحتاج UUIDs من preflight)
4. ❌ تطبيق backfill (يحتاج mapping)
5. ❌ تطبيق core stabilization (يحتاج backfill)

### 📋 Checklist للمستخدم:

- [ ] تشغيل preflight check
- [ ] توثيق request_ids
- [ ] تشغيل add_missing_medicines
- [ ] توثيق medicine_ids
- [ ] ملء mapping table
- [ ] مراجعة كل mapping يدوياً
- [ ] تشغيل backfill
- [ ] التحقق من النتائج
- [ ] تطبيق core stabilization
- [ ] التحقق النهائي

### 🎯 القرار النهائي:

**هل يمكن backfill الآن؟**
❌ **لا** - يحتاج معلومات من preflight check

**هل migration ستصبح Safe to apply؟**
✅ **نعم** - بعد إكمال المراحل 1-4 بنجاح

**هل نحتاج إضافة medicines؟**
✅ **نعم** - 3 أدوية على الأقل

**هل manual mapping آمن؟**
✅ **نعم** - أفضل من fuzzy matching بكثير

---

**الخطوة التالية:** تشغيل preflight check على Supabase الحقيقي والحصول على UUIDs
