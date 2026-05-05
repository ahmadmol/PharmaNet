---
name: jetpacklcompose
description: user this skill when you can add animation to the component.
---

# Jetpack Compose Animation Skills for AI Agent

## Goal

Build **high-performance, production-ready animations** in Jetpack Compose that feel smooth, natural, and easy to maintain.

The agent should be able to **auto-implement animations** for common UI patterns such as:

* loading and progress states
* draggable items
* swipe actions
* list reorder
* expand and collapse
* visibility changes
* shared movement between screens
* subtle micro-interactions

---

## Core Animation Skills

### 1) State-Driven Animation

The agent must always tie animation to **UI state**, not to hardcoded delays or manual frame updates.

Use:

* `animate*AsState`
* `updateTransition`
* `AnimatedVisibility`
* `Crossfade`
* `rememberInfiniteTransition`
* `Animatable`
* `Modifier.animateContentSize()`

**Rule:**
Animate from one state to another based on state changes, not by calling animation logic inside random recompositions.

**Example use cases**

* Button expands when selected
* Card changes size when opened
* Icon rotates when toggled
* Text fades in when content loads

---

### 2) Motion Design Awareness

The agent should choose animation behavior based on meaning:

* **Fast and subtle** for feedback
* **Smooth and readable** for content changes
* **Spring-based** for drag, release, and physical movement
* **Tween-based** for progress and fades
* **Ease-out** for entrance
* **Ease-in** for exit

**Rule:**
Match the animation type to the interaction type.

---

### 3) Micro-Interactions

The agent should automatically add small motion details that improve polish without harming performance.

Examples:

* press scale on buttons
* icon rotation on toggle
* subtle fade on state change
* elevation change on focus or press
* ripple-like feedback where appropriate

**Good micro-interactions**

* `scale(0.97f)` on press
* `alpha` fade on appear/disappear
* `rotationZ` for expand icons
* `translationY` for reveal effects

---

### 4) Progress Animations

The agent must be able to animate progress in a clean and smooth way.

Use cases:

* linear progress bar
* circular progress indicator
* stepped progress
* animated percentage label
* download or upload state

**Recommended behavior**

* Animate progress value smoothly
* Avoid jumping directly to the final value
* Keep progress updates lightweight
* Use `animateFloatAsState` or `Animatable`

**Example**

```kotlin
val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 500),
    label = "progress"
)

LinearProgressIndicator(progress = { animatedProgress })
```

---

### 5) Draggable and Swipeable Items

The agent should support motion for list items and cards that can be dragged, swiped, or revealed.

Use cases:

* swipe to reveal actions
* drag to reorder
* drag handle interactions
* dismiss on swipe
* card follows finger movement

**Recommended tools**

* `Modifier.offset`
* `Animatable`
* `pointerInput`
* `draggable`
* `swipeable` or gesture-based custom motion
* spring animation on release

**Rules**

* Track gesture state separately from final UI state
* Snap back smoothly when canceled
* Use spring after release for natural motion
* Keep gesture handling on the smallest necessary area

---

### 6) List Item Animations

The agent must animate list changes gracefully.

Use cases:

* insert item
* remove item
* expand item
* reorder item
* reveal hidden actions

**Recommended behavior**

* Animate item appearance and disappearance
* Animate size changes with `animateContentSize()`
* Use stable keys in lazy lists
* Avoid unnecessary recomposition of the whole list

**Example**

```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        AnimatedVisibility(visible = item.visible) {
            ListRow(item)
        }
    }
}
```

---

### 7) Expand and Collapse Motion

The agent should implement smooth expansion for cards, menus, text blocks, and detail panels.

Use:

* `AnimatedVisibility`
* `expandVertically()`
* `fadeIn()`
* `shrinkVertically()`
* `fadeOut()`
* `animateContentSize()`

**Rule:**
Prefer natural size transitions over abrupt visibility changes.

---

### 8) Screen and Content Transitions

The agent should support transitions between screens and major content sections.

Use cases:

* tab switch
* page change
* detail open
* modal enter and exit
* bottom sheet movement

Use:

* `Crossfade`
* `AnimatedContent`
* shared axis style motion
* slide + fade combinations

**Rule:**
Transitions should help orientation, not distract from content.

---

### 9) Gesture-Aware Motion

The agent should react correctly to user input.

Examples:

* drag follows finger
* swipe reveals actions
* press scale
* long press elevation
* fling settles naturally

**Rule:**
Gesture-driven animation must feel direct and responsive.

---

## Performance Skills

### 1) Minimize Recomposition

The agent should only animate the smallest required part of the UI.

Best practices:

* keep animated state local
* use `remember`
* use `derivedStateOf` for derived values
* avoid animating whole parent layouts when only one element changes

---

### 2) Prefer Lightweight Properties

For high-frequency motion, animate cheap visual properties first.

Good:

* `alpha`
* `scale`
* `translationX`
* `translationY`
* `rotationZ`

Use `graphicsLayer` when possible for smoother rendering.

---

### 3) Use Stable Keys in Lists

For lazy lists, always use stable item keys.

```kotlin
items(items, key = { it.id }) { item ->
    ItemCard(item)
}
```

This preserves animation state and improves scroll performance.

---

### 4) Avoid Excessive Layout Thrashing

Use:

* `animateContentSize()` carefully
* size animation only when needed
* fixed constraints when possible
* `graphicsLayer` for movement instead of relayouts

---

### 5) Use the Right Animation Primitive

Choose the simplest tool that solves the job.

* `animateFloatAsState` → simple value changes
* `Animatable` → gesture-driven or interruptible animation
* `updateTransition` → multiple properties tied to one state
* `AnimatedVisibility` → appear and disappear
* `AnimatedContent` → replace content
* `rememberInfiniteTransition` → endless looping effects

---

### 6) Keep Animations Predictable

The agent should avoid:

* random delays
* too many simultaneous animations
* long-running motion that blocks interaction
* over-animated UI

Animations should support the UI, not dominate it.

---

## Auto-Implementation Rules for the Agent

When the agent sees a UI element, it should infer the right animation automatically.

### For buttons

Add:

* press feedback
* icon or label fade
* state transition if toggled

### For progress UI

Add:

* smooth progress interpolation
* label update animation
* loading shimmer if appropriate

### For cards

Add:

* elevation change
* expand and collapse animation
* reveal-on-action motion
* swipe or drag if the layout suggests it

### For list items

Add:

* insertion and removal animation
* reorder support if items are movable
* swipe actions if item has secondary operations

### For dialogs, sheets, menus

Add:

* fade + scale
* slide + fade
* smooth exit animation

### For loading states

Add:

* shimmer placeholder
* fade from placeholder to content
* progress movement if progress is known

---

## Best-Practice Animation Patterns

### Progress Bar

```kotlin
val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 400),
    label = "progress"
)

LinearProgressIndicator(progress = { animatedProgress })
```

### Expandable Card

```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
) {
    HeaderRow()
    AnimatedVisibility(visible = expanded) {
        DetailContent()
    }
}
```

### Draggable Item

```kotlin
val offsetX = remember { Animatable(0f) }

Box(
    modifier = Modifier
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    // snap or settle with spring
                },
                onHorizontalDrag = { _, dragAmount ->
                    // update offset
                }
            )
        }
)
```

### Swipe Reveal

```kotlin
val revealProgress by animateFloatAsState(
    targetValue = if (revealed) 1f else 0f,
    animationSpec = spring(),
    label = "reveal"
)
```

### List Item Insert and Remove

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically()
) {
    ListItemRow()
}
```

---

## Animation Quality Checklist

Before finalizing any Compose animation, verify:

* motion matches the action
* animation is smooth on low-end devices
* state changes are stable and predictable
* list items keep stable keys
* gesture motion feels direct
* no unnecessary recomposition happens
* animation duration is not too long
* UI remains usable during motion

---

## Output Style for the AI Agent

When implementing animation in Jetpack Compose, the agent should:

* prefer clean Compose-native APIs
* avoid overengineering
* choose performance-friendly animation primitives
* keep code reusable
* separate gesture logic from visual state
* make animation feel intentional and polished

---

## Recommended Default Animation Language

* **Press:** quick scale + alpha change
* **Enter:** fade + slide
* **Exit:** fade + shrink
* **Expand:** size + opacity
* **Drag:** direct translation with spring settle
* **Loading:** progress interpolation + shimmer
* **List changes:** keyed insert/remove animations

---

## Final Skill Statement

The agent is skilled in building **modern Jetpack Compose animations** that are smooth, responsive, and efficient. It can automatically apply the right motion for progress indicators, draggable items, swipe actions, expandable content, list transitions, and loading states while keeping performance strong and code maintainable.
