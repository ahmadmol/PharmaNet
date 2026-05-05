# التشخيص الكامل لنظام PharmaNet

**التاريخ**: 2026-04-30  
**النطاق**: تحليل أمني ومعماري محلي للنظام الكامل بعد Phase 4.5.1B  
**نوع الدليل**: static/local repository evidence فقط  
**Live SQL executed**: لا  
**Runtime/API tests executed**: لا  
**Code changed**: لا  
**Migrations changed**: لا  
**Secrets used/saved**: لا  

## 1. نظرة عامة على النظام

### البنية المعمارية

PharmaNet تطبيق Android مبني حول:

- Kotlin + Jetpack Compose.
- Supabase Auth/PostgREST/Realtime.
- طبقة `AuthRepository` لحالة الجلسة والهوية.
- طبقة `PharmaRepository` مركزية تنفذ أغلب I/O عبر Supabase.
- UseCases في ميزات orders/requests تضيف فحوصات business rules قبل استدعاء repository.
- ViewModels تعتمد غالبًا على `AuthRepository.getUserSnapshot()` وواجهات repository/usecase.

### حدود الثقة

حدود الثقة الفعلية بعد Phase 4.5.1B:

- قاعدة البيانات هي مصدر الحقيقة لـ `profiles.account_type`, `profiles.pharmacy_id`, `profiles.warehouse_id`, و`profiles.is_active`.
- `UserSnapshotStore` هو cache محلي للـ UX والتوجيه وليس مصدر تفويض كافٍ بمفرده.
- ViewModel وUseCase checks مفيدة للتجربة، لكنها ليست حدًا أمنيًا.
- RLS/RPC يجب أن يكون الحد الأمني النهائي لكل قراءة/كتابة multi-tenant.

### نموذج البيانات

الأدوار:

- `PUBLIC_USER`: عميل B2C.
- `PHARMACY`: tenant صيدلية عبر `profiles.pharmacy_id`.
- `WAREHOUSE`: tenant مستودع عبر `profiles.warehouse_id`.
- `ADMIN`: دور منصة، لكن صلاحياته DB/RLS غير مثبتة محليًا.

الجداول/التدفقات الأساسية:

- `profiles`: مصدر الدور والربط tenant.
- `orders`: يدعم `CUSTOMER_PHARMACY` و`PHARMACY_WAREHOUSE`.
- `requests`: B2B طلبات الصيدلية للمستودع.
- `warehouses`, `medicines`, `app_notifications`: مستخدمة من repository، لكن لا توجد أدلة RLS محلية كافية لها.

## 2. حالة أمان ملفات التعريف Profiles

### ما هو آمن من 4.5.1B

الدليل المحلي:

- `database/migrations/20260430_harden_profiles_rls.sql:18` يفعل RLS على `public.profiles`.
- `database/migrations/20260430_harden_profiles_rls.sql:50` يضيف own-row `SELECT`.
- `database/migrations/20260430_harden_profiles_rls.sql:60` يضيف own-row `UPDATE`.
- `database/migrations/20260430_harden_profiles_rls.sql:71` يضيف trigger لحماية الحقول الحساسة.
- `database/migrations/20260430_harden_profiles_rls.sql:127` يضيف RPC `create_public_user_profile`.
- `database/migrations/20260430_harden_profiles_rls.sql:185` يلغي direct authenticated `INSERT/UPDATE/DELETE`.
- `database/migrations/20260430_harden_profiles_rls.sql:191` يمنح update فقط للحقول الآمنة.

في Kotlin:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt:207` يقرأ profile أولًا.
- `SupabaseAuthRepository.kt:287` يسمح بإنشاء missing profile فقط إذا كان `AccountType.PUBLIC_USER`.
- `SupabaseAuthRepository.kt:302` يستدعي `create_public_user_profile`.
- `SupabaseAuthRepository.kt:422` يعرّف params آمنة فقط للـ RPC.
- `SupabasePharmaRepository.kt:1570` يعرّف `ProfileUpdateDto` بحقول display/contact فقط.

النتيجة: محليًا، تم إغلاق مسار client-side role/tenant mutation الأساسي.

### ما هو مفترض وغير متحقق

- لم يتم التحقق live أن migration مطبقة في Supabase.
- لم يتم اختبار شكل استجابة `create_public_user_profile` ضد `ProfileRowDto`.
- لم يتم فحص live schema بحثًا عن أعمدة approval/status/admin إضافية في `profiles`.
- لم يتم تشغيل negative tests ضد RLS/grants/triggers.

### ما هو غير آمن أو غير مكتمل

- لا توجد آلية موثقة محليًا لتوفير حسابات `PHARMACY`, `WAREHOUSE`, أو `ADMIN` وربطها tenant بشكل موثوق.
- `is_active` محمي من التعديل، لكنه غير مستخدم محليًا كشرط منع في orders/requests.
- `FORCE ROW LEVEL SECURITY` غير مفعل في profiles عمدًا، وهذا مقبول مؤقتًا لكنه يتطلب جرد admin/service workflows قبل production.

## 3. تحليل نظام الأوامر Orders

### تدفقات الإنشاء

#### B2C CUSTOMER_PHARMACY

المسار:

- UI يختار صيدلية ودواء.
- `CreateCustomerOrderViewModel.kt:131` يقرأ `UserSnapshot`.
- `CreateCustomerOrderViewModel.kt:148` يستدعي use case ويمرر `pharmacyId = state.pharmacy.id`.
- `CreateCustomerOrderUseCase.kt:34` يتحقق أن `accountType == PUBLIC_USER`.
- `SupabasePharmaRepository.kt:888` ينفذ `createCustomerOrder`.
- `SupabasePharmaRepository.kt:900` يعيد حل الهوية من repository.
- `SupabasePharmaRepository.kt:934` يبني `CreateOrderDto`.
- `SupabasePharmaRepository.kt:949` ينفذ direct insert في `orders`.

الحماية المحلية:

- `database/migrations/20260429_harden_b2c_order_rls.sql:35` يعيد إنشاء `customer_create_order`.
- السياسة تتحقق من `auth.uid() = customer_id`, `order_type = CUSTOMER_PHARMACY`, `status = PENDING`, و`profiles.account_type = PUBLIC_USER`.

المخاطر:

- `pharmacy_id` في B2C يأتي من اختيار العميل (`state.pharmacy.id`) وليس من tenant مملوك للعميل. هذا منطقي كهدف B2C، لكنه يحتاج تحقق DB أن الصيدلية target صالحة/نشطة/قابلة للبيع.
- `medicineId` عليه TODO للتحقق catalog-backed في `CreateCustomerOrderUseCase.kt:80` و`SupabasePharmaRepository.kt:917`.

**مستوى المخاطر**: 🟡 متوسط حتى live RLS واختيار الصيدلية يتم التحقق منهما.

#### B2B PHARMACY_WAREHOUSE

الدليل المحلي:

- Generic `createOrder` في `SupabasePharmaRepository` معطل عمدًا.
- إنشاء B2B order يحدث عبر trigger عند إدراج `requests`.
- `database/triggers/create_order_from_request.sql:122` trigger يعمل `AFTER INSERT ON requests`.

المخاطر:

- trigger ينسخ `NEW.pharmacy_id` و`NEW.warehouse_id` إلى `orders` دون فحص auth/tenant داخل function.
- إذا كانت RLS على `requests` ضعيفة أو غير موجودة، يمكن استغلال request insert لتوليد orders مزيفة.
- trigger يعمل عند `INSERT` وليس عند `SUBMIT`، بينما app ينشئ الطلب بحالة `DRAFT`.

**مستوى المخاطر**: 🔴 حرج لأن `requests` RLS غير مثبت محليًا والـ trigger يخلق order تلقائيًا.

### تدفقات القراءة

`SupabasePharmaRepository.fetchOrdersForIdentity`:

- ADMIN: `SupabasePharmaRepository.kt:342` يقرأ كل orders بدون filter.
- WAREHOUSE: `SupabasePharmaRepository.kt:347` يفلتر `warehouse_id = organizationId`.
- PHARMACY: `SupabasePharmaRepository.kt:355` يجلب request ids للصيدلية ثم orders حيث `request_id IN (...)`.
- PUBLIC_USER: `SupabasePharmaRepository.kt:373` يمنع observeOrders العام.
- PUBLIC_USER يستخدم `getMyOrders` في `SupabasePharmaRepository.kt:1182` مع `customer_id = current user`.

المخاطر:

- ADMIN broad read يجب أن يكون مقصودًا ومدعومًا بسياسة RLS صريحة؛ غير مثبت live.
- PHARMACY `fetchOrdersForIdentity` لا يغطي B2C orders ذات `request_id = null`؛ قد تكون فجوة وظيفية لا أمنية.
- WAREHOUSE يعتمد على `organizationId` من snapshot، وsnapshot فيه fallback legacy للمستودعات.

**مستوى المخاطر**: 🟡 متوسط، و🔴 إذا كانت RLS live تسمح بتجاوز filters.

### تدفقات التحديث

B2C:

- إلغاء العميل: `SupabasePharmaRepository.kt:956` ثم RPC `cancel_customer_order`.
- تأكيد/رفض/تسليم الصيدلية: `SupabasePharmaRepository.kt:989`, `1030`, `1063`, `1101`, `1139` وكلها تستدعي RPC.
- RPCs في `20260429_harden_b2c_order_rls.sql` تتحقق من `profiles.account_type` و`profiles.pharmacy_id`.

B2B:

- `updateOrderStatus`, `createOrder`, `deleteOrder` generic معطلة في repository.
- B2B lifecycle يبدو مرتبطًا بـ `requests.updateRequest`, لا `orders` مباشرة.
- Local orders B2B policies القديمة (`warehouse_update_b2b`) لا تثبت status transition constraints.

**مستوى المخاطر**: B2C 🟡 متوسط حتى live RPC tests، B2B 🔴 حرج/غير مكتمل.

### فجوات RLS في Orders

من migrations:

- `orders` RLS مفعلة محليًا في `20250425_extend_orders_for_b2c.sql:116` و`20260429_harden_b2c_order_rls.sql:21`.
- B2C create/lifecycle عُززت في `20260429_harden_b2c_order_rls.sql`.
- B2B policies القديمة محفوظة عمدًا ولم تُعزز في `20260429_harden_b2c_order_rls.sql:14`.
- `warehouse_create_b2b` في `20250425_extend_orders_for_b2c.sql:185` يسمح للمستودع بإنشاء B2B order إذا طابق `warehouse_id`، وهذا يتعارض معماريًا مع أن B2B orders يجب أن تنشأ من requests trigger.
- لا يوجد local policy يثبت دور `WAREHOUSE` عبر `profiles.account_type = WAREHOUSE` في B2B policies القديمة.

### سيناريوهات الاستغلال

1. إذا استطاع مستخدم إدراج request مزور، يمكنه توليد order عبر trigger.
2. إذا بقيت `warehouse_create_b2b` فعالة live، قد يتمكن مستودع من إنشاء B2B order مباشرة بدل مسار request.
3. إذا لم تكن `customer_create_order` الجديدة مطبقة live، يمكن لأي authenticated role إنشاء B2C order لنفسه.
4. إذا لم تكن profiles hardening مطبقة live، يمكن spoofing لـ `pharmacy_id` أو `warehouse_id` أن يفتح قراءة/تحديث orders عبر policies المعتمدة على profiles.

## 4. تحليل نظام الطلبات Requests

### تدفقات الإدراج

المسار:

- `CreateRequestViewModel` يبني `Request` من snapshot والصيدلية الحالية والمستودع المختار.
- `SupabasePharmaRepository.kt:548` ينفذ `createRequest`.
- `SupabasePharmaRepository.kt:562` يتحقق من `AccountType.PHARMACY`.
- `SupabasePharmaRepository.kt:566` يأخذ `pharmacyId` من `identity.organizationId`.
- `SupabasePharmaRepository.kt:579` يأخذ `warehouseId` من request القادم من UI/domain.
- `SupabasePharmaRepository.kt:587` يفرض `status = DRAFT`.
- `SupabasePharmaRepository.kt:591` ينفذ insert في `requests`.

المخاطر:

- لا توجد migration محلية تثبت RLS على `requests`.
- `warehouse_id` يختاره العميل من قائمة warehouses؛ إن لم تكن `warehouses` reads و`requests` insert محمية، يمكن اختيار مستودع غير مسموح.
- لأن trigger ينشئ order عند request insert، حتى DRAFT request ينتج order PENDING.

**مستوى المخاطر**: 🔴 حرج.

### تدفقات القراءة

`SupabasePharmaRepository.fetchRequests`:

- PHARMACY: `requests.pharmacy_id = organizationId`.
- WAREHOUSE: `requests.warehouse_id = organizationId`.
- ADMIN: no filter.
- PUBLIC_USER: empty list.

`observeIncomingRequestsForWarehouse`:

- `SupabasePharmaRepository.kt:413` يقبل `warehouseId` كمعامل ويقرأ `requests.warehouse_id = warehouseId`.
- `RequestListViewModel.kt:68` يمرر warehouseId من snapshot، لكنه يبقى client-side input.

المخاطر:

- إن غابت RLS، يمكن استدعاء repository/API أو endpoint مباشرة بwarehouseId آخر.
- ADMIN broad read غير مثبت DB-side.

**مستوى المخاطر**: 🔴 حرج حتى إثبات RLS.

### تدفقات التحديث/الحذف

`updateRequest`:

- `SupabasePharmaRepository.kt:688` ينفذ update request.
- يقرأ current request عبر `getRequest`.
- يتحقق من ownership في Kotlin.
- يبني `RequestUpdateDto` في `SupabasePharmaRepository.kt:743`.
- DTO يكتب `status`, `warehouse_id`, `warehouse_name`, `notes`.
- update filter فقط `id = requestId` في `SupabasePharmaRepository.kt:751`.

`submitRequest`:

- `SupabasePharmaRepository.kt:781` يتحقق من request مملوك للصيدلية.
- يحدّث `status = PENDING` بفلتر `id` و`pharmacy_id`.

`deleteRequest`:

- `SupabasePharmaRepository.kt:762` يسمح فقط للصيدلية المالكة وDRAFT.
- delete بفلتر `id` و`pharmacy_id`.

المخاطر:

- `updateRequest` يعتمد على Kotlin ownership ثم update بفلتر `id` فقط؛ RLS يجب أن يمنع bypass أو race/crafted client.
- `RequestUpdateDto` يسمح بتغيير `warehouse_id`; يجب تحديد هل هذا مسموح بعد DRAFT أم لا.
- لا توجد RLS محلية تمنع صيدلية من تعديل طلبات غيرها عند bypass.

**مستوى المخاطر**: 🔴 حرج.

### التفاعل مع المشغل

- trigger يعمل `AFTER INSERT`, لا عند `DRAFT -> PENDING`.
- ينسخ `pharmacy_id`, `warehouse_id`, `medicine_name`, `quantity`, `unit`, `warehouse_name`, `supplier_name`, `created_at`, و`priority`.
- لا يفشل request إذا فشل order creation (`create_order_from_request.sql:84` إلى `93`).
- يوجد helper `create_order_for_existing_request` بلا auth checks واضحة في `create_order_from_request.sql:131`.

**خلاصة Requests**: هذا هو أكبر سطح خطر حاليًا.

## 5. تحليل المشغلات Triggers

### create_order_from_request

الموقع:

- `database/triggers/create_order_from_request.sql:13`
- trigger في `database/triggers/create_order_from_request.sql:122`

السلوك:

- يعمل `AFTER INSERT ON requests`.
- ينشئ order بحالة `PENDING`.
- ينسخ tenant fields من request.
- يحدث `related_order_id` على request.
- يمنع duplicates عبر `request_id`.

المخاطر:

1. يعمل عند INSERT، بينما التطبيق ينشئ `requests.status = DRAFT`.
2. لا يتحقق من أن request submitted.
3. لا يتحقق من أن `pharmacy_id` يطابق `auth.uid()` أو trusted profile.
4. لا يتحقق من أن `warehouse_id` مستودع صالح/مرئي.
5. لا يجعل فشل إنشاء order يفشل إنشاء request، مما قد يترك بيانات غير متسقة.
6. helper function `create_order_for_existing_request` لا يظهر عليها `SECURITY DEFINER` أو grants، لكن أيضًا لا توجد revokes/permissions موثقة؛ يجب ضبطها أو حذفها.

**مستوى المخاطر**: 🔴 حرج.

### مشغلات أخرى

الأدلة المحلية:

- `trigger_orders_updated` في `20250425_extend_orders_for_b2c.sql:212`.
- `prevent_profiles_protected_field_update` في `20260430_harden_profiles_rls.sql:118`.
- `create_order_from_request` في trigger file.

لا توجد ملفات SQL أخرى تثبت مشغلات إضافية.

## 6. فجوات RLS الكاملة

### جداول بدون RLS مثبت محليًا

| الجدول | الدليل المحلي | الخطر |
|---|---|---|
| `profiles` | RLS موجود في migration 4.5.1B، live غير متحقق | 🟡 متوسط حتى live |
| `orders` | RLS موجود، B2C معزز، B2B غير مكتمل | 🟡/🔴 |
| `requests` | لا يوجد `ALTER TABLE requests ENABLE ROW LEVEL SECURITY` محليًا | 🔴 حرج |
| `warehouses` | لا توجد RLS policies محلية | 🟡 متوسط؛ يؤثر على اختيار target warehouse/pharmacy |
| `medicines` | لا توجد RLS policies محلية | 🟡 متوسط؛ catalog/data exposure |
| `app_notifications` | لا توجد RLS policies محلية | 🔴 حرج إذا يحتوي tenant notifications |

### سياسات RLS ضعيفة أو غير مكتملة

- B2B orders policies القديمة لا تتحقق من `profiles.account_type`.
- `warehouse_create_b2b` يسمح بإنشاء B2B orders من warehouse، بينما المعمارية الحالية تقول request trigger هو مصدر الإنشاء.
- لا توجد request policies محلية.
- `profiles.is_active` غير مستخدم في orders/requests.

### RPCs غير آمنة أو تحتاج تحقق

| RPC/function | الخطر | الحكم |
|---|---|---|
| `create_public_user_profile` | مصمم جيدًا محليًا، لكن response shape/live behavior غير متحقق | 🟡 |
| B2C order RPCs | مصممة للتحقق من role/tenant، لكن live غير متحقق | 🟡 |
| `create_order_from_request` trigger function | لا يطبق auth/tenant/status checks بنفسه ويثق بـ NEW | 🔴 |
| `create_order_for_existing_request` | helper بلا نموذج صلاحيات موثق | 🔴 |

## 7. نقاط الضعف الحرجة

1. **غياب RLS محلي مثبت على `requests`**
   - الموقع: لا توجد migration لـ `requests` RLS؛ `SupabasePharmaRepository.kt:591`, `751`, `773`, `798`.
   - الاستغلال: forged insert/update/delete أو cross-tenant read إذا bypassed client.
   - الخطورة: 🔴 حرج.
   - الإصلاح: Phase 4.5.4 لتعزيز requests RLS/RPC.

2. **Trigger ينشئ B2B order عند INSERT وليس SUBMIT**
   - الموقع: `database/triggers/create_order_from_request.sql:122`.
   - الاستغلال: إنشاء DRAFT request يولد order PENDING مبكرًا.
   - الخطورة: 🔴 حرج.
   - الإصلاح: نقل الإنشاء إلى transition `DRAFT -> PENDING` أو RPC submit موثوق.

3. **Trigger يثق بـ `NEW.pharmacy_id` و`NEW.warehouse_id`**
   - الموقع: `create_order_from_request.sql:59` و`60`.
   - الاستغلال: إذا request insert غير محمي، attacker ينسخ tenant ids مزورة إلى orders.
   - الخطورة: 🔴 حرج.
   - الإصلاح: trigger/RPC يعيد حل tenant من `profiles` أو يعمل بعد RLS request insert صارم.

4. **B2B orders policies لم تُعزز**
   - الموقع: `20260429_harden_b2c_order_rls.sql:14`، و`20250425_extend_orders_for_b2c.sql:178`, `185`, `193`.
   - الاستغلال: warehouse direct B2B insert/update إذا policies live تسمح.
   - الخطورة: 🔴 حرج.
   - الإصلاح: حذف direct B2B writes أو نقلها لـ RPCs/trigger موثوق مع role checks.

5. **ADMIN صلاحيات غير معرفة**
   - الموقع: `SupabasePharmaRepository.kt:342`, `400`, `535`.
   - الاستغلال: broad reads في client إذا RLS تسمح، أو غموض في صلاحيات المسؤول.
   - الخطورة: 🟡/🔴 حسب live RLS.
   - الإصلاح: تصميم admin role model وسياسات RLS/RPC صريحة.

6. **Warehouse legacy fallback**
   - الموقع: `UserIdentityMapper.kt:18` و`UserSnapshotStore.kt:99`.
   - الاستغلال: أي خلط بين `pharmacy_id` و`warehouse_id` قد يوسع tenant access.
   - الخطورة: 🟡 متوسط.
   - الإصلاح: إزالة fallback بعد اكتمال warehouse linkage migration.

7. **is_active غير مطبق كشرط وصول**
   - الموقع: Phase 4.5.1B يحمي الحقل، لكن لا يوجد check محلي في orders/requests.
   - الاستغلال: مستخدم معطل قد يستمر في تنفيذ عمليات إن بقيت الجلسة صالحة.
   - الخطورة: 🟡 متوسط.
   - الإصلاح: قرار product ثم RLS/RPC checks على `profiles.is_active`.

8. **اختيار target pharmacy في B2C غير مثبت DB-side**
   - الموقع: `CreateCustomerOrderViewModel.kt:155`, `SupabasePharmaRepository.kt:939`.
   - الاستغلال: إنشاء order لصيدلية غير نشطة/غير صالحة إذا لا توجد constraints.
   - الخطورة: 🟡 متوسط.
   - الإصلاح: validate target pharmacy عبر profiles/catalog/RPC.

## 8. تحليل البنية المعمارية

### انتهاكات Clean Architecture

- `PharmaRepository` يجمع I/O ومنطق business validation كثيفًا، مثل role checks, transitions, ownership checks.
- UseCases موجودة وتتحقق، لكن repository يعيد التحقق أيضًا. هذا جيد أمنيًا لكنه يخلط طبقات business rules وI/O.
- بعض ViewModels تعتمد على `AuthRepository.getUserSnapshot()` وتتحقق من role قبل usecase. هذا UX check فقط.
- لا يوجد استخدام مباشر لـ Supabase من UseCases حسب البحث؛ Supabase محصور في repositories/data source.

### مسؤوليات الطبقات

- Repository حاليًا هو الحارس التطبيقي الأقوى فوق RLS، وليس مجرد I/O.
- UseCase يحتوي قواعد flow، لكنه أحيانًا يعتمد على accountType/pharmacyId الممرر من snapshot.
- ViewModel يمرر IDs من navigation/UI مثل `pharmacyId` و`warehouseId`.

### DTO ↔ Domain وdata leakage

- `CreateOrderDto` يكتب `pharmacy_id` و`customer_id`; `customer_id` آمن نسبيًا لأنه من `identity.userId`، أما `pharmacy_id` فهو target client-selected ويحتاج DB validation.
- `RequestInsertDto` يكتب `pharmacy_id` من trusted snapshot و`warehouse_id` من UI selection.
- `RequestUpdateDto` يكتب `warehouse_id`, `status`, `notes`; يجب تقييده DB-side.
- `ProfileUpdateDto` آمن محليًا.

## 9. خريطة المخاطر الشاملة

| الوظيفة/المسار | المخاطر | الموقع | الإصلاح |
|---|---|---|---|
| `ensureProfileForCurrentUser` | 🟢 منخفض بعد 4.5.1B؛ live RPC غير متحقق | `SupabaseAuthRepository.kt:207` | live bootstrap/RPC tests |
| `createMissingProfileForAllowedPublicUser` | 🟡 RPC response/duplicate behavior غير متحقق | `SupabaseAuthRepository.kt:287` | اختبار RPC live |
| `mapUser` | 🟡 metadata لا يزال request signal مطلوبًا قبل profile | `SupabaseAuthRepository.kt:335` | توثيق/اختبار عدم استخدامه authorization truth |
| `UserSnapshotStore` | 🟡 cache محلي للدور/tenant | `UserSnapshotStore.kt:57` | re-bootstrap دوري أو invalidation عند profile changes |
| `UserIdentityMapper` | 🟡 warehouse fallback | `UserIdentityMapper.kt:18` | إزالة fallback |
| `updateProfile` | 🟢 منخفض | `SupabasePharmaRepository.kt:256` | live own-row RLS test |
| `fetchOrdersForIdentity ADMIN` | 🟡/🔴 broad read | `SupabasePharmaRepository.kt:342` | admin RLS design |
| `fetchOrdersForIdentity WAREHOUSE` | 🟡 يعتمد على tenant + RLS | `SupabasePharmaRepository.kt:347` | live isolation tests |
| `fetchOrdersForIdentity PHARMACY` | 🟡 B2B only via request ids؛ B2C detail gap | `SupabasePharmaRepository.kt:355` | clarify B2C pharmacy read path |
| `createCustomerOrder` | 🟡 target pharmacy client-selected | `SupabasePharmaRepository.kt:888` | validate target pharmacy |
| `cancelCustomerOrder` | 🟡 يعتمد على RPC live | `SupabasePharmaRepository.kt:956` | live RPC negative tests |
| `confirm/reject/mark B2C` | 🟡 يعتمد على RPC live | `SupabasePharmaRepository.kt:989` وما بعدها | live RPC negative tests |
| `getMyOrders` | 🟡 يعتمد على RLS live | `SupabasePharmaRepository.kt:1182` | cross-user tests |
| `createRequest` | 🔴 request RLS غير مثبت + trigger side effect | `SupabasePharmaRepository.kt:548` | requests RLS/RPC |
| `updateRequest` | 🔴 update filter id only + DTO يكتب status/warehouse | `SupabasePharmaRepository.kt:688` | request lifecycle RPCs |
| `submitRequest` | 🔴 trigger already ran at insert | `SupabasePharmaRepository.kt:781` | move order creation to submit |
| `deleteRequest` | 🔴 يحتاج DB-side delete policy | `SupabasePharmaRepository.kt:762` | request delete RLS |
| `observeIncomingRequestsForWarehouse` | 🔴 warehouseId parameter client-controlled | `SupabasePharmaRepository.kt:413` | resolve warehouse id inside repository/RLS |
| `create_order_from_request` | 🔴 trusts NEW and fires on INSERT | `database/triggers/create_order_from_request.sql:13` | replace with trusted submit RPC/trigger |
| `create_order_for_existing_request` | 🔴 helper صلاحياته غير موثقة | `database/triggers/create_order_from_request.sql:131` | revoke/drop or secure |
| `warehouse_create_b2b` policy | 🔴 direct B2B order creation | `20250425_extend_orders_for_b2c.sql:185` | drop/replace |
| `warehouse_update_b2b` policy | 🔴 no transition constraints | `20250425_extend_orders_for_b2c.sql:193` | RPC lifecycle |
| `app_notifications` access | 🔴 RLS غير مثبت | `SupabasePharmaRepository.kt:299`, `426` | notification RLS |
| `warehouses` read | 🟡 RLS غير مثبت | `SupabasePharmaRepository.kt:68`, `74` | public-safe view/policy |
| `medicines` read | 🟡 RLS غير مثبت | `SupabasePharmaRepository.kt:107` | catalog policy/view |

## 10. المراحل التالية المطلوبة

### المرحلة 4.5.2: خطة التحقق المباشر

- تطبيق migrations على dev/staging.
- تشغيل negative tests لـ profiles, orders, requests.
- اختبار RPC response shapes.
- توثيق live policies/grants/triggers.

### المرحلة 4.5.3: تعزيز Orders

- إعادة تصميم B2B orders policies.
- إسقاط `warehouse_create_b2b` إن كان trigger/RPC هو المصدر الوحيد.
- إضافة role checks لـ B2B policies: `PHARMACY` و`WAREHOUSE`.
- منع direct lifecycle updates واستبدالها بـ RPCs.
- إضافة `is_active` إذا قرر المنتج أنه access gate.

### المرحلة 4.5.4: تعزيز Requests

- تفعيل RLS على `requests`.
- سياسات/RPCs للإدراج والقراءة والتحديث والحذف.
- منع cross-pharmacy وcross-warehouse.
- تقييد `warehouse_id` وstatus transitions.
- منع PUBLIC_USER من أي access.

### المرحلة 4.5.5: إصلاح المشغلات

- نقل order creation من INSERT إلى trusted submit path.
- جعل submit request عملية DB واحدة: validate ownership, status transition, create order, link request.
- حذف أو تأمين `create_order_for_existing_request`.
- جعل failure atomic بدل request ينجح وorder يفشل بصمت.

### المرحلة 4.5.6: Admin Provisioning

- تصميم trusted admin/server path لتعيين:
  - `account_type`
  - `pharmacy_id`
  - `warehouse_id`
  - `is_active`
- تحديد هل ADMIN يستخدم RLS policies أم service/admin-only RPCs.
- اختبار privilege escalation.

## 11. الحكم النهائي

```text
غير آمن للإنتاج الآن
```

التفسير:

- `profiles` أصبح محليًا آمنًا بشكل مشروط بعد Phase 4.5.1B، لكن لم يتم التحقق live.
- B2C orders تحسنت عبر RPC/RLS migration، لكنها ما زالت تحتاج live verification.
- `requests` لا يوجد لها RLS محلي مثبت، وهي تتحكم بإنشاء B2B orders عبر trigger.
- trigger الحالي ينشئ orders عند INSERT حتى لحالة DRAFT، وينسخ tenant fields من request دون تحقق داخلي.
- B2B orders policies القديمة متروكة عمدًا وغير معززة.
- ADMIN model و`is_active` enforcement غير محددين.

يمكن مواصلة التطوير فوق هذا التشخيص، لكن لا يجوز اعتبار النظام production-ready حتى تنفيذ مراحل التحقق والتعزيز أعلاه.

## 12. Scope Confirmation

تم في هذه المهمة:

- إنشاء هذا التقرير فقط.
- قراءة وفحص ملفات `.specify` ذات الصلة عبر جرد وبحث repo-wide.
- فحص Kotlin في طبقات data/domain/presentation/feature/app ذات الصلة.
- فحص SQL في `database/migrations` و`database/triggers`.

لم يتم:

- تعديل Kotlin.
- تعديل SQL migrations أو triggers.
- تنفيذ SQL live.
- تشغيل API/runtime tests.
- حفظ أسرار أو مفاتيح أو JWTs.
