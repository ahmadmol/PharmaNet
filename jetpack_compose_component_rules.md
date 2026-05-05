# 📐 Jetpack Compose Component Rules

> Strict rules every coding agent **must follow** when creating, editing, or reviewing Jetpack Compose UI components. No exceptions unless explicitly noted.

---

## Table of Contents

1. [General Principles](#1-general-principles)
2. [File & Package Structure](#2-file--package-structure)
3. [File Layout Order](#3-file-layout-order)
4. [Composable Function Rules](#4-composable-function-rules)
5. [Parameter Rules](#5-parameter-rules)
6. [Color Rules](#6-color-rules)
7. [Typography Rules](#7-typography-rules)
8. [Spacing & Sizing Rules](#8-spacing--sizing-rules)
9. [Corner Radius Rules](#9-corner-radius-rules)
10. [Icon & Image Rules](#10-icon--image-rules)
11. [No Magic Numbers / No Hardcoded Values](#11-no-magic-numbers--no-hardcoded-values)
12. [Event Handlers & onClick Rules](#12-event-handlers--onclick-rules)
13. [State Management Rules](#13-state-management-rules)
14. [List Item Component Rules](#14-list-item-component-rules)
15. [Component Placement Rules](#15-component-placement-rules)
16. [Figma Implementation Rules](#16-figma-implementation-rules)
17. [Utilities & Reusable Helpers](#17-utilities--reusable-helpers)
18. [Preview Rules](#18-preview-rules)
19. [Accessibility Rules](#19-accessibility-rules)
20. [Naming Conventions](#20-naming-conventions)
21. [Import & Dependency Rules](#21-import--dependency-rules)
22. [Checklist Before Committing](#22-checklist-before-committing)

---

## 1. General Principles

- **Every composable must be stateless by default.** State is hoisted to the caller unless explicitly a stateful screen-level composable.
- **Single Responsibility:** Each composable does one thing. If it handles layout AND data logic, split it.
- **No business logic inside a composable.** Logic belongs in a ViewModel or use-case layer.
- **No hardcoded strings, dimensions, colors, or font sizes** inside composable files.
- **Clean Code over clever code.** Readability and explicitness always win.
- **Follow the Unidirectional Data Flow (UDF) pattern.** Data flows down, events flow up.

---

## 2. File & Package Structure

```
ui/
├── components/               ← Generic, reusable composables usable across the whole app
│   ├── buttons/
│   ├── cards/
│   ├── inputs/
│   ├── dialogs/
│   └── ...
├── screens/
│   └── dashboard/
│       ├── DashboardScreen.kt
│       └── components/       ← Dashboard-specific composables, NOT reusable elsewhere
├── theme/
│   ├── Color.kt              ← All color tokens (Material + AppColors)
│   ├── Typography.kt
│   ├── Shape.kt
│   ├── Spacing.kt
│   └── Theme.kt
└── utils/
    └── Utilities.kt
```

**Placement decision:**
- Can the component be used on more than one screen? → `ui/components/`
- Is it tightly coupled to dashboard data/logic/design? → `ui/screens/dashboard/components/`
- When in doubt, prefer `ui/components/` and parameterize the specifics.

### Rules:
- One composable per file unless composables are tightly coupled (parent + its slot composables).
- File name **must match** the primary composable name. `PrimaryButton.kt` → `fun PrimaryButton(...)`.
- Secondary/support composables go into `Utilities.kt` or `<ComponentName>Utils.kt`.
- Theme files are the **single source of truth** for all visual tokens.

---

## 3. File Layout Order

Every composable file must follow this top-to-bottom order:

```
1. Package declaration & imports
2. Primary (parent) composable  ← ALWAYS first
3. Sub-composables that support the parent  ← below the parent
4. Private preview functions
5. Constants, token objects, and sealed classes related to this component  ← ALWAYS last
```

### ✅ DO

```kotlin
// 1. Primary composable — TOP
@Composable
fun NotificationCard(
    data: NotificationCardData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) { /* ... */ }

// 2. Sub-composables — BELOW parent
@Composable
private fun NotificationCardIcon(type: NotificationType, modifier: Modifier = Modifier) { /* ... */ }

@Composable
private fun NotificationCardActions(onDismiss: () -> Unit, modifier: Modifier = Modifier) { /* ... */ }

// 3. Previews
@Preview
@Composable
private fun PreviewNotificationCard() { /* ... */ }

// 4. Constants / data classes / enums — BOTTOM
data class NotificationCardData(val title: String, val body: String, val type: NotificationType)

enum class NotificationType { INFO, WARNING, ERROR }
```

---

## 4. Composable Function Rules

### ✅ DO

```kotlin
@Composable
fun ProductCard(
    title: String,
    subtitle: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) { /* ... */ }
}
```

### ❌ DO NOT

```kotlin
@Composable
fun ProductCard() {
    val title = "My Product"       // ❌ Hardcoded
    val color = Color(0xFF1A73E8)  // ❌ Inline color
    val padding = 16               // ❌ Magic number
}
```

### Rules:
- `modifier: Modifier = Modifier` is **always the last non-lambda parameter**.
- No hardcoded string literals for user-visible text — use parameters or `stringResource(R.string.xxx)`.
- No local `val`/`var` for any visual property (color, size, shape) inside the composable body.
- Composable names use **UpperCamelCase** and are a noun or noun-phrase.

---

## 5. Parameter Rules

> **Core Rule:** Every input arrives via the parameter list. No global/singleton reads, no companion object access.

- All **data** the composable displays → parameters.
- All **actions** the composable triggers → lambda parameters.
- All **visual customizations** → `MaterialTheme` directly, or overridable parameters with `MaterialTheme` defaults.
- **Never** pass a ViewModel into a component-level composable. ViewModels are screen-level only.
- Group related parameters in a data class when there are more than ~5 data fields:

```kotlin
data class UserCardData(
    val name: String,
    val email: String,
    val avatarUrl: String,
    val isVerified: Boolean,
)

@Composable
fun UserCard(
    data: UserCardData,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

---

## 6. Color Rules

> **Core Rule:** Colors come **only** from `MaterialTheme.colorScheme` or named constants defined in `theme/Color.kt`. No inline `Color(0xFFxxxxxx)`, no `Color.Red`.

### Figma Color Workflow

1. Check if the Figma color maps to a Material 3 semantic token (see table below). If yes, use it.
2. If no direct match exists, find the **closest semantic token** and add a comment: `// Closest match to Figma "surface-muted"`.
3. If a completely custom color is required (brand colors, gradients, one-off palette colors), **add it to `theme/Color.kt`** inside `AppColors` before using it in any composable. Never add it inline.
4. Both light **and** dark variants must be defined for every custom color.

```kotlin
// theme/Color.kt ✅
object AppColors {
    val BrandGradientStart = Color(0xFF6A11CB)
    val BrandGradientEnd   = Color(0xFF2575FC)
    val WarningAmber       = Color(0xFFFFB300)
    val WarningAmberDark   = Color(0xFFFF8F00)
}

// ✅ In composable — reference only
containerColor = AppColors.WarningAmber
```

```kotlin
// ❌ Never do this in a composable
containerColor = Color(0xFFFFB300)
```

### Material 3 Token Reference

| Figma Usage | Material 3 Token |
|---|---|
| Primary brand color | `colorScheme.primary` |
| Icon/text on primary | `colorScheme.onPrimary` |
| Primary container / chip background | `colorScheme.primaryContainer` |
| Text/icon on primary container | `colorScheme.onPrimaryContainer` |
| Secondary accent | `colorScheme.secondary` |
| Secondary container | `colorScheme.secondaryContainer` |
| Error / destructive | `colorScheme.error` |
| Error container background | `colorScheme.errorContainer` |
| Page background | `colorScheme.background` |
| Card / sheet surface | `colorScheme.surface` |
| Elevated list item surface | `colorScheme.surfaceVariant` |
| Divider / outline | `colorScheme.outline` |
| Subtle border / decorative | `colorScheme.outlineVariant` |
| Snackbar / inverse surfaces | `colorScheme.inverseSurface` |
| Modal scrim overlay | `colorScheme.scrim` |

---

## 7. Typography Rules

> **Core Rule:** All text styles come from `MaterialTheme.typography`. No inline `fontSize`, `fontWeight`, or `letterSpacing`.

| Figma Text Style | Material 3 Token |
|---|---|
| Display Large (57sp) | `typography.displayLarge` |
| Headline Large (32sp) | `typography.headlineLarge` |
| Headline Medium (28sp) | `typography.headlineMedium` |
| Title Large (22sp) | `typography.titleLarge` |
| Title Medium (16sp medium) | `typography.titleMedium` |
| Body Large (16sp) | `typography.bodyLarge` |
| Body Medium (14sp) | `typography.bodyMedium` |
| Body Small (12sp) | `typography.bodySmall` |
| Label Large (14sp medium) | `typography.labelLarge` |
| Label Medium (12sp medium) | `typography.labelMedium` |
| Label Small (11sp medium) | `typography.labelSmall` |

```kotlin
// ✅ Correct
Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

// ❌ Wrong
Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
```

- Font family, weight, letter spacing, and line height are defined **only** in `theme/Typography.kt`.
- If a Figma style falls between two tokens, use the smaller/more conservative one and note it.

---

## 8. Spacing & Sizing Rules

> **Core Rule:** All padding, size, and gap values reference tokens from `theme/Spacing.kt`. No raw `.dp` literals in composables.

```kotlin
object Spacing {
    val none  = 0.dp;  val xxs = 2.dp;  val xs  = 4.dp;  val sm  = 8.dp
    val md    = 12.dp; val base = 16.dp; val lg  = 20.dp; val xl  = 24.dp
    val xxl   = 32.dp; val xxxl = 40.dp; val giant = 48.dp; val huge = 64.dp
}

object Sizing {
    val iconSm = 16.dp;    val iconMd = 24.dp;   val iconLg = 32.dp
    val avatarSm = 32.dp;  val avatarMd = 40.dp; val avatarLg = 56.dp
    val buttonHeight = 48.dp; val inputHeight = 56.dp
    val cardMinHeight = 80.dp; val dividerThickness = 1.dp
    val touchTarget = 48.dp
}
```

- If Figma uses a value not in the token set → **add it to `Spacing.kt` first**, then reference it.
- Match Figma spacing values exactly. Non-integer values round to nearest even integer (document it).

```kotlin
// ✅
Column(modifier = modifier.padding(horizontal = Spacing.base, vertical = Spacing.sm))

// ❌
Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
```

---

## 9. Corner Radius Rules

> **Core Rule:** All shapes come from `MaterialTheme.shapes.*`. Custom radii live in `theme/Shape.kt` only.

```kotlin
// theme/Shape.kt
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, tooltips, small badges
    small      = RoundedCornerShape(8.dp),   // inputs, small cards
    medium     = RoundedCornerShape(12.dp),  // cards, dialogs
    large      = RoundedCornerShape(16.dp),  // bottom sheets, large cards
    extraLarge = RoundedCornerShape(28.dp),  // FABs, large containers
)
```

- Never write `RoundedCornerShape(12.dp)` inline in a composable.
- Circular shapes always use `CircleShape` — never `RoundedCornerShape(50)` or `percent = 50`.

```kotlin
// ✅
Box(modifier = modifier.size(Sizing.avatarMd).clip(CircleShape))
Card(shape = MaterialTheme.shapes.medium) { }

// ❌
Box(modifier = modifier.clip(RoundedCornerShape(20.dp)))
```

---

## 10. Icon & Image Rules

> **Core Rules:**
> - Always use `Icon` composable for icons — **never** `Image` for icon assets.
> - `contentDescription` for all icons and decorative images is **always `null`**.
> - Implement icons as SVG `ImageVector` from Figma. If the exact SVG is not yet available, use a **single shared placeholder** (`Icons.Default.Image` or a project-wide `PlaceholderIcon`) — never a different placeholder per component.

### ✅ DO

```kotlin
// Icon with always-null contentDescription
Icon(
    imageVector = Icons.Default.Notifications,   // or your SVG ImageVector
    contentDescription = null,                   // ✅ Always null for icons
    tint = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.size(Sizing.iconMd),
)

// Decorative image — null is correct here too
AsyncImage(
    model = imageUrl,
    contentDescription = null,                   // ✅ Decorative; text label is nearby
    modifier = modifier,
)

// Shared placeholder when SVG is not yet available
// Define ONCE in Utilities.kt, use everywhere
val PlaceholderIcon = Icons.Default.Image

Icon(imageVector = PlaceholderIcon, contentDescription = null)
```

### ❌ DO NOT

```kotlin
// ❌ Using Image instead of Icon for icons
Image(painter = painterResource(R.drawable.ic_bell), contentDescription = "Bell")

// ❌ Hardcoded contentDescription
Icon(imageVector = Icons.Default.Star, contentDescription = "Star icon")

// ❌ Different placeholder per component
// ComponentA: Icons.Default.AccountCircle
// ComponentB: Icons.Default.Face
// ComponentC: Icons.Default.Person
```

### SVG Icon Integration

```kotlin
// Implement Figma SVG icons as ImageVector in a dedicated file: ui/theme/Icons.kt
// or ui/utils/AppIcons.kt

object AppIcons {
    val Dashboard = ImageVector.Builder(/* your SVG path data */).build()
    val Analytics = ImageVector.Builder(/* your SVG path data */).build()
    // If SVG not yet extracted from Figma, use the shared placeholder:
    val Placeholder = Icons.Default.Image
}
```

---

## 11. No Magic Numbers / No Hardcoded Values

Any value with semantic meaning must be a named token:

```kotlin
object AnimationTokens {
    val durationShort  = 150   // ms
    val durationMedium = 300
    val durationLong   = 500
}

object ElevationTokens {
    val none = 0.dp; val low = 1.dp; val medium = 4.dp; val high = 8.dp
}

object AlphaTokens {
    val disabled = 0.38f; val medium = 0.60f; val high = 0.87f; val full = 1.00f
}

object GridTokens {
    val columnsCompact = 2; val columnsMedium = 3; val columnsExpanded = 4
}
```

- A number appearing more than once → **must be a token**.
- A number appearing once but with semantic meaning → **must be a named token**.
- Inline numbers are acceptable **only** when mathematically obvious (e.g., `weight(1f)` for equal distribution in a `Row`).

---

## 12. Event Handlers & onClick Rules

> **Core Rule:** All interaction callbacks are **lambda parameters**. Nothing is created or resolved inside the composable body.

```kotlin
// ✅
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
)

// ✅ Multiple zones, named lambdas
@Composable
fun NotificationItem(
    title: String,
    body: String,
    onItemClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- Never navigate, call a ViewModel, or emit side effects inside a component composable.
- Optional callbacks → nullable lambda with `null` default.
- No `LocalContext.current`, `LocalNavController.current`, or similar inside a component composable.

---

## 13. State Management Rules

- **Stateless by default.** Components own state only for purely internal UI concerns (e.g., expanded/collapsed with no external meaning).
- Use **state hoisting** — lift all meaningful state to screen or ViewModel.
- Internal state must not leak unless exposed via an `onXxx` callback.
- Never use `rememberCoroutineScope` in a component composable for business logic. Only for UI animation/scroll.

```kotlin
// ✅
@Composable
fun ExpandableCard(
    title: String,
    content: String,
    onExpandChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    // ...
}
```

---

## 14. List Item Component Rules

> **Core Rule:** If a composable is an item in a list, **every potentially dynamic property must be a parameter**. Static display strings belong in `strings.xml`, not hardcoded in the composable.

### Dynamic Parameters (always as function parameters):
- All data fields (title, subtitle, value, count, imageUrl, etc.)
- All state flags (`isSelected`, `isRead`, `isExpanded`, `hasError`, etc.)
- All interaction callbacks (`onClick`, `onLongClick`, `onSwipe`, etc.)
- Any color/icon that varies per item

### Static Strings → `strings.xml`:
- Labels that never change per item (e.g., section headers, fixed button labels, units)
- Accessibility strings for decorative structure (though icons use `null`)

```kotlin
// ✅
@Composable
fun TransactionItem(
    senderName: String,
    amount: String,
    currencyCode: String,
    timestamp: String,
    isRead: Boolean,
    statusIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)

// Inside composable — fixed label from strings.xml
Text(text = stringResource(R.string.label_sent_to))

// ❌ Hardcoded string for a dynamic or reusable label
Text(text = "Sent to")
```

---

## 15. Component Placement Rules

| Situation | Where it goes |
|---|---|
| Can be used across multiple screens | `ui/components/<category>/` |
| Tightly coupled to dashboard data/design/logic | `ui/screens/dashboard/components/` |
| Reusable modifier / helper function | `ui/utils/Utilities.kt` |
| Reusable icon definitions | `ui/theme/AppIcons.kt` or `ui/utils/AppIcons.kt` |

- Dashboard-specific components should be **fully customized** to match the Figma dashboard design — do not water them down with excess abstraction.
- Generic components expose parameters for all customizable aspects so they can adapt across screens.

---

## 16. Figma Implementation Rules

> **Core Rule:** Implement what is in Figma — exactly. Every visible detail is intentional.

### What to check from Figma:

- **Shadows:** If Figma shows an elevation/drop shadow, implement it using `ElevationTokens.*` or a named `shadowElevation` token. Never ignore a shadow.
- **Light vs. Dark mode:** If Figma provides separate light and dark frames for a component, compare them pixel-by-pixel. Any color, opacity, border, or shadow difference between modes **must** be implemented. Use `isSystemInDarkTheme()` at screen level or provide both color values in `AppColors` (light + dark).
- **Colors not in `AppColors`:** If a Figma color does not exist in the current theme, **add it to `AppColors` in `theme/Color.kt`** with a semantic name (light + dark variant) before writing any composable code. Never paste the hex inline.
- **Spacing:** Use exact Figma values. Add missing values to `Spacing.kt` before using.
- **Corner radii:** Add missing values to `Shape.kt` before using.
- **Icons:** Extract as SVG `ImageVector`. If not yet available, use the shared `AppIcons.Placeholder`.
- **Opacity/alpha:** Never hardcode — add to `AlphaTokens` if missing.
- **Typography:** Map to the nearest Material 3 token; document any deviation.

```kotlin
// ✅ Figma shows a custom teal color not in AppColors
// Step 1 — Add to theme/Color.kt
object AppColors {
    val StatusTeal      = Color(0xFF00897B)  // Light
    val StatusTealDark  = Color(0xFF4DB6AC)  // Dark — from Figma dark frame
}

// Step 2 — Use in composable
containerColor = AppColors.StatusTeal
// Comment if it differs between modes:
// Light: AppColors.StatusTeal / Dark: AppColors.StatusTealDark — see Figma "Status Badge – Dark"
```

---

## 17. Utilities & Reusable Helpers

> Secondary composables, modifier extensions, and helpers that are not the primary component must live in a Utilities file.

| Type | Example |
|---|---|
| Modifier extensions | `fun Modifier.cardShadow(): Modifier` |
| Sub-composables | `@Composable fun BadgeOverlay(count: Int)` |
| Formatting helpers | `fun formatPrice(amount: Double, currency: String): String` |
| Shared placeholder icon | `val PlaceholderIcon = Icons.Default.Image` |
| Shimmer / loading | `@Composable fun ShimmerBox(modifier: Modifier)` |
| Icon mapping | `fun categoryToIcon(category: String): ImageVector` |

Group with region comments:

```kotlin
// region ─── Modifier Extensions ──────────────────────────────────────
// endregion

// region ─── Sub-Composables ───────────────────────────────────────────
// endregion

// region ─── Formatting & Mapping Helpers ──────────────────────────────
// endregion
```

---

## 18. Preview Rules

- Every component **must** have at least one `@Preview`.
- Cover: default, disabled/empty, dark theme, significantly different variants.
- Always wrap in `AppTheme { }`.
- Named: `Preview<ComponentName><Variant>`.

```kotlin
@Preview(showBackground = true, name = "Default")
@Composable
private fun PreviewPrimaryButton() {
    AppTheme { PrimaryButton(label = "Confirm Order", onClick = {}) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun PreviewPrimaryButtonDark() {
    AppTheme(darkTheme = true) { PrimaryButton(label = "Confirm Order", onClick = {}) }
}

@Preview(showBackground = true, name = "Disabled")
@Composable
private fun PreviewPrimaryButtonDisabled() {
    AppTheme { PrimaryButton(label = "Confirm Order", onClick = {}, isEnabled = false) }
}
```

---

## 19. Accessibility Rules

- `contentDescription` for **all icons and decorative images is always `null`**. This is non-negotiable.
- Interactive elements that are icon-only (no visible label) must still expose meaning through `Modifier.semantics { contentDescription = ... }` at the component level if required by screen-reader UX — passed as a parameter, never hardcoded.
- Minimum touch target: `48.dp × 48.dp` via `Modifier.minimumInteractiveComponentSize()` or `Sizing.touchTarget`.
- Text contrast must always come from the paired `onXxx` color in the Material color scheme.
- Use `Modifier.semantics { }` to improve TalkBack for complex components.

---

## 20. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Composable functions | `UpperCamelCase` | `ProductCard`, `SearchBar` |
| Preview functions | `Preview<Name><Variant>` | `PreviewProductCardDark` |
| Parameters | `lowerCamelCase` | `onCardClick`, `isLoading` |
| Data classes for params | `<Component>Data` / `<Component>State` | `UserCardData` |
| Lambda params | `on<Action>` | `onClick`, `onDismiss` |
| Boolean params | `is<State>` / `has<Property>` | `isEnabled`, `hasError` |
| Spacing tokens | Descriptive size name | `Spacing.base`, `Spacing.xl` |
| Custom color tokens | Semantic, not visual | `StatusTeal`, not `GreenColor` |
| Shape tokens | Match Material naming | `MaterialTheme.shapes.medium` |
| App icon object | `AppIcons` | `AppIcons.Dashboard` |

---

## 21. Import & Dependency Rules

- Never import `androidx.compose.ui.graphics.Color` to create inline colors. Use `MaterialTheme` or `AppColors`.
- No wildcard imports (`import androidx.compose.*`). Be explicit.
- Third-party image loaders (Coil, Glide) are abstracted behind a utility composable (`RemoteImage`, `AvatarImage`).
- New `build.gradle` dependencies must have a comment documenting the reason.

---

## 22. Checklist Before Committing

- [ ] Parent composable is at the **top** of the file; sub-composables below it; constants/data classes at the **bottom**
- [ ] All data values arrive via parameters — no internal `val` for display data
- [ ] All click/interaction handlers are lambda parameters prefixed with `on`
- [ ] No raw `Color(0xFFxxxxxx)` inline — all colors are in `AppColors` or `MaterialTheme.colorScheme`
- [ ] Figma colors not in the theme were added to `AppColors` (with light + dark variants) before use
- [ ] No raw `.dp` literals — all dimensions use `Spacing.*` or `Sizing.*`
- [ ] No raw `.sp` or inline font weight — all text uses `MaterialTheme.typography.*`
- [ ] No raw corner radius inline — all shapes via `MaterialTheme.shapes.*` or `Shape.kt`
- [ ] No magic numbers or hardcoded strings
- [ ] `modifier: Modifier = Modifier` is the last non-lambda parameter
- [ ] Icons use `Icon` composable — not `Image`
- [ ] `contentDescription = null` on all icons and decorative images
- [ ] SVG icons from Figma implemented as `ImageVector`; missing ones use the shared `PlaceholderIcon`
- [ ] Figma shadows, opacities, and light/dark differences are all implemented
- [ ] List item components expose all dynamic fields as parameters; static strings are in `strings.xml`
- [ ] Generic component → `ui/components/`; dashboard-specific → `ui/screens/dashboard/components/`
- [ ] Secondary/utility composables are in `Utilities.kt`
- [ ] At least one `@Preview` exists, wrapped in `AppTheme { }`
- [ ] Minimum touch target of `48.dp` enforced for all clickable elements
- [ ] File name matches the primary composable name
- [ ] No business logic, navigation, or ViewModel access inside a component composable

---

*Last updated: 2026 — When in doubt: extract it, name it, token-ize it, and put it in the right package.*
