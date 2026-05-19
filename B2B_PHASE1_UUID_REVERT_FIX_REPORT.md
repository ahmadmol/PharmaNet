# تقرير تصحيح B2B Phase 1 Migration - إرجاع UUID

## التاريخ
2026-05-06

## الخلفية
كان هناك إصلاح سابق حوّل B2B RPCs إلى TEXT بناءً على أدلة محلية قديمة، لكن نتيجة Supabase SQL Editor الحقيقية أثبتت أن هذا غير صحيح.

### Schema الحقيقي من Supabase
```sql
-- requests table
id: uuid default gen_random_uuid()
pharmacy_id: uuid
warehouse_id: uuid
medicine_id: uuid (nullable)
related_order_id: uuid (nullable)

-- orders table
id: uuid default gen_random_uuid()
request_id: uuid (nullable)
pharmacy_id: uuid
warehouse_id: uuid
medicine_id: uuid
```

---

## A. لماذا كان TEXT fix خطأ؟

بعد فحص Supabase الحقيقي عبر SQL Editor، تبين أن:

1. **جميع IDs في requests/orders هي UUID** وليست TEXT
2. `orders.id` له `default gen_random_uuid()` - لا يحتاج توليد يدوي
3. `requests.related_order_id` هو UUID - لا يقبل نص مثل `'order_...'`
4. المقارنات `WHERE id = p_request_id` تفشل عندما يكون parameter TEXT والعمود UUID
5. الـ TEXT fix كان يولّد `v_order_id TEXT` ويكتبه في `related_order_id uuid` مما يسبب type mismatch

**الخلاصة:** TEXT fix كان مبنياً على افتراض خاطئ وكان سيفشل عند التطبيق على Supabase الحقيقي.

---

## B. ما الذي تم إرجاعه إلى UUID؟

### 1. RPC Signatures
تم تغيير جميع الدوال من TEXT إلى UUID:

```sql
-- قبل (خطأ)
submit_pharmacy_request(p_request_id TEXT)
warehouse_accept_b2b_request(p_request_id TEXT, ...)
warehouse_reject_b2b_request(p_request_id TEXT, ...)
warehouse_start_b2b_fulfillment(p_request_id TEXT)
warehouse_mark_b2b_delivered(p_request_id TEXT, ...)

-- بعد (صحيح)
submit_pharmacy_request(p_request_id UUID)
warehouse_accept_b2b_request(p_request_id UUID, ...)
warehouse_reject_b2b_request(p_request_id UUID, ...)
warehouse_start_b2b_fulfillment(p_request_id UUID)
warehouse_mark_b2b_delivered(p_request_id UUID, ...)
```

### 2. المقارنات داخل الدوال
تم إزالة جميع casts غير الضرورية:

```sql
-- الآن جميع المقارنات مباشرة بدون cast
WHERE id = p_request_id
WHERE request_id = p_request_id
```

### 3. GRANT EXECUTE
تم تصحيح التوقيعات:

```sql
-- قبل (خطأ)
GRANT EXECUTE ON FUNCTION public.submit_pharmacy_request(TEXT) TO authenticated;

-- بعد (صحيح)
GRANT EXECUTE ON FUNCTION public.submit_pharmacy_request(UUID) TO authenticated;
```

---

## C. هل أُزيل v_order_id TEXT؟

✅ **نعم، تم إزالته بالكامل**

```sql
-- قبل (خطأ)
DECLARE
  v_order_id TEXT;
BEGIN
  v_order_id := 'order_' || substr(md5(p_request_id || clock_timestamp()::text), 1, 16);
  INSERT INTO public.orders (id, ...) VALUES (v_order_id, ...);
  UPDATE public.requests SET related_order_id = v_order_id WHERE ...;
END;

-- بعد (صحيح)
DECLARE
  v_order RECORD;
BEGIN
  INSERT INTO public.orders (...) VALUES (...) RETURNING * INTO v_order;
  UPDATE public.requests SET related_order_id = v_order.id WHERE ...;
END;
```

---

## D. هل orders.id يعتمد الآن على gen_random_uuid()؟

✅ **نعم، بشكل كامل**

- تم إزالة `id` من قائمة columns في INSERT
- تم إزالة `v_order_id` من قائمة VALUES
- PostgreSQL الآن يولّد UUID تلقائياً عبر `default gen_random_uuid()`
- يتم استرجاع الـ UUID المولّد عبر `RETURNING * INTO v_order`

```sql
INSERT INTO public.orders (
  order_type,
  request_id,
  pharmacy_id,
  -- لا يوجد id هنا
  ...
) VALUES (
  'PHARMACY_WAREHOUSE',
  v_request.id,
  v_request.pharmacy_id,
  -- لا يوجد v_order_id هنا
  ...
)
RETURNING * INTO v_order;
```

---

## E. هل related_order_id يأخذ v_order.id uuid؟

✅ **نعم، بشكل صحيح**

```sql
-- قبل (خطأ - كتابة TEXT في UUID column)
UPDATE public.requests
SET related_order_id = v_order_id  -- v_order_id كان TEXT
WHERE id = p_request_id;

-- بعد (صحيح - كتابة UUID في UUID column)
UPDATE public.requests
SET related_order_id = v_order.id  -- v_order.id هو UUID
WHERE id = p_request_id;
```

---

## F. هل تم حل SUBMITTED → PENDING؟

✅ **نعم، تم إضافة status compatibility fix**

تم إضافة في Section 2 من migration:

```sql
-- تحويل البيانات القديمة
UPDATE public.requests
SET status = 'PENDING',
    updated_at = now()
WHERE status = 'SUBMITTED';

-- تغيير default للبيانات الجديدة
ALTER TABLE public.requests
  ALTER COLUMN status SET DEFAULT 'DRAFT';
```

**السبب:**
- Supabase الحالي يحتوي على 7 requests بحالة 'SUBMITTED'
- Phase 1 lifecycle يستخدم 'PENDING' وليس 'SUBMITTED'
- هذا يضمن أن البيانات القديمة تعمل مع RPCs الجديدة

---

## G. هل status default صار DRAFT؟

✅ **نعم**

```sql
ALTER TABLE public.requests
  ALTER COLUMN status SET DEFAULT 'DRAFT';
```

هذا يضمن أن:
- Pharmacy تنشئ requests بحالة 'DRAFT'
- `submit_pharmacy_request` يقبل فقط 'DRAFT' ويحولها إلى 'PENDING'
- Warehouse يرى فقط 'PENDING' requests

---

## H. هل بقيت أي بقايا TEXT؟

✅ **لا، تم تنظيف كل شيء**

تم إضافة cleanup section:

```sql
-- --------------------------------------------------------------------
-- 4. Clean up legacy TEXT-based RPCs (if any)
-- --------------------------------------------------------------------

DROP FUNCTION IF EXISTS public.submit_pharmacy_request(TEXT);
DROP FUNCTION IF EXISTS public.warehouse_accept_b2b_request(TEXT, BIGINT, TEXT);
DROP FUNCTION IF EXISTS public.warehouse_reject_b2b_request(TEXT, TEXT);
DROP FUNCTION IF EXISTS public.warehouse_start_b2b_fulfillment(TEXT);
DROP FUNCTION IF EXISTS public.warehouse_mark_b2b_delivered(TEXT, TEXT);
```

هذا يضمن:
- حذف أي نسخ TEXT خاطئة إن وجدت محلياً
- عدم تعارض بين توقيعات TEXT و UUID
- النسخ UUID الصحيحة فقط هي الموجودة

---

## I. نتيجة Build

```
BUILD SUCCESSFUL in 20s
```

✅ **Kotlin code لا يحتوي على أخطاء**

### لماذا Kotlin لا يحتاج تعديل؟

```kotlin
// Kotlin يرسل String
@Serializable
private data class B2bRequestIdRpcParams(
    @SerialName("p_request_id") val requestId: String,
)

// Supabase يحول String UUID إلى UUID تلقائياً
supabase.postgrest.rpc("submit_pharmacy_request", params)
```

**الخلاصة:**
- Kotlin يرسل `requestId: String` يحتوي UUID صالح
- Supabase Postgrest يحول String إلى UUID تلقائياً عند استدعاء RPC
- لا حاجة لتغيير Kotlin code

---

## J. هل migration أصبحت Safe locally؟

### ✅ Safe Locally

**الأسباب:**

1. **Schema corrections صحيحة:**
   - `requests.medicine_id UUID nullable` ✓
   - `requests.rejection_reason TEXT nullable` ✓
   - `orders.request_id nullable` ✓
   - Constraints تدعم B2B و B2C ✓

2. **Status compatibility fix موجود:**
   - تحويل SUBMITTED → PENDING ✓
   - Default = DRAFT ✓

3. **RPC signatures صحيحة:**
   - جميع parameters UUID ✓
   - لا يوجد TEXT casts ✓
   - GRANT EXECUTE على UUID signatures ✓

4. **orders.id generation صحيح:**
   - يعتمد على gen_random_uuid() ✓
   - لا يوجد توليد يدوي ✓
   - related_order_id يأخذ UUID ✓

5. **Cleanup موجود:**
   - حذف TEXT-based RPCs ✓
   - حذف legacy triggers ✓

6. **Kotlin build ناجح:**
   - لا يوجد compile errors ✓
   - RPC params ترسل String UUID ✓

### ⚠️ ملاحظات قبل التطبيق على Supabase

1. **Backup أولاً:** خذ backup من production قبل التطبيق
2. **Test locally:** اختبر على local Supabase أولاً إن أمكن
3. **Rollback plan:** جهّز plan للرجوع إن حدثت مشاكل
4. **Monitor:** راقب logs بعد التطبيق

---

## الخلاصة

### ما تم إصلاحه:
✅ إرجاع RPC signatures من TEXT إلى UUID  
✅ إزالة v_order_id TEXT  
✅ الاعتماد على gen_random_uuid() لـ orders.id  
✅ related_order_id يأخذ v_order.id UUID  
✅ حل SUBMITTED → PENDING  
✅ status default = DRAFT  
✅ تنظيف بقايا TEXT  
✅ Kotlin build ناجح  

### القرار النهائي:
**Safe locally** - Migration جاهزة للتطبيق مع أخذ الاحتياطات المذكورة أعلاه.

---

## الخطوات التالية (ممنوع تنفيذها الآن)

1. ❌ لا تطبق migration على Supabase
2. ❌ لا تبدأ Phase 2
3. ❌ لا تضف Inventory أو Reports أو Notifications
4. ✅ انتظر موافقة المستخدم قبل أي خطوة

---

**انتهى تصحيح migration باتجاه UUID. القرار: Safe locally.**
