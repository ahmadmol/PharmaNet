# دليل تطبيق B2B Phase 1 Migration

## نظرة عامة
هذا الدليل يشرح كيفية تطبيق B2B Phase 1 migration بشكل آمن مع معالجة البيانات القديمة (SUBMITTED requests).

## الملفات المطلوبة

### 1. Preflight Check
**الملف:** `database/migrations/20260506_b2b_preflight_check.sql`

**الغرض:** فحص حالة البيانات قبل التطبيق

**ماذا يفعل:**
- يعد الـ SUBMITTED requests بدون orders
- يعرض تفاصيل كل request (ID, medicine_name, warehouse, quantity)
- يعرض قائمة medicines الموجودة في قاعدة البيانات
- يفحص orphan orders
- يقرر: PASS أو FAIL

**متى تستخدمه:** قبل أي شيء آخر

---

### 2. Add Missing Medicines
**الملف:** `database/migrations/20260506_add_missing_medicines.sql`

**الغرض:** إضافة الأدوية الناقصة

**ماذا يفعل:**
- يفحص وجود: باراسيتامول 500mg، أموكسيسيلين 250mg، فيتامين سي 1000mg، ايبوبروفين 400mg
- يضيف الأدوية الناقصة فقط
- يعرض medicine_id لكل دواء

**متى تستخدمه:** إذا أظهر preflight check أن medicines ناقصة

---

### 3. Manual Backfill
**الملف:** `database/migrations/20260506_b2b_manual_backfill.sql`

**الغرض:** ربط SUBMITTED requests بـ medicine_id الصحيح وإنشاء orders

**ماذا يفعل:**
- يستخدم manual mapping table (يجب ملؤها يدوياً)
- يحدّث requests.medicine_id
- ينشئ order لكل request
- يحدّث related_order_id
- يحوّل status من SUBMITTED إلى PENDING

**متى تستخدمه:** بعد إضافة medicines الناقصة وقبل core stabilization

**⚠️ مهم:** يجب ملء mapping table يدوياً بـ UUIDs الصحيحة

---

### 4. Core Stabilization
**الملف:** `database/migrations/20260505_b2b_core_stabilization.sql`

**الغرض:** تطبيق B2B Phase 1 schema وRPCs

**ماذا يفعل:**
- يضيف requests.medicine_id و rejection_reason
- يصحح orders constraints
- يحوّل SUBMITTED → PENDING (للبيانات المتبقية)
- ينشئ B2B lifecycle RPCs (UUID-based)
- يشدد RLS policies

**متى تستخدمه:** بعد backfill أو بعد التأكد من عدم وجود SUBMITTED requests

---

## خطوات التطبيق

### السيناريو A: يوجد SUBMITTED requests (الحالة الحالية)

#### الخطوة 1: Preflight Check
```sql
-- في Supabase SQL Editor
\i database/migrations/20260506_b2b_preflight_check.sql
```

**النتيجة المتوقعة:**
```
Check 1: SUBMITTED requests without orders: 7
Details of SUBMITTED requests:
<request_id>                          | <medicine_name>              | <warehouse_name>    | <quantity>
...

Check 3: Available medicines in database:
<medicine_id>                         | <name>                       | <strength>
...

RESULT: ❌ PREFLIGHT FAILED
```

**احفظ:**
- قائمة request_id لكل request
- قائمة medicine_id الموجودة

---

#### الخطوة 2: Add Missing Medicines
```sql
-- في Supabase SQL Editor
\i database/migrations/20260506_add_missing_medicines.sql
```

**النتيجة المتوقعة:**
```
✓ باراسيتامول 500mg already exists: <uuid>
✓ Added أموكسيسيلين 250mg: <uuid>
✓ Added فيتامين سي 1000mg: <uuid>
✓ Added ايبوبروفين 400mg: <uuid>

Medicine IDs for manual mapping:
باراسيتامول 500mg: <uuid-1>
أموكسيسيلين 250mg: <uuid-2>
فيتامين سي 1000mg: <uuid-3>
ايبوبروفين 400mg: <uuid-4>
```

**احفظ:** medicine_id لكل دواء

---

#### الخطوة 3: Fill Manual Mapping

افتح `20260506_b2b_manual_backfill.sql` وابحث عن:

```sql
FOR v_mapping IN (
  SELECT * FROM (VALUES
    -- ⚠️ FILL IN ACTUAL MAPPINGS HERE ⚠️
    (NULL::uuid, NULL::uuid, NULL::text)
  ) AS mapping(request_id, medicine_id, medicine_name_verify)
```

**استبدل بـ:**

```sql
FOR v_mapping IN (
  SELECT * FROM (VALUES
    ('<request-id-1>'::uuid, '<medicine-id-1>'::uuid, 'باراسيتامول 500 مجم'),
    ('<request-id-2>'::uuid, '<medicine-id-2>'::uuid, 'أموكسيسيلين 250 مجم'),
    ('<request-id-3>'::uuid, '<medicine-id-3>'::uuid, 'فيتامين سي 1000 مجم'),
    ('<request-id-4>'::uuid, '<medicine-id-4>'::uuid, 'ايبوبروفين 400 مجم')
    -- أضف باقي الـ 7 requests
  ) AS mapping(request_id, medicine_id, medicine_name_verify)
```

**⚠️ تحذيرات:**
- لا تربط أموكسيسيلين 250 مجم بـ medicine_id لـ 500mg
- تأكد من medicine_name_verify يطابق ما في request
- استخدم UUIDs الحقيقية من الخطوات السابقة

---

#### الخطوة 4: Run Manual Backfill
```sql
-- في Supabase SQL Editor
\i database/migrations/20260506_b2b_manual_backfill.sql
```

**النتيجة المتوقعة:**
```
=== Starting Manual Backfill ===

Processing request <uuid>: باراسيتامول 500 مجم → medicine <uuid>
  ✓ Created order <uuid> and updated request to PENDING
...

=== Backfill Complete ===
Requests updated: 7
Orders created: 7

=== Post-Backfill Verification ===
Remaining SUBMITTED requests: 0
SUBMITTED/PENDING requests without medicine_id: 0
PENDING requests without orders: 0

✅ All checks passed. Safe to proceed with core stabilization migration.
```

---

#### الخطوة 5: Core Stabilization
```sql
-- في Supabase SQL Editor
\i database/migrations/20260505_b2b_core_stabilization.sql
```

**النتيجة المتوقعة:**
```
-- Schema changes applied
-- Status compatibility fix applied (no SUBMITTED left to convert)
-- RPCs created
-- RLS policies updated
```

---

### السيناريو B: لا يوجد SUBMITTED requests

إذا كانت البيانات test/dev فقط:

#### الخيار 1: حذف SUBMITTED requests
```sql
DELETE FROM public.requests WHERE status = 'SUBMITTED';
```

#### الخيار 2: إرجاعها إلى DRAFT
```sql
UPDATE public.requests 
SET status = 'DRAFT', 
    updated_at = now() 
WHERE status = 'SUBMITTED';
```

ثم:
```sql
\i database/migrations/20260505_b2b_core_stabilization.sql
```

---

## التحقق النهائي

بعد تطبيق core stabilization:

```sql
-- 1. تحقق من RPCs
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name LIKE '%b2b%'
ORDER BY routine_name;

-- يجب أن ترى:
-- submit_pharmacy_request
-- warehouse_accept_b2b_request
-- warehouse_reject_b2b_request
-- warehouse_start_b2b_fulfillment
-- warehouse_mark_b2b_delivered

-- 2. تحقق من status distribution
SELECT status, COUNT(*) 
FROM public.requests 
GROUP BY status;

-- يجب ألا ترى SUBMITTED

-- 3. تحقق من B2B orders
SELECT 
  COUNT(*) as total_b2b_orders,
  COUNT(DISTINCT request_id) as unique_requests
FROM public.orders
WHERE order_type = 'PHARMACY_WAREHOUSE';

-- يجب أن يكون unique_requests = عدد PENDING requests

-- 4. تحقق من medicine_id
SELECT COUNT(*) 
FROM public.requests 
WHERE status = 'PENDING' 
  AND medicine_id IS NULL;

-- يجب أن يكون 0
```

---

## استكشاف الأخطاء

### خطأ: "Request not found or not SUBMITTED"
**السبب:** request_id خاطئ في mapping table  
**الحل:** راجع UUIDs من preflight check

### خطأ: "Medicine not found"
**السبب:** medicine_id خاطئ في mapping table  
**الحل:** راجع UUIDs من add_missing_medicines

### خطأ: "Order already exists for request"
**السبب:** تم تشغيل backfill مرتين  
**الحل:** لا مشكلة، skip هذا request

### خطأ: "Missing required medicines"
**السبب:** لم يتم تشغيل add_missing_medicines  
**الحل:** شغّل الخطوة 2 أولاً

---

## ملاحظات مهمة

### ✅ آمن
- Manual mapping بـ UUIDs صحيحة
- إضافة medicines بـ strength صحيح
- Preflight check قبل التطبيق
- Verification بعد كل خطوة

### ❌ غير آمن
- Fuzzy matching تلقائي
- ربط أموكسيسيلين 250mg بـ 500mg medicine
- تطبيق core stabilization قبل backfill
- تخطي preflight check

### 🔄 Rollback Plan
إذا حدثت مشاكل:

```sql
-- 1. حذف orders المنشأة
DELETE FROM public.orders 
WHERE order_type = 'PHARMACY_WAREHOUSE'
  AND created_at > '<timestamp_before_migration>';

-- 2. إرجاع requests
UPDATE public.requests
SET status = 'SUBMITTED',
    medicine_id = NULL,
    related_order_id = NULL,
    updated_at = now()
WHERE status = 'PENDING'
  AND updated_at > '<timestamp_before_migration>';

-- 3. حذف RPCs
DROP FUNCTION IF EXISTS public.submit_pharmacy_request(UUID);
-- ... باقي الدوال
```

---

## الخلاصة

### ترتيب التنفيذ الصحيح:
1. ✅ Preflight Check
2. ✅ Add Missing Medicines (إن لزم)
3. ✅ Fill Manual Mapping
4. ✅ Run Manual Backfill
5. ✅ Core Stabilization
6. ✅ Final Verification

### لا تنسَ:
- ✅ Backup قبل التطبيق
- ✅ Test على local/staging أولاً
- ✅ Manual verification لكل mapping
- ✅ Monitor logs بعد التطبيق
