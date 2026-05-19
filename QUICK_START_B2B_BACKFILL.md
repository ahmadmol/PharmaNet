# دليل سريع: B2B Phase 1 Backfill

## 🎯 الهدف
معالجة 7 SUBMITTED requests بشكل آمن قبل تطبيق B2B Phase 1 migration.

---

## ⚡ الخطوات السريعة

### 1️⃣ Preflight Check (5 دقائق)

افتح Supabase SQL Editor وشغّل:

```sql
\i database/migrations/20260506_b2b_preflight_check.sql
```

**احفظ النتيجة:**
- قائمة request_id
- قائمة medicine_id الموجودة

---

### 2️⃣ Add Missing Medicines (2 دقيقة)

```sql
\i database/migrations/20260506_add_missing_medicines.sql
```

**احفظ النتيجة:**
- medicine_id لكل دواء مضاف

---

### 3️⃣ Fill Mapping Table (10 دقائق)

افتح: `database/migrations/20260506_b2b_manual_backfill.sql`

ابحث عن السطر 95 تقريباً:

```sql
FOR v_mapping IN (
  SELECT * FROM (VALUES
    -- ⚠️ FILL IN ACTUAL MAPPINGS HERE ⚠️
```

**استبدل بـ:**

```sql
FOR v_mapping IN (
  SELECT * FROM (VALUES
    ('<request-uuid-1>'::uuid, '<medicine-uuid-1>'::uuid, 'باراسيتامول 500 مجم'),
    ('<request-uuid-2>'::uuid, '<medicine-uuid-2>'::uuid, 'أموكسيسيلين 250 مجم'),
    ('<request-uuid-3>'::uuid, '<medicine-uuid-3>'::uuid, 'فيتامين سي 1000 مجم'),
    ('<request-uuid-4>'::uuid, '<medicine-uuid-4>'::uuid, 'ايبوبروفين 400 مجم')
    -- أضف باقي الـ 7 requests
```

**⚠️ تحذير:** لا تربط أموكسيسيلين 250mg بـ medicine_id لـ 500mg!

---

### 4️⃣ Run Backfill (2 دقيقة)

```sql
\i database/migrations/20260506_b2b_manual_backfill.sql
```

**تحقق:**
- ✅ Requests updated: 7
- ✅ Orders created: 7
- ✅ All checks passed

---

### 5️⃣ Core Stabilization (2 دقيقة)

```sql
\i database/migrations/20260505_b2b_core_stabilization.sql
```

**تحقق:**
- ✅ RPCs created
- ✅ No errors

---

## 📁 الملفات المطلوبة

| الملف | الغرض | يحتاج تعديل؟ |
|------|-------|-------------|
| `20260506_b2b_preflight_check.sql` | فحص البيانات | ❌ لا |
| `20260506_add_missing_medicines.sql` | إضافة أدوية | ❌ لا |
| `20260506_b2b_manual_backfill.sql` | Backfill | ✅ نعم - املأ mapping |
| `20260505_b2b_core_stabilization.sql` | Migration | ❌ لا |

---

## ⚠️ تحذيرات مهمة

### ❌ ممنوع:
- استخدام fuzzy matching
- ربط 250mg بـ 500mg medicine
- تخطي preflight check
- تطبيق core stabilization قبل backfill

### ✅ واجب:
- Manual verification لكل mapping
- Backup قبل التطبيق
- Test على staging أولاً

---

## 🆘 استكشاف الأخطاء

### "Request not found"
→ راجع request_id من preflight

### "Medicine not found"
→ راجع medicine_id من add_missing_medicines

### "Order already exists"
→ لا مشكلة، skip

---

## 📚 للمزيد

- **دليل شامل:** `B2B_PHASE1_MIGRATION_GUIDE.md`
- **تقرير تفصيلي:** `B2B_PHASE1_MANUAL_BACKFILL_REPORT.md`
