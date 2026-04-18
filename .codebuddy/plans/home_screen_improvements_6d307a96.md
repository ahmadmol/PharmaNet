---
name: home_screen_improvements
overview: "Precisely implement Home screen improvements: remove top-bar search, fix notification navigation, apply role-based visibility for \"Add Medicine\", refactor quick actions to explicit callbacks, and fix double-loading."
todos:
  - id: update-ui-state
    content: 在 HomeUiState.Success 中添加 canAddMedicine 标识和角色信息
    status: completed
  - id: refactor-view-model
    content: 在 HomeViewModel 中获取账户类型，移除 init 中的重复加载逻辑
    status: completed
    dependencies:
      - update-ui-state
  - id: refactor-home-header
    content: 从 HomeScreen 的 HomeHeader 组件中移除搜索图标及其参数，更新占位图
    status: completed
  - id: refactor-quick-actions
    content: 重构 QuickActionsSection，实现基于角色的可见性和命名回调
    status: completed
    dependencies:
      - refactor-view-model
  - id: fix-double-loading
    content: 移除 HomeScreen 中 LaunchedEffect 的数据加载调用，统一由 ViewModel 管理
    status: completed
    dependencies:
      - refactor-view-model
  - id: wire-notifications-nav
    content: 在 PharmaNavigator 和 HomeRoute 中打通通往 Notifications 屏幕的导航路径
    status: completed
    dependencies:
      - refactor-home-header
  - id: verify-and-compile
    content: 验证角色可见性逻辑，确保无搜索图标残留，且代码编译通过
    status: completed
    dependencies:
      - wire-notifications-nav
      - refactor-quick-actions
      - fix-double-loading
---

## 用户需求分析

本项目需要对主页（Home Screen）进行一系列 UI 优化和逻辑重构，核心目标是提升导航准确性、实现基于角色的内容可见性，并修复现有的逻辑缺陷（如重复加载）。

## 核心功能点

- **顶部栏优化**：从顶部栏移除搜索图标及其回调；激活通知图标，点击后跳转至通知界面（不再跳转至订单界面）。
- **角色权限控制**：
    - 根据用户类型（AccountType）控制“添加药品（Add Medicine）”功能的可见性。
    - **仓库账号（WAREHOUSE）**：可见。
    - **药店账号（PHARMACY）**：隐藏。
- **快捷操作重构**：废弃基于索引（Index-based）的导航分发逻辑，改为使用明确的、类型安全的函数回调。
- **逻辑修复**：修复主页初始化时 `ViewModel` 和 `Compose` 生命周期导致的重复数据请求问题。
- **后端适配预留**：清理主页统计数据中的硬编码逻辑，为未来接入真实的统计汇总（RPC/Function）和动态活动流做准备，保持 UI 视觉一致性。

## 视觉效果描述

- 顶部栏布局简化，仅保留个人资料和通知图标。
- 快捷操作区域会根据登录账号身份动态显示或隐藏特定功能卡片。
- 主页统计、最近活动和推荐仓库的 UI 布局保持现状，但数据加载过程将更稳定且仅触发一次。

## 技术栈选择

- **UI 框架**：Jetpack Compose
- **导航**：Navigation Compose
- **依赖注入**：Hilt
- **架构模式**：MVVM + Clean Architecture
- **后端集成**：Supabase (Postgrest)

## 实施方案

### 1. 导航与 UI 架构

- **PharmaNavigator**：作为根导航器，将 `AppDestination.Notifications` 的跳转逻辑传递给 `homeScreen` 路由。
- **HomeRoute**：新增 `onNavigateToNotifications` 参数，确保点击事件能穿透到具体的 Composable。

### 2. 状态管理

- **HomeUiState**：新增 `canAddMedicine` 布尔标识，或直接暴露 `accountType` 供 UI 判断。
- **HomeViewModel**：在 `loadHomeData` 中通过 `authRepository.getUserSnapshot()` 获取当前用户信息，注入角色权限逻辑。

### 3. 组件重构

- **HomeScreen**：
    - 修改 `HomeHeader` 参数，移除 `onSearchClick`，将 `onNotificationsClick` 绑定到正确的导航动作。
    - 移除 `HomeScreen` 中的 `LaunchedEffect` 重复请求，依赖 `ViewModel` 的 `init` 块进行首次加载。
    - 重写 `QuickActionsSection`：不再在 `items` 循环中使用索引 `when` 分支，而是根据角色构建卡片列表，并为每个卡片分配独立的回调。

### 4. 目录结构变更清单

```
feature/home/src/main/kotlin/com/pharmalink/feature/home/
├── HomeUiState.kt        # [MODIFY] 添加权限状态位
├── HomeViewModel.kt      # [MODIFY] 修复双重加载，注入角色判断
├── HomeRoute.kt          # [MODIFY] 更新路由定义，增加通知导航参数
└── HomeScreen.kt         # [MODIFY] 移除顶部搜索，重构快捷操作与通知跳转
app/src/main/kotlin/com/pharmalink/feature/main/navigation/
└── PharmaNavigator.kt    # [MODIFY] 配置 Home 到 Notifications 的导航连接
```