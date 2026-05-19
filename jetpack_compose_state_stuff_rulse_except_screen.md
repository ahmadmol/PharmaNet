# ViewModel · UiState · Actions · Effects

Rules for everything except the screen composable. Define the contract and wire it to whatever screen already exists.

---

## Data Flow

```
Action (user intent) → ViewModel.onAction() → UiState update
                                             ↘ Effect emit
```

---

## File Layout

```
feature/home/
  HomeUiState.kt     # UiState + UI models + Action + Effect + preview fixtures
  HomeViewModel.kt   # state updates, business logic, effect emission
```

- `UiState`, all screen component models, `Action`, and `Effect` must all live in the `UiState` file — not in the screen file.
- No business logic in composables.
- No Compose UI objects in ViewModels.
- No `Context`, `Resources`, `NavController`, or `SnackbarHostState` inside ViewModels.

---

## UiState

### What belongs in UiState

| Allowed (persistent state) | Forbidden — use Effect instead |
|---|---|
| `isLoading`, `isRefreshing` | `navigateToDetail: Boolean` |
| Content data, empty flag | `showToast: Boolean` |
| Selected tab / filter | `snackbarMessage: String` |
| Text field values | `openBottomSheet: Boolean` |
| Inline validation errors | `launchPermission: Boolean` |
| Pagination flags | Anything that must fire exactly once |

### Required shape

```kotlin
@Immutable data class HomeUiState(
    val isLoading: Boolean = false,           // full-screen blocking load
    val isRefreshing: Boolean = false,        // pull-to-refresh, non-blocking
    val contentError: UiText = UiText.Empty,  // persistent content-area error
    val posts: List<PostModel> = emptyList(),
)
```

- Transient error notifications go through `Effect`, not a `message: String` field.
- Annotate with `@Immutable` when every property is truly immutable.
- Use read-only `List<T>`, `Set<T>`, `Map<K,V>` — never mutable variants.
- Never store lambdas, `Flow`, `State`, `MutableState`, `CoroutineScope`, or Compose runtime objects in `UiState`.
- Never expose repository or domain models directly in `UiState`.

### UiText

ViewModels must never call `stringResource`, `context.getString`, or `resources.getString`. Use `UiText` and resolve it only in the UI layer.

```kotlin
sealed interface UiText {
    data object Empty : UiText
    data class DynamicString(val value: String) : UiText
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText
}
```

- ViewModels may create `UiText.StringResource`.
- Prefer `StringResource` for user-facing text; use `DynamicString` for server-provided or already-localised text.

---

## UI Models

### Non-null defaults

| Type | Default |
|---|---|
| `String` | `""` |
| `Int` / `Long` | `0` (or `-1` when `0` is meaningful) |
| `Boolean` | `false` |
| `List<T>` / `Set<T>` / `Map<K,V>` | `emptyList()` / `emptySet()` / `emptyMap()` |
| `Enum` | `NONE` or `UNKNOWN` as first entry |
| Nested model | `ModelName()` |

- Nullable fields are forbidden unless absence is truly meaningful.
- Every model rendered in a lazy list must have a stable `id`. Use real backend IDs; never use list index.
- Null cleanup and domain-to-UI mapping happen in mapper functions, not in composables.

---

## Actions

```kotlin
sealed interface HomeAction {
    data object OnRetryClicked : HomeAction
    data class OnPostClicked(val postId: String) : HomeAction
    data class OnSearchQueryChanged(val query: String) : HomeAction
}
```

- Name for user intent: `OnSaveClicked`, `OnQueryChanged` — not `OnClick`.
- `data object` for no-parameter actions; `data class` for parameterised ones.
- Actions are pure data — no logic, no callbacks, no lambdas inside them.
- Do not create actions for internal ViewModel work — only for user-driven intents.

---

## Effects

```kotlin
sealed interface HomeEffect {
    data object NavigateBack : HomeEffect
    data class NavigateToDetail(val postId: String) : HomeEffect
    data class ShowMessage(
        val message: UiText,
        val type: MessageType = MessageType.Info
    ) : HomeEffect
}
```

- Navigation, snackbars, toasts, bottom sheets opened by command, permission launchers, share/browser intents — always effects, never `UiState` fields.
- Effects are emitted once and consumed once — never stored in `UiState`.
- Effects tell the screen *what to do*; they must not carry UI objects (`SnackbarHostState`, `SheetState`, etc.).
- Handle every variant exhaustively in the screen's `when(effect)` — no `else` branch.

---

## ViewModel

### State and effect declarations

```kotlin
private val _state = MutableStateFlow(HomeUiState())
val state: StateFlow<HomeUiState> = _state.asStateFlow()

private val _effect = MutableSharedFlow<HomeEffect>(
    replay = 0,
    extraBufferCapacity = 1
)
val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()
```

- Mutable flows are always `private`; expose only read-only `StateFlow` / `SharedFlow`.
- `replay = 0` for effects; `extraBufferCapacity = 1` prevents quick emissions from suspending.

### Single action entry point

```kotlin
fun onAction(action: HomeAction) {
    when (action) {
        is HomeAction.OnRetryClicked          -> loadData()
        is HomeAction.OnPostClicked           -> navigateToPost(action.postId)
        is HomeAction.OnSearchQueryChanged    -> updateQuery(action.query)
    }
    // No else branch — exhaustive by the type system
}
```

- One public `onAction` entry point for all UI actions.
- Other public functions only when a framework (e.g. lifecycle callbacks) requires them.
- All helper functions are `private`.

### State updates

```kotlin
// Preferred
_state.update { it.copy(isLoading = true, contentError = UiText.Empty) }

// Never do this
_state.value = _state.value.copy(isLoading = true)
```

- Always use `_state.update { }` over direct `.value =` assignment.
- Never mutate collections in-place — always create new lists via `copy()` or list operators.
- Clear persistent errors when a new load starts.

### Initial loading

```kotlin
init {
    loadData()
}
```

- Data required on first screen open is triggered from `init`.
- Use `SavedStateHandle` to receive required navigation arguments.
- UI composables must not trigger first-load work.

### Emitting effects

```kotlin
private fun navigateToPost(postId: String) {
    viewModelScope.launch {
        _effect.emit(HomeEffect.NavigateToDetail(postId))
    }
}
```

- Always emit from `viewModelScope.launch`.
- Use `emit` when order matters; `tryEmit` only for drop-safe effects.
- Never store a pending effect in state to work around lifecycle issues.

### Error handling

- Persistent errors that change screen content → `UiState.contentError`.
- Transient error notifications (toasts, snackbars) → `Effect.ShowMessage`.
- Never expose raw exception messages to users.
- Log unexpected exceptions through the project's logging mechanism, not through UI state.

---

## Linking to an Existing Screen

The screen already exists. You only need to connect three public surfaces.

```kotlin
// Inside the existing <Feature>Screen composable — add these lines:

val state by viewModel.state.collectAsStateWithLifecycle()

CollectEffect(viewModel.effect) { effect ->
    when (effect) {
        is HomeEffect.NavigateBack          -> navController.popBackStack()
        is HomeEffect.NavigateToDetail      -> navController.navigate(...)
        is HomeEffect.ShowMessage           -> messageBarState.addMessage(effect.message)
    }
}

HomeContent(
    state = state,
    onAction = viewModel::onAction,
)
```

- Collect state with `collectAsStateWithLifecycle()` — never `collectAsState()`.
- One effect collector per screen. Never collect ViewModel flows inside child components.
- Pass `viewModel::onAction` down to content as a lambda — the screen is the only place that knows about the ViewModel.

### CollectEffect utility

Define once in a shared UI module and reuse across all screens:

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

---

## PR Checklist

### UiState and models

- [ ] `UiState` contains persistent screen state only — no navigation, snackbar, dialog, or launcher commands
- [ ] All UI model fields are non-null with safe defaults
- [ ] All lazy-list models have a stable `id` field
- [ ] `@Immutable` applied where every property is truly immutable
- [ ] No mutable collections in state
- [ ] No domain or repository models exposed directly in `UiState`
- [ ] `UiState` and models are defined in the `UiState` file, not the screen file

### Actions and effects

- [ ] Exactly one `<Feature>Action` sealed type
- [ ] Exactly one `<Feature>Effect` sealed type
- [ ] Actions named for user intent (`OnSaveClicked`, not `OnClick`)
- [ ] `when(action)` in ViewModel is exhaustive — no `else` branch
- [ ] `when(effect)` in screen is exhaustive — no `else` branch
- [ ] Effects used for all one-time commands (nav, messages, sheets, launchers)
- [ ] No `Effect` subtypes carry UI objects (`SnackbarHostState`, `SheetState`, etc.)

### ViewModel

- [ ] Mutable flows are `private`; public flows are read-only
- [ ] `MutableStateFlow` for state; `MutableSharedFlow(replay=0, extraBufferCapacity=1)` for effects
- [ ] State updated via `_state.update { }`
- [ ] Initial data loading started from `init`
- [ ] No `Context`, `Resources`, `NavController`, or Compose state in ViewModel
- [ ] Content errors → `UiState.contentError`; transient errors → `Effect.ShowMessage`
- [ ] All helper functions are `private`
- [ ] Effects emitted inside `viewModelScope.launch`
- [ ] No raw exception messages exposed to users
