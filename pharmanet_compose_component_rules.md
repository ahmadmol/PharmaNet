# 📐 PharmaNet / صيدليتي — Jetpack Compose UI Rules

> نسخة مخصصة لمشروع **PharmaNet / صيدليتي** بدل القواعد العامة الموجودة في الملف الأصلي.  
> هذه القواعد موجّهة لأي AI Agent يعمل داخل Windsurf/Codex/Stitch على واجهات Jetpack Compose في المشروع.

---

## 0. Project Context

المشروع هو تطبيق Android متعدد الأدوار للصيدليات والمستودعات والمستخدم العام، مبني على:

- Kotlin
- Jetpack Compose
- MVVM
- Clean Architecture
- Supabase
- Role-aware navigation
- UseCases للـ business rules
- Repository layer للـ data access فقط
- Arabic RTL UI

الأدوار الأساسية:

- `PUBLIC_USER`
- `PHARMACY`
- `WAREHOUSE`
- `ADMIN`

أي واجهة جديدة يجب أن تحترم الدور المستهدف ولا تكشف مسارات أو إجراءات غير مسموحة لهذا الدور.

---

## 1. Non-Negotiable Architecture Rules

### 1.1 UI لا يحتوي Business Logic

ممنوع داخل Composable:

- استدعاء Repository
- استدعاء Supabase
- تنفيذ permission checks عميقة
- تنفيذ ownership checks
- تنفيذ state transition validation
- إنشاء fake success
- استخدام `!!`
- تنفيذ navigation من component reusable

المسموح:

```kotlin
Screen -> ViewModel -> UseCase -> Repository
```

وليس:

```kotlin
Composable -> Repository
Composable -> Supabase
Composable -> UseCase
ReusableComponent -> ViewModel
```

### 1.2 Screen emits events فقط

الشاشة ترسل أحداث:

```kotlin
onSubmitClick()
onBackClick()
onMedicineSelected(...)
onFulfillmentTypeChange(...)
```

والـ ViewModel يقرر ماذا يفعل.

### 1.3 Components stateless by default

أي component reusable يجب أن يستقبل:

- data عبر parameters
- state عبر parameters
- events عبر lambdas

ولا يقرأ ViewModel مباشرة.

---

## 2. Scope Protection Rules

قبل أي تنفيذ يجب على الـ AI Agent ذكر:

1. المرحلة الحالية
2. الدور المستهدف
3. الملفات التي سيلمسها
4. الملفات التي لن يلمسها
5. هل يوجد schema/model/repository change أم لا

إذا كان الطلب UI فقط:

ممنوع لمس:

- `Order.kt`
- `Request.kt`
- `SupabasePharmaRepository.kt`
- `PharmaRepository.kt`
- أي migration SQL
- Auth/session logic
- role guards
- Bottom Bar role logic إلا بتصريح صريح

إذا كان الطلب backend/domain فقط:

ممنوع لمس:

- Composable screens
- navigation
- strings UI copy إلا إذا كان compile يتطلب ذلك
- visual design

---

## 3. Current Project Package Guidance

لا تفترض بنية عامة مثل `ui/screens/...` إذا لم تكن موجودة. استخدم بنية المشروع الحالية.

أمثلة من المشروع الحالي:

```text
app/src/main/kotlin/com/pharmalink/feature/main/navigation/
app/src/main/kotlin/com/pharmalink/core/navigation/

core/common/src/main/kotlin/com/pharmalink/domain/model/
core/common/src/main/kotlin/com/pharmalink/data/repository/
core/common/src/main/kotlin/com/pharmalink/core/repository/

feature/home/src/main/kotlin/com/pharmalink/feature/home/
feature/orders/src/main/kotlin/com/pharmalink/feature/orders/
feature/orders/src/main/kotlin/com/pharmalink/feature/orders/usecase/
feature/request/src/main/kotlin/com/pharmalink/feature/request/
feature/warehouses/src/main/kotlin/com/pharmalink/feature/warehouses/
feature/profile/src/main/kotlin/com/pharmalink/feature/profile/
```

### Placement rules

- شاشة خاصة بـ feature معيّن توضع داخل نفس feature.
- Components الخاصة بشاشة واحدة توضع في نفس الملف أو `components/` داخل نفس feature إذا كان الحجم كبيرًا.
- Component عام قابل لإعادة الاستخدام يوضع فقط في design system/shared UI إذا كان المشروع يملك مكانًا واضحًا لذلك.
- لا تنشئ package جديد باسم عام إلا إذا كان مطابقًا لبنية المشروع.

---

## 4. File Layout Rules

كل ملف Compose يجب أن يكون بهذا الترتيب:

```text
1. package + imports
2. public route/screen composable
3. content composable
4. private section composables
5. private item/card composables
6. preview إن كانت conventions المشروع تسمح
7. UI state/data classes الخاصة بالملف إذا لم تكن في ملف منفصل
```

مثال:

```kotlin
@Composable
fun CreateCustomerOrderScreen(
    uiState: CreateCustomerOrderUiState,
    onEvent: (CreateCustomerOrderEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CreateCustomerOrderContent(
        uiState = uiState,
        onEvent = onEvent,
        modifier = modifier,
    )
}

@Composable
private fun CreateCustomerOrderContent(
    uiState: CreateCustomerOrderUiState,
    onEvent: (CreateCustomerOrderEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // UI only
}

@Composable
private fun OrderSummaryCard(...) {
    // UI only
}
```

---

## 5. Parameter Rules

### 5.1 Modifier position

`modifier: Modifier = Modifier` يكون آخر non-lambda parameter.

جيد:

```kotlin
@Composable
fun PharmacyCard(
    data: PharmacySummaryUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

سيئ:

```kotlin
@Composable
fun PharmacyCard(
    modifier: Modifier = Modifier,
    data: PharmacySummaryUi,
    onClick: () -> Unit,
)
```

### 5.2 لا تمرر ViewModel للـ components

مسموح فقط في route/screen-level إذا كان المشروع يستخدم ذلك pattern.

سيئ:

```kotlin
@Composable
fun PharmacyCard(viewModel: OrdersViewModel)
```

جيد:

```kotlin
@Composable
fun PharmacyCard(
    pharmacyName: String,
    locationLabel: String,
    onSelectClick: () -> Unit,
)
```

### 5.3 أكثر من 5 حقول؟ استخدم UI data class

```kotlin
data class PharmacySelectionItemUi(
    val pharmacyId: String,
    val pharmacyName: String,
    val locationLabel: String,
    val supportsPickup: Boolean,
    val supportsDelivery: Boolean,
)
```

---

## 6. Strings Rules

ممنوع hardcoded user-visible strings داخل Compose.

استخدم:

```kotlin
stringResource(R.string.customer_order_submit)
```

أو مرر النص من caller إذا كان dynamic.

### PUBLIC_USER safe copy

استخدم هذه النصوص عند بناء flow المستخدم العام:

```text
اختر صيدلية لإرسال الطلب
سيتم تأكيد السعر والتوفر من الصيدلية
بانتظار تأكيد الصيدلية
لن يظهر السعر النهائي قبل تأكيد الصيدلية
تم إرسال الطلب
لا توجد طلبات حتى الآن
```

ممنوع:

```text
متوفر الآن
في المخزون
ادفع الآن
تتبع مباشر
سبب الرفض
```

إلا إذا تمت إضافة backend/schema يدعمها صراحة في مرحلة لاحقة.

---

## 7. Color Rules

لا تستخدم:

```kotlin
Color(0xFF00897B)
Color.Red
```

داخل Composable.

استخدم الموجود في المشروع:

- `MaterialTheme.colorScheme`
- `appColors`
- `appColor`
- أي tokens موجودة في theme/designSystem

مثال مناسب للمشروع:

```kotlin
Text(
    text = title,
    color = MaterialTheme.colorScheme.onSurface,
    style = MaterialTheme.typography.titleMedium,
)
```

أو إذا المشروع يستخدم:

```kotlin
color = MaterialTheme.appColors.textPrimary
```

اتبع الموجود، لا تفرض نظام Theme جديد.

### Stitch/Figma colors

إذا Stitch أعطى لونًا غير موجود:

1. لا تضع hex inline.
2. أضفه كـ token في theme المناسب.
3. استخدم اسم semantic مثل:
   - `MedicalPrimary`
   - `MedicalMintContainer`
   - `WarningSurface`
   - `SuccessContainer`

---

## 8. Typography Rules

لا تستخدم inline:

```kotlin
fontSize = 16.sp
fontWeight = FontWeight.Bold
```

إلا إذا كان المشروع لا يملك token مناسب وبموافقة صريحة.

استخدم:

- `MaterialTheme.typography`
- أو `appTypography` إذا كان مستخدمًا في المشروع

مثال:

```kotlin
Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
)
```

أو:

```kotlin
Text(
    text = title,
    style = MaterialTheme.appTypography.titleMedium,
)
```

حسب النظام الموجود.

---

## 9. Spacing & Size Rules

لا تستخدم `.dp` عشوائيًا في كل مكان.

استخدم الموجود في المشروع:

- `appSpacing`
- spacing tokens
- sizing tokens
- MaterialTheme spacing extension إن وجد

مثال:

```kotlin
Column(
    modifier = modifier.padding(MaterialTheme.appSpacing.screenPadding)
)
```

إذا لم يوجد token مناسب:

- أضف token باسم واضح.
- لا تكرر الرقم نفسه في عدة أماكن.

مسموح بالقيم الرياضية الواضحة فقط مثل:

```kotlin
weight(1f)
alpha(0.5f) // فقط إذا كان token غير متوفر وموضّح
```

---

## 10. Shape & Radius Rules

لا تكتب:

```kotlin
RoundedCornerShape(24.dp)
```

داخل كل component.

استخدم:

- `MaterialTheme.shapes`
- أو shape tokens الموجودة في المشروع

مثال:

```kotlin
Card(
    shape = MaterialTheme.shapes.large,
)
```

إذا كان تصميم Stitch يحتاج radius خاص متكرر، أضفه في theme/shapes بدل inline.

---

## 11. Icon & Image Rules

- استخدم `Icon` للأيقونات.
- استخدم `Image` أو `AsyncImage` للصور فقط.
- `contentDescription = null` للأيقونات decorative.
- إذا كان الزر icon-only ويحتاج accessibility، أضف semantics على clickable container وليس hardcoded description داخل icon.

جيد:

```kotlin
Icon(
    imageVector = Icons.Default.Search,
    contentDescription = null,
)
```

سيئ:

```kotlin
Icon(
    imageVector = Icons.Default.Search,
    contentDescription = "search",
)
```

---

## 12. RTL Rules

المشروع عربي RTL. لذلك:

- لا تفترض left/right.
- استخدم `Start` و `End`.
- استخدم `Alignment.CenterStart` / `CenterEnd` حسب RTL.
- لا تستخدم padding(left/right)، استخدم start/end.
- أي أيقونة اتجاهية يجب أن تكون AutoMirrored إن كانت متاحة.

مثال:

```kotlin
Icon(
    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription = null,
)
```

---

## 13. State Management Rules

### Screen-level

ViewModel يدير:

- loading
- error
- form state
- validation message
- submit state
- navigation effect إذا كان pattern المشروع يستخدم effects

### Component-level

Component يعرض فقط.

مثال:

```kotlin
@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrementClick: () -> Unit,
    onDecrementClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

ولا يفعل:

```kotlin
var quantity by remember { mutableStateOf(1) }
```

إلا إذا كان component مستقل تمامًا ولا يؤثر على domain أو submit.

---

## 14. ViewModel Rules

ViewModel مسموح له:

- جمع UseCase results
- تحديث UiState
- تحويل errors إلى UI messages
- إرسال one-time effects

ViewModel ممنوع أن:

- ينفذ ownership validation بدل UseCase/Repository
- يحلل RLS/security
- ينشئ fake data للنجاح
- يستخدم Supabase مباشرة
- يقرر business state transitions إذا كانت موجودة في UseCase

---

## 15. UseCase Binding Rules For PUBLIC_USER Order Flow

في Phase 4.4B:

- `CreateCustomerOrderScreen` يربط فقط مع `CreateCustomerOrderUseCase`.
- `CustomerOrderSuccessScreen` لا يستدعي repository.
- `MedicineSearchScreen` لا ينشئ order.
- `PharmacySelectionScreen` لا ينشئ order.
- لا تستخدم `CancelCustomerOrderUseCase` هنا إلا إذا كانت الشاشة تدعم cancel فعليًا، وهذا ليس ضمن 4.4B.

---

## 16. PUBLIC_USER Order UI Rules

### Screens المسموحة في 4.4B

```text
MedicineSearchScreen update
PharmacySelectionScreen
CreateCustomerOrderScreen
CustomerOrderSuccessScreen
```

### ممنوع في 4.4B

```text
MyCustomerOrdersScreen
CustomerOrderDetailScreen
PaymentScreen
TrackingScreen
RejectionReasonScreen
Inventory/Stock UI
```

### Forbidden B2C status in UI

لا تعرض `IN_PROGRESS` كحالة للمستخدم العام في هذه المرحلة.

حالات B2C المسموحة:

```text
PENDING
CONFIRMED
REJECTED
READY_FOR_PICKUP
OUT_FOR_DELIVERY
DELIVERED
CANCELLED
```

---

## 17. Navigation Rules

- أضف route فقط عند الحاجة.
- لا تغيّر Bottom Bar إلا بتصريح.
- لا تغيّر role shell.
- لا تكسر PHARMACY/WAREHOUSE navigation.
- استخدم destinations الموجودة في المشروع.

Routes المقترحة:

```text
medicine_search
pharmacy_selection/{medicineId}
create_customer_order/{medicineId}/{pharmacyId}
customer_order_success/{orderId}
```

لـ display data مثل medicineName/pharmacyName:

- استخدم savedStateHandle أو lightweight summary object إذا كان pattern المشروع يسمح.
- لا تكدّس route params كثيرة.

---

## 18. Supabase / Repository Safety Rules

أي UI phase لا تلمس:

- Supabase repository
- DTO
- mapper
- SQL
- RLS

إلا إذا طلبت المرحلة ذلك صراحة.

أي Repository write يجب أن يحقق:

- role validation
- ownership validation
- state transition validation
- no fake success
- no `!!`

---

## 19. DTO / Mapper Rules

إذا تم تعديل Domain model في مرحلة مستقبلية:

يجب تحديث كل التالي معًا:

- Domain model
- DTO
- Mapper
- InMemory repository/sample data
- UI call sites
- tests/compile

ممنوع تعديل `Order.kt` ثم ترك `OrderDto` أو `InMemoryPharmaRepository` مكسورين.

---

## 20. Stitch / Figma Implementation Rules

تصاميم Stitch هي visual reference فقط.

الـ AI Agent يجب أن:

- يطبّق الشكل ضمن architecture المشروع.
- لا ينسخ logic من Stitch.
- لا يضيف schema لأن Stitch أظهر حقلًا.
- لا يضيف payment/tracking/inventory لأن التصميم أظهرها.
- يستبدل أي copy غير آمن بنصوص المشروع المعتمدة.

إذا كان Stitch يحتوي:

```text
متوفر الآن
في المخزون
ادفع الآن
تتبع مباشر
```

استبدلها فورًا بـ:

```text
سيتم تأكيد السعر والتوفر من الصيدلية
بانتظار تأكيد الصيدلية
لن يظهر السعر النهائي قبل تأكيد الصيدلية
```

---

## 21. Error / Empty / Loading Rules

كل شاشة يجب أن تدعم بوضوح:

- loading
- content
- empty إذا منطقي
- error
- retry callback إذا هناك error قابل لإعادة المحاولة

لا تعرض شاشة فارغة بدون تفسير.

---

## 22. Accessibility Rules

- minimum touch target 48dp أو token equivalent.
- icon decorative: `contentDescription = null`
- button text visible أفضل من icon-only.
- complex cards clickable يجب أن يكون لديها نص مرئي واضح.
- لا تعتمد على اللون وحده لتمييز الحالة.

---

## 23. Preview Rules

إذا كان المشروع يستخدم Previews:

- أضف Preview للشاشة أو component المهم.
- لا تضف dependency جديدة للـ previews.
- لا تجعل preview يكسر compile.
- استخدم fake UI data محلي داخل preview فقط.

إذا كان المشروع لا يستخدم previews في هذه feature:

- لا تفرضها.

---

## 24. Build Gate

بعد أي تنفيذ UI:

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

يجب الإبلاغ عن:

1. الملفات المعدلة
2. هل تم لمس UI فقط؟
3. هل تم لمس navigation؟
4. هل تم لمس repository/domain/schema/model؟
5. نتيجة compile
6. warnings غير مانعة

---

## 25. Final Checklist Before Commit

- [ ] لا business logic داخل Composable
- [ ] لا Repository/Supabase داخل Composable
- [ ] لا ViewModel داخل reusable component
- [ ] كل الأحداث lambdas
- [ ] كل dynamic data parameters أو UiState
- [ ] لا hardcoded user-visible strings
- [ ] لا inline colors
- [ ] لا dp/sp عشوائي
- [ ] لا RoundedCornerShape inline إذا يوجد token
- [ ] RTL باستخدام start/end
- [ ] لا `!!`
- [ ] لا fake success
- [ ] لا schema/model changes في UI phase
- [ ] لا payment/tracking/inventory في PUBLIC_USER order UI
- [ ] لا claim للدواء أنه متوفر فعليًا
- [ ] compile ناجح

---

## 26. Recommended Agent Header For Every Task

استخدم هذا في بداية أي prompt تنفيذي:

```text
Before coding:
1. اقرأ .specify/memory/constitution.md
2. اقرأ spec الخاص بالمرحلة الحالية
3. اذكر الملفات التي ستعدلها
4. اذكر الملفات التي لن تلمسها
5. لا تبدأ التنفيذ إذا كان الطلب يتعارض مع constitution
```

---

## 27. Project-Specific Rule Summary

قاعدة المشروع الذهبية:

```text
UI يعرض فقط.
ViewModel يدير الحالة فقط.
UseCase يملك business rules.
Repository يملك data access وdefensive validation.
Supabase/RLS هو خط الدفاع النهائي.
```

أي كود يخالف هذا التسلسل يجب رفضه أو إيقافه قبل التنفيذ.
