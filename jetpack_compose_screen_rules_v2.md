# Jetpack Compose Screen Architecture Rules v2

> A strict, practical ruleset for building Compose screens that are predictable, previewable, lifecycle-safe, testable, and cheap to recompose.

---

## Table of Contents

1. [Core Principles](#1-core-principles)
2. [File and Package Structure](#2-file-and-package-structure)
3. [Screen Contract](#3-screen-contract)
4. [UiState Rules](#4-uistate-rules)
5. [Model Rules](#5-model-rules)
6. [Action and Effect Rules](#6-action-and-effect-rules)
7. [Screen Composable Rules](#7-screen-composable-rules)
8. [ViewModel Rules](#8-viewmodel-rules)
9. [Message and Dialog Rules](#9-message-and-dialog-rules)
10. [Component Rules](#10-component-rules)
11. [Performance Rules](#11-performance-rules)
12. [Preview Rules](#12-preview-rules)
13. [Accessibility and Resources](#13-accessibility-and-resources)
14. [AI Agent Wiring Rules](#14-ai-agent-wiring-rules)
15. [Reference Example](#15-reference-example)
16. [PR Checklist](#16-pr-checklist)

---

## 1. Core Principles

Every screen follows the same mental model:

- `UiState` is persistent screen state. It describes what is visible right now.
- `Action` is user intent from the UI to the ViewModel.
- `Effect` is a one-time instruction from the ViewModel to the UI.
- The ViewModel owns state changes and business decisions.
- The screen composable wires state, effects, navigation, and top-level UI.
- Child composables are stateless, small, stable, and previewable.

Hard rule: do not put one-time work in `UiState`. Navigation, snackbars, message bars, bottom sheets, permission requests, share sheets, browser launches, and focus commands are effects.

---

## 2. File and Package Structure

Each screen must live in its own package.

```text
feature/
+-- home/
    +-- HomeScreen.kt       # Route composable + content composables
    +-- HomeUiState.kt      # UiState + models + Action + Effect + preview data
    +-- HomeViewModel.kt    # State updates + business logic + effect emission
```

Optional files are allowed when the screen becomes large:

```text
feature/home/components/
feature/home/HomePreviewData.kt
```

Rules:

- One screen package owns one screen contract.
- Keep `UiState`, screen models, `Action`, `Effect`, and preview fixtures in `HomeUiState.kt` unless the file becomes too large.
- Keep reusable design-system components outside feature packages.
- Keep screen-only components inside the screen package or `components` subpackage.
- No business logic in composables.
- No Compose UI objects in ViewModels.
- No `Context`, `Resources`, `NavController`, or `SnackbarHostState` in ViewModels.

---

## 3. Screen Contract

Each screen must define exactly one `UiState`, one `Action`, and one `Effect`.

```kotlin
@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val posts: List<PostModel> = emptyList(),
    val searchQuery: String = "",
)

sealed interface HomeAction {
    data object OnRetryClicked : HomeAction
    data object OnRefreshTriggered : HomeAction
    data class OnPostClicked(val postId: String) : HomeAction
    data class OnSearchQueryChanged(val query: String) : HomeAction
}

sealed interface HomeEffect {
    data object NavigateBack : HomeEffect
    data class NavigateToDetail(val postId: String) : HomeEffect
    data class ShowMessage(val message: UiText, val type: MessageType = MessageType.Info) : HomeEffect
}
```

Rules:

- Name the sealed types `<Feature>Action` and `<Feature>Effect`.
- Prefer `Effect` over `Event`. In Compose, "event" is often used for UI input, so `Effect` keeps the direction clear.
- Do not define generic actions such as `OnClick` or `OnChanged`. Name the intent: `OnSaveClicked`, `OnPhoneChanged`, `OnRetryClicked`.
- Do not expose repository/domain models directly in `UiState`. Map them to screen models.

---

## 4. UiState Rules

### 4.1 Persistent State Only

`UiState` contains values that should survive recomposition and describe the current screen.

Good `UiState` fields:

- Loading flags
- Screen data
- Empty-state flags
- Selected tab/filter/sort
- Text field values
- Validation errors shown inline
- Pagination flags
- Permission state already known by the screen

Bad `UiState` fields:

- Navigation commands
- Toast/snackbar/message-bar commands
- Bottom-sheet open commands caused by one click
- Dialog open commands caused by one click
- Browser/share/permission launcher commands
- Anything that must happen exactly once

### 4.2 Required Loading and Error Shape

Use explicit state for content loading. Avoid a single vague `error` string for the whole screen.

```kotlin
@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: UiText = UiText.Empty,
    val posts: List<PostModel> = emptyList(),
)
```

Rules:

- `isLoading` is for first load or blocking full-screen loading.
- `isRefreshing` is for pull-to-refresh or non-blocking reload.
- `contentError` is for persistent screen errors shown in the content area.
- Transient errors go through `HomeEffect.ShowMessage`.
- Do not keep a generic `message: String` in `UiState` for snackbars/message bars.

### 4.3 UiText

Define `UiText` once in a shared UI module.

```kotlin
import android.content.Context

sealed interface UiText {
    data object Empty : UiText
    data class DynamicString(val value: String) : UiText
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}
```

Resolve it only in the UI layer:

```kotlin
@Composable
fun UiText.asString(): String {
    return when (this) {
        UiText.Empty -> ""
        is UiText.DynamicString -> value
        is UiText.StringResource -> stringResource(resId, *args.toTypedArray())
    }
}

fun UiText.asString(context: Context): String {
    return when (this) {
        UiText.Empty -> ""
        is UiText.DynamicString -> value
        is UiText.StringResource -> context.getString(resId, *args.toTypedArray())
    }
}
```

Rules:

- ViewModels may create `UiText.StringResource`.
- ViewModels must not call `stringResource`, `context.getString`, or `resources.getString`.
- Prefer `StringResource` for user-facing text.
- Use `DynamicString` for server-provided text, debug messages, or already-localized content.

### 4.4 Stability

Rules:

- Annotate screen `UiState` and UI models with `@Immutable` when every property is immutable.
- Use `List<T>`, not `MutableList<T>`.
- Use `Set<T>` and `Map<K, V>` only when they are read-only.
- Do not store lambdas, `Flow`, `State`, `MutableState`, `CoroutineScope`, or Compose runtime objects in `UiState`.
- Do not store large bitmaps or Android views in `UiState`; store stable IDs, URLs, resource IDs, or lightweight models.

---

## 5. Model Rules

### 5.1 Non-Null UI Models

Screen models must be non-null and have safe defaults.

```kotlin
// Bad
data class PostModel(
    val id: String? = null,
    val title: String? = null,
)

// Good
@Immutable
data class PostModel(
    val id: String = "",
    val title: String = "",
    val body: String = "",
)
```

Default values:

| Type | Default |
| --- | --- |
| `String` | `""` |
| `Int` | `0` or `-1` when `0` is meaningful |
| `Long` | `0L` or `-1L` when `0L` is meaningful |
| `Float` / `Double` | `0f` / `0.0` or sentinel when needed |
| `Boolean` | `false` |
| `List<T>` | `emptyList()` |
| `Set<T>` | `emptySet()` |
| `Map<K, V>` | `emptyMap()` |
| `Enum` | `NONE` or `UNKNOWN` first entry |
| Nested model | `ModelName()` |

### 5.2 IDs for Lists

Every model rendered in a lazy list must have a stable ID.

```kotlin
@Immutable
data class PostModel(
    val id: String = "",
    val title: String = "",
)
```

Rules:

- Use real backend IDs when available.
- If there is no backend ID, create a stable UI ID during mapping.
- Never use list index as a lazy-list key unless the list is static and never reordered.

### 5.3 Domain to UI Mapping

Map domain/data models before they enter `UiState`.

```kotlin
private fun Post.toUiModel(): PostModel {
    return PostModel(
        id = id,
        title = title.orEmpty(),
        body = body.orEmpty(),
    )
}
```

Rules:

- Null cleanup happens in mappers, not in composables.
- Formatting for display may happen in mappers when it is not locale-sensitive.
- Locale-sensitive formatting should use injected formatters or UI-layer resources, not raw `Context` in ViewModels.

---

## 6. Action and Effect Rules

### 6.1 Actions

Actions represent user intent.

```kotlin
sealed interface HomeAction {
    data object OnRetryClicked : HomeAction
    data object OnRefreshTriggered : HomeAction
    data class OnPostClicked(val postId: String) : HomeAction
    data class OnSearchQueryChanged(val query: String) : HomeAction
}
```

Rules:

- One action per meaningful user intent.
- Use `data object` for actions with no parameters.
- Use `data class` for actions with parameters.
- Actions are pure data. They contain no logic.
- Do not create actions for internal ViewModel work.

### 6.2 Effects

Effects represent one-time UI work.

```kotlin
sealed interface HomeEffect {
    data object NavigateBack : HomeEffect
    data class NavigateToDetail(val postId: String) : HomeEffect
    data class ShowMessage(val message: UiText, val type: MessageType) : HomeEffect
    data class OpenUrl(val url: String) : HomeEffect
}
```

Rules:

- Effects are emitted once and consumed once.
- Effects are never stored in `UiState`.
- Navigation always goes through an effect.
- Message bars, snackbars, dialogs, launchers, and external intents always go through effects.
- Handle every effect variant exhaustively. Do not use `else` in `when(effect)`.

---

## 7. Screen Composable Rules

### 7.1 Route and Content Split

Every screen should have a route composable and a content composable.

```kotlin
@Composable
fun HomeRoute(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            HomeEffect.NavigateBack -> navController.popBackStack()
            is HomeEffect.NavigateToDetail -> navController.navigate(Route.Detail(effect.postId))
            is HomeEffect.ShowMessage -> snackbarHostState.showSnackbar(
                message = effect.message.asString(context),
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HomeScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stateless UI layout
}
```

Rules:

- The route composable may know about `NavController` and `ViewModel`.
- The content composable must not know about `NavController`, `ViewModel`, Hilt, or repositories.
- Previews call content composables, never route composables.
- If the project already uses `<Feature>Screen` as the route name, use `<Feature>Content` for the previewable function.

### 7.2 State Collection

Always collect ViewModel state with lifecycle awareness.

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

Rules:

- Do not use `collectAsState()` for ViewModel flows in Android screens.
- Do not collect ViewModel flows inside child components.
- Pass only the state slices each child needs.

### 7.3 Effect Collection

Define this utility once in a shared UI module.

```kotlin
@Composable
fun <T> CollectEffect(
    effect: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effect, lifecycleOwner, lifecycleState) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            effect.collect { currentOnEffect(it) }
        }
    }
}
```

Usage:

```kotlin
val context = LocalContext.current

CollectEffect(effect = viewModel.effect) { effect ->
    when (effect) {
        HomeEffect.NavigateBack -> navController.popBackStack()
        is HomeEffect.NavigateToDetail -> navController.navigate(Route.Detail(effect.postId))
        is HomeEffect.ShowMessage -> messageBarState.show(
            message = effect.message.asString(context),
            type = effect.type,
        )
        is HomeEffect.OpenUrl -> openUrl(effect.url)
    }
}
```

Rules:

- Effect collection belongs in the route/top-level screen only.
- Use one collector per effect flow.
- Do not use a raw `LaunchedEffect(Unit)` to collect ViewModel effects.
- Do not replay navigation or messages after configuration change.

### 7.4 Root Layout

Use `Scaffold` for screens that need app bars, FABs, snackbar/message hosts, or insets.

```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { padding ->
    HomeContent(
        state = state,
        onAction = onAction,
        modifier = Modifier.padding(padding),
    )
}
```

Rules:

- Respect `padding` from `Scaffold`.
- Do not ignore system bars or IME insets.
- Do not nest a second `Scaffold` unless there is a strong reason.
- Simple full-screen surfaces may use `Box`/`Column` instead of `Scaffold` only when no scaffold slots or insets are needed.

---

## 8. ViewModel Rules

### 8.1 State and Effect Declarations

```kotlin
private val _state = MutableStateFlow(HomeUiState())
val state: StateFlow<HomeUiState> = _state.asStateFlow()

private val _effect = MutableSharedFlow<HomeEffect>(
    replay = 0,
    extraBufferCapacity = 1,
)
val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()
```

Rules:

- Mutable flows are private.
- Public flows are read-only.
- `state` is a `StateFlow`.
- `effect` is a `SharedFlow`.
- `replay = 0` for effects.
- `extraBufferCapacity = 1` prevents quick effects from suspending when the collector is briefly not ready.

### 8.2 Action Entry Point

```kotlin
fun onAction(action: HomeAction) {
    when (action) {
        HomeAction.OnRetryClicked -> loadPosts()
        HomeAction.OnRefreshTriggered -> refreshPosts()
        is HomeAction.OnPostClicked -> openPost(action.postId)
        is HomeAction.OnSearchQueryChanged -> updateSearch(action.query)
    }
}
```

Rules:

- Use one public `onAction` entry point for UI actions.
- Keep other public functions only when framework integration requires them.
- Make helper functions private.
- Use exhaustive `when`; no `else` branch.

### 8.3 Initial Loading

```kotlin
init {
    loadPosts()
}
```

Rules:

- Data required on first screen open is started from `init`.
- UI composables must not trigger first-load work.
- Use `SavedStateHandle` for required navigation arguments.

### 8.4 Updating State

```kotlin
_state.update { current ->
    current.copy(isLoading = true, contentError = UiText.Empty)
}
```

Rules:

- Prefer `_state.update { }` over `_state.value =`.
- Never mutate collections inside the current state.
- Create new lists when changing list items.
- Clear persistent errors when a new load starts.

### 8.5 Emitting Effects

```kotlin
private fun openPost(postId: String) {
    viewModelScope.launch {
        _effect.emit(HomeEffect.NavigateToDetail(postId))
    }
}
```

Rules:

- Emit effects from `viewModelScope.launch`.
- Use `emit` when order matters.
- Use `tryEmit` only for non-critical effects where dropping is acceptable.
- Do not store a pending effect in state to work around lifecycle issues.

### 8.6 Error Handling

```kotlin
private fun loadPosts() {
    viewModelScope.launch {
        _state.update { it.copy(isLoading = true, contentError = UiText.Empty) }

        repository.getPosts()
            .onSuccess { posts ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        posts = posts.map(Post::toUiModel),
                    )
                }
            }
            .onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        contentError = UiText.StringResource(R.string.error_posts_load_failed),
                    )
                }
                _effect.emit(
                    HomeEffect.ShowMessage(
                        message = UiText.StringResource(R.string.error_posts_load_failed),
                        type = MessageType.Error,
                    )
                )
            }
    }
}
```

Rules:

- Persistent errors that change content go in `UiState`.
- Transient error notifications go through `Effect`.
- Do not expose raw exception messages to users unless they are intentionally user-facing.
- Log unexpected exceptions through the project's logging mechanism, not through UI state.

---

## 9. Message and Dialog Rules

Preferred rule: ViewModels emit message/dialog effects; composables own UI host state.

```kotlin
sealed interface HomeEffect {
    data class ShowMessage(val message: UiText, val type: MessageType) : HomeEffect
    data object ShowDeleteConfirmation : HomeEffect
}
```

```kotlin
val context = LocalContext.current

CollectEffect(effect = viewModel.effect) { effect ->
    when (effect) {
        is HomeEffect.ShowMessage -> messageBarState.show(
            message = effect.message.asString(context),
            type = effect.type,
        )
        HomeEffect.ShowDeleteConfirmation -> showDeleteDialog = true
    }
}
```

Rules:

- The ViewModel must not attach, store, or receive `MessageBarState`, `SnackbarHostState`, `SheetState`, `FocusRequester`, or dialog state.
- The UI layer owns Compose runtime state.
- Effects tell the UI what to do; they do not carry UI objects.
- Dialog visibility that is part of ongoing screen state may be a `UiState` field. Dialogs caused by one-time commands should be effects.

Acceptable exception:

- If the existing codebase already has a message manager abstraction, the manager must not store Compose state in the ViewModel. Prefer a manager that exposes a `SharedFlow<MessageCommand>` collected by the UI.

---

## 10. Component Rules

### 10.1 Stateless Components

Bad:

```kotlin
@Composable
fun PostCard(state: HomeUiState) {
    Text(state.posts.first().title)
}
```

Good:

```kotlin
@Composable
fun PostCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // UI only
}
```

Rules:

- Components receive only the data they render.
- Components do not receive full screen `UiState` unless they are screen-level content components.
- Components do not receive ViewModels.
- Components do not start business work.
- Nullable parameters are not allowed unless absence is truly meaningful.

### 10.2 Modifier Position

Every reusable composable accepts `modifier` as the last parameter with a default.

```kotlin
@Composable
fun UserAvatar(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
)
```

### 10.3 Events from Components

Pass callbacks from parent to child.

```kotlin
PostCard(
    title = post.title,
    body = post.body,
    onClick = { onAction(HomeAction.OnPostClicked(post.id)) },
)
```

Rules:

- Child components emit callbacks, not screen actions, when they are reusable.
- Screen-specific components may use `onAction: (HomeAction) -> Unit`.
- Do not pass multiple callbacks when one typed action would be clearer at the screen-content level.

---

## 11. Performance Rules

### 11.1 Lazy Lists

```kotlin
LazyColumn {
    items(
        items = state.posts,
        key = { it.id },
        contentType = { "post" },
    ) { post ->
        PostCard(
            title = post.title,
            body = post.body,
            onClick = { onAction(HomeAction.OnPostClicked(post.id)) },
        )
    }
}
```

Rules:

- Always provide stable `key`.
- Provide `contentType` when a lazy list has mixed item layouts.
- Do not use list index as key for dynamic data.
- Keep expensive item formatting outside lazy item lambdas when possible.

### 11.2 Derived Values

Use `derivedStateOf` only for derived values that are expensive or change less often than their inputs during recomposition.

```kotlin
val filteredPosts by remember(state.posts, state.searchQuery) {
    derivedStateOf {
        state.posts.filter { post ->
            post.title.contains(state.searchQuery, ignoreCase = true)
        }
    }
}
```

Rules:

- Do not wrap every computed value in `derivedStateOf`.
- Prefer ViewModel/domain filtering for large lists.
- Keep UI-only cheap derivations in composables.

### 11.3 Remember Correctly

Rules:

- Use `remember` for values that are expensive to create and safe to keep.
- Use `rememberSaveable` for UI input state that must survive process recreation when it is not owned by the ViewModel.
- Include all changing inputs in `remember` keys.
- Use `rememberUpdatedState` when a long-lived effect needs the latest lambda or value.

### 11.4 Avoid Unnecessary Recomposition

Rules:

- Pass stable models or primitive slices to children.
- Avoid creating new lambdas in tight lazy-list loops when they capture many changing values.
- Avoid sorting/filtering large lists directly in composable bodies.
- Avoid mutable collections in state.
- Do not read frequently changing state at a high level if only one child needs it.
- Use `snapshotFlow` only when bridging Compose state to Flow is necessary.

### 11.5 Side Effects

Rules:

- Use `LaunchedEffect(key)` for suspend work owned by the composition.
- Use `DisposableEffect(key)` for registering/unregistering listeners.
- Use `SideEffect` only to publish Compose state to non-Compose code after successful recomposition.
- Do not launch coroutines directly in the composable body.
- Do not use `GlobalScope`.

---

## 12. Preview Rules

Every screen must have realistic previews for content states.

```kotlin
@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val posts: List<PostModel> = emptyList(),
) {
    companion object {
        val preview = HomeUiState(
            posts = listOf(
                PostModel(
                    id = "post_1",
                    title = "Designing resilient Compose screens",
                    body = "A practical guide to state, effects, and previews.",
                ),
            ),
        )

        val previewLoading = HomeUiState(isLoading = true)
        val previewEmpty = HomeUiState(posts = emptyList())
        val previewError = HomeUiState(
            contentError = UiText.StringResource(R.string.error_posts_load_failed),
        )
    }
}
```

Rules:

- Previews call content composables, not route composables.
- Provide light and dark previews.
- Provide loading, empty, and error previews when the screen supports those states.
- Use realistic data, not `"test"`, `"abc"`, or `123`.
- Preview data may use hardcoded display text.
- Do not require Hilt, navigation, network, database, or ViewModel objects in previews.

---

## 13. Accessibility and Resources

Rules:

- User-visible strings come from `strings.xml`, except preview fixture text.
- Colors come from `MaterialTheme.colorScheme` or approved design tokens.
- Typography comes from `MaterialTheme.typography` or approved design tokens.
- Spacing comes from project dimensions/tokens when the project has them.
- Interactive icons must have meaningful `contentDescription`.
- Decorative images/icons use `contentDescription = null`.
- Touch targets should be at least `48.dp`.
- Do not rely on color alone to communicate state.
- Support font scaling; avoid fixed-height containers around text unless overflow is handled.

Bad:

```kotlin
Icon(Icons.Default.Delete, contentDescription = null)
Text("Delete")
```

Good:

```kotlin
Icon(
    imageVector = Icons.Default.Delete,
    contentDescription = stringResource(R.string.cd_delete),
)
Text(text = stringResource(R.string.delete))
```

---

## 14. AI Agent Wiring Rules

When generating or editing a screen, follow this order.

### Rule A: Build the Contract First

Before writing UI, define:

- `<Feature>UiState`
- UI models
- `<Feature>Action`
- `<Feature>Effect`
- Preview fixtures

### Rule B: Wire Every User Interaction

For every clickable/input UI element:

1. Add one named `Action`.
2. Call `onAction(...)` from the composable.
3. Handle the action in `ViewModel.onAction`.
4. Update state or emit an effect.

### Rule C: Wire Every One-Time UI Command as an Effect

Use effects for:

- Navigation
- Snackbars/message bars
- Toasts
- Dialogs and bottom sheets opened by commands
- Permission launchers
- Browser/share/file pickers

### Rule D: Keep Effects Out of State

Never add fields such as:

```kotlin
val navigateToDetail: Boolean = false
val showToast: Boolean = false
val snackbarMessage: String = ""
```

Use:

```kotlin
data class NavigateToDetail(val postId: String) : HomeEffect
data class ShowMessage(val message: UiText, val type: MessageType) : HomeEffect
```

### Rule E: Model Completeness Check

Before finalizing `UiState`:

1. List every component in the screen.
2. List every value each component displays.
3. Ensure every value is represented by a non-null UI model field.
4. Ensure every lazy-list model has a stable `id`.
5. Ensure every conditional UI branch has an explicit boolean or enum state.

### Rule F: Preview Completeness Check

Before finalizing previews:

- Content preview exists.
- Loading preview exists if loading UI exists.
- Empty preview exists if empty UI exists.
- Error preview exists if error UI exists.
- Light and dark modes are covered.
- Preview does not require ViewModel, Hilt, NavController, network, or database.

---

## 15. Reference Example

### `HomeUiState.kt`

```kotlin
package com.example.app.feature.home

import androidx.compose.runtime.Immutable

enum class MessageType {
    Info,
    Success,
    Error,
}

enum class PostCategory {
    NONE,
    TECH,
    DESIGN,
    BUSINESS,
}

@Immutable
data class PostModel(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val author: String = "",
    val category: PostCategory = PostCategory.NONE,
    val likesCount: Int = 0,
    val isBookmarked: Boolean = false,
)

@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: UiText = UiText.Empty,
    val posts: List<PostModel> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: PostCategory = PostCategory.NONE,
) {
    companion object {
        val preview = HomeUiState(
            posts = listOf(
                PostModel(
                    id = "post_001",
                    title = "Mastering Jetpack Compose",
                    body = "A practical guide to state, effects, and previews.",
                    author = "Maria Johnson",
                    category = PostCategory.TECH,
                    likesCount = 342,
                    isBookmarked = true,
                ),
                PostModel(
                    id = "post_002",
                    title = "Design Systems at Scale",
                    body = "How teams keep UI consistent across features.",
                    author = "Carlos Rivera",
                    category = PostCategory.DESIGN,
                    likesCount = 128,
                ),
            ),
        )

        val previewLoading = HomeUiState(isLoading = true)
        val previewEmpty = HomeUiState(posts = emptyList())
        val previewError = HomeUiState(
            contentError = UiText.StringResource(R.string.error_posts_load_failed),
        )
    }
}

sealed interface HomeAction {
    data object OnRetryClicked : HomeAction
    data object OnRefreshTriggered : HomeAction
    data object OnBackClicked : HomeAction
    data class OnPostClicked(val postId: String) : HomeAction
    data class OnBookmarkClicked(val postId: String) : HomeAction
    data class OnSearchQueryChanged(val query: String) : HomeAction
    data class OnCategorySelected(val category: PostCategory) : HomeAction
}

sealed interface HomeEffect {
    data object NavigateBack : HomeEffect
    data class NavigateToDetail(val postId: String) : HomeEffect
    data class ShowMessage(
        val message: UiText,
        val type: MessageType = MessageType.Info,
    ) : HomeEffect
}
```

### `HomeScreen.kt`

```kotlin
package com.example.app.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun HomeRoute(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            HomeEffect.NavigateBack -> navController.popBackStack()
            is HomeEffect.NavigateToDetail -> navController.navigate(Route.Detail(effect.postId))
            is HomeEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message.asString(context))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HomeContent(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> HomeLoadingContent(modifier = modifier)
        state.contentError != UiText.Empty -> HomeErrorContent(
            message = state.contentError.asString(),
            onRetryClick = { onAction(HomeAction.OnRetryClicked) },
            modifier = modifier,
        )
        state.posts.isEmpty() -> HomeEmptyContent(modifier = modifier)
        else -> HomePostList(
            posts = state.posts,
            onPostClick = { postId -> onAction(HomeAction.OnPostClicked(postId)) },
            onBookmarkClick = { postId -> onAction(HomeAction.OnBookmarkClicked(postId)) },
            modifier = modifier,
        )
    }
}
```

### `HomeViewModel.kt`

```kotlin
package com.example.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PostRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        loadPosts()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRetryClicked -> loadPosts()
            HomeAction.OnRefreshTriggered -> refreshPosts()
            HomeAction.OnBackClicked -> navigateBack()
            is HomeAction.OnPostClicked -> openPost(action.postId)
            is HomeAction.OnBookmarkClicked -> toggleBookmark(action.postId)
            is HomeAction.OnSearchQueryChanged -> updateSearch(action.query)
            is HomeAction.OnCategorySelected -> updateCategory(action.category)
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    contentError = UiText.Empty,
                )
            }

            repository.getPosts()
                .onSuccess { posts ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            posts = posts.map(Post::toUiModel),
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = UiText.StringResource(R.string.error_posts_load_failed),
                        )
                    }
                    _effect.emit(
                        HomeEffect.ShowMessage(
                            message = UiText.StringResource(R.string.error_posts_load_failed),
                            type = MessageType.Error,
                        )
                    )
                }
        }
    }

    private fun refreshPosts() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.getPosts()
                .onSuccess { posts ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            posts = posts.map(Post::toUiModel),
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(
                        HomeEffect.ShowMessage(
                            message = UiText.StringResource(R.string.error_posts_refresh_failed),
                            type = MessageType.Error,
                        )
                    )
                }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateBack)
        }
    }

    private fun openPost(postId: String) {
        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToDetail(postId))
        }
    }

    private fun toggleBookmark(postId: String) {
        _state.update { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == postId) {
                        post.copy(isBookmarked = !post.isBookmarked)
                    } else {
                        post
                    }
                },
            )
        }
    }

    private fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    private fun updateCategory(category: PostCategory) {
        _state.update { it.copy(selectedCategory = category) }
    }
}
```

---

## 16. PR Checklist

Use this before review.

State and models:

- [ ] `UiState` contains persistent screen state only.
- [ ] No navigation, snackbar, message-bar, dialog command, or launcher command is stored in `UiState`.
- [ ] All UI model fields are non-null with safe defaults.
- [ ] All lazy-list models have stable `id`.
- [ ] `@Immutable` is used where the model is truly immutable.
- [ ] No mutable collections are stored in state.

Actions and effects:

- [ ] Exactly one `<Feature>Action` sealed type exists.
- [ ] Exactly one `<Feature>Effect` sealed type exists.
- [ ] Actions represent user intent.
- [ ] Effects represent one-time UI work.
- [ ] `when(action)` and `when(effect)` are exhaustive with no `else`.

ViewModel:

- [ ] Mutable flows are private; public flows are read-only.
- [ ] State uses `MutableStateFlow` and `StateFlow`.
- [ ] Effects use `MutableSharedFlow(replay = 0)`.
- [ ] State updates use `_state.update { }`.
- [ ] Initial data loading starts from `init` when needed.
- [ ] No `Context`, `Resources`, `NavController`, or Compose state is stored in the ViewModel.
- [ ] Errors use persistent state for content errors and effects for transient messages.

Screen:

- [ ] Route composable owns ViewModel, navigation, and effect collection.
- [ ] Content composable is previewable and does not know about ViewModel or NavController.
- [ ] ViewModel state is collected with `collectAsStateWithLifecycle()`.
- [ ] Effects are collected with the shared lifecycle-aware effect collector.
- [ ] `Scaffold` padding is applied to content when `Scaffold` is used.

Components:

- [ ] Reusable components receive only the data they render.
- [ ] `modifier: Modifier = Modifier` is the last parameter.
- [ ] No child component receives a ViewModel.
- [ ] Lazy lists have `key` and `contentType` where useful.
- [ ] Expensive sorting/filtering is not done directly in composable bodies.

Previews:

- [ ] Content previews exist for light and dark mode.
- [ ] Loading, empty, and error previews exist when supported.
- [ ] Preview data is realistic.
- [ ] Previews do not require Hilt, NavController, network, database, or ViewModel objects.

Accessibility and resources:

- [ ] User-visible strings come from `strings.xml`.
- [ ] Colors and typography come from Material theme or approved tokens.
- [ ] Interactive icons/images have meaningful content descriptions.
- [ ] Decorative icons/images use `contentDescription = null`.
- [ ] Touch targets are at least `48.dp`.
- 
---

## 17. UiStateAndActionsEvent
1. **Keep `UiState` in a separate file inside the same screen package**    
2. Each screen must define its `UiState` in its own file within the same screen package.  
3. The models for the screen components must also be placed in this file.2. **Keep events and actions in a separate file**  
4. All screen events and actions must be defined in a separate file.