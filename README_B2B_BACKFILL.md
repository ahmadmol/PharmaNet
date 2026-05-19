# B2B Phase 1 Manual Backfill - README

## 🎯 نظرة عامة

هذا المجلد يحتوي على حل كامل لمعالجة 7 SUBMITTED requests بشكل آمن قبل تطبيق B2B Phase 1 migration.

### المشكلة
- ✅ 7 requests بحالة SUBMITTED بدون orders
- ❌ Fuzzy matching ربط أموكسيسيلين 250mg بـ 500mg (غير آمن)
- ❌ 3 أدوية ناقصة من قاعدة البيانات

### الحل
✅ Manual mapping آمن مع preflight checks وverification

---

## 🚀 البدء السريع

### 1. اقرأ هذا أولاً
→ **`QUICK_START_B2B_BACKFILL.md`** (5 دقائق)

### 2. شغّل Preflight Check
```sql
\i database/migrations/20260506_b2b_preflight_check.sql
```

### 3. أضف Medicines الناقصة
```sql
\i database/migrations/20260506_add_missing_medicines.sql
```

### 4. املأ Mapping Table
عدّل `database/migrations/20260506_b2b_manual_backfill.sql`

### 5. شغّل Backfill
```sql
\i database/migrations/20260506_b2b_manual_backfill.sql
```

### 6. طبّق Core Stabilization
```sql
\i database/migrations/20260505_b2b_core_stabilization.sql
```

---

## 📁 الملفات

### ⭐ ابدأ من هنا
- **`B2B_BACKFILL_INDEX.md`** - دليل جميع الملفات
- **`QUICK_START_B2B_BACKFILL.md`** - دليل سريع
- **`B2B_BACKFILL_FINAL_ANSWER.md`** - إجابات مباشرة

### 🔧 Migration Scripts
- `database/migrations/20260506_b2b_preflight_check.sql`
- `database/migrations/20260506_add_missing_medicines.sql`
- `database/migrations/20260506_b2b_manual_backfill.sql` ⚠️ يحتاج تعديل
- `database/migrations/20260505_b2b_core_stabilization.sql`

### 📖 Documentation
- `B2B_PHASE1_MIGRATION_GUIDE.md` - دليل شامل
- `B2B_PHASE1_MANUAL_BACKFILL_REPORT.md` - تقرير تفصيلي
- `B2B_BACKFILL_SUMMARY.md` - ملخص

### 📝 Examples
- `database/migrations/EXAMPLE_MAPPING.sql` - مثال mapping

---

## ⚠️ تحذيرات مهمة

### ❌ ممنوع:
- استخدام fuzzy matching تلقائي
- ربط أموكسيسيلين 250mg بـ medicine_id لـ 500mg
- تخطي preflight check
- تطبيق core stabilization قبل backfill

### ✅ واجب:
- Backup قبل التطبيق
- Manual verification لكل mapping
- Test على staging أولاً
- Monitor logs بعد التطبيق

---

## 📊 الإجابات السريعة

### هل نحتاج إضافة medicines؟
✅ **نعم** - 3 أدوية (أموكسيسيلين 250mg، فيتامين سي 1000mg، ايبوبروفين 400mg)

### ما request_id لكل دواء؟
❓ **يحتاج preflight check** - سيعرض القائمة الكاملة

### ما medicine_id الصحيح؟
✅ **جزئياً** - باراسيتامول موجود، الباقي سيتم توليده

### هل يمكن backfill الآن؟
❌ **لا** - يحتاج preflight + add medicines + fill mapping

### هل migration Safe بعد mapping؟
✅ **نعم** - بعد استيفاء checklist الكامل

---

## ⏱️ الوقت المطلوب

| المرحلة | الوقت |
|---------|-------|
| القراءة | 10-30 دقيقة |
| Preflight | 5 دقائق |
| Add Medicines | 2 دقيقة |
| Fill Mapping | 10 دقائق |
| Backfill | 2 دقيقة |
| Core Stabilization | 2 دقيقة |
| Verification | 5 دقائق |
| **الإجمالي** | **~1 ساعة** |

---

## ✅ Checklist

### قبل البدء:
- [ ] قرأت QUICK_START
- [ ] فهمت المشكلة
- [ ] جهزت Supabase SQL Editor
- [ ] أخذت Backup

### أثناء التنفيذ:
- [ ] Preflight check نجح
- [ ] Missing medicines أضيفت
- [ ] Mapping table ملئت يدوياً
- [ ] كل mapping تم التحقق منه
- [ ] Backfill نجح
- [ ] Core stabilization نجح

### بعد الانتهاء:
- [ ] 0 SUBMITTED requests
- [ ] 7 PENDING requests
- [ ] 7 B2B orders
- [ ] 5 RPCs موجودة
- [ ] لا يوجد errors

---

## 🎉 النتيجة المتوقعة

```
✅ 7 SUBMITTED requests → PENDING
✅ 7 orders created
✅ 3 medicines added
✅ B2B Phase 1 RPCs active
✅ RLS policies tightened
✅ Safe to use B2B workflow
```

---

## 📞 الدعم

### إذا واجهت مشاكل:

**"Request not found"**
→ راجع request_id من preflight check

**"Medicine not found"**
→ راجع medicine_id من add_missing_medicines

**"Order already exists"**
→ لا مشكلة، skip

**"Preflight failed"**
→ راجع التفاصيل في output

**"Backfill failed"**
→ راجع mapping table

---

## 📚 المراجع

### للبدء:
- `B2B_BACKFILL_INDEX.md` - دليل الملفات
- `QUICK_START_B2B_BACKFILL.md` - دليل سريع

### للتفاصيل:
- `B2B_PHASE1_MIGRATION_GUIDE.md` - دليل شامل
- `B2B_PHASE1_MANUAL_BACKFILL_REPORT.md` - تقرير تفصيلي

### للأمثلة:
- `database/migrations/EXAMPLE_MAPPING.sql` - مثال mapping

---

## 🔄 Rollback Plan

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
    related_order_id = NULL
WHERE status = 'PENDING'
  AND updated_at > '<timestamp_before_migration>';

-- 3. حذف RPCs (إن تم تطبيق core stabilization)
DROP FUNCTION IF EXISTS public.submit_pharmacy_request(UUID);
-- ... باقي الدوال
```

---

## 🚀 الخطوة التالية

**الآن:**
1. افتح `QUICK_START_B2B_BACKFILL.md`
2. اقرأه (5 دقائق)
3. ابدأ بـ preflight check

**بعد النجاح:**
1. Test B2B workflow
2. Monitor production
3. Document lessons learned

---

## 📝 ملاحظات

- ✅ جميع الملفات جاهزة
- ⚠️ `20260506_b2b_manual_backfill.sql` يحتاج ملء mapping table
- ✅ لا تطبق على Supabase حتى تملأ mapping
- ✅ Test على staging أولاً

---

**تم إنشاء جميع الملفات المطلوبة. جاهز للتطبيق! 🎉**

**ابدأ من:** `QUICK_START_B2B_BACKFILL.md`
