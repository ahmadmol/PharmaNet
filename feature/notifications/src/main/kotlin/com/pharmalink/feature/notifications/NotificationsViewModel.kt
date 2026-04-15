package com.pharmalink.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.NotificationCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class NotificationFilter {
    All,
    Attention,
    Requests,
    Orders,
    Warehouses,
    Compliance,
    Support,
}

enum class NotificationSectionType {
    Attention,
    Unread,
    Archived,
}

data class NotificationSection(
    val type: NotificationSectionType,
    val items: List<AppNotification>,
)

data class NotificationsUiState(
    val selectedFilter: NotificationFilter = NotificationFilter.All,
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val attentionCount: Int = 0,
    val filterCounts: Map<NotificationFilter, Int> = NotificationFilter.entries.associateWith { 0 },
    val screenState: ScreenState<List<NotificationSection>> = ScreenState.Loading,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(NotificationFilter.All)
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeNotifications(),
                selectedFilter,
            ) { notifications, filter ->
                val filtered = notifications.filterBy(filter)
                NotificationsUiState(
                    selectedFilter = filter,
                    totalCount = notifications.size,
                    unreadCount = notifications.count { !it.read },
                    attentionCount = notifications.count { it.requiresAction },
                    filterCounts = NotificationFilter.entries.associateWith { currentFilter ->
                        notifications.filterBy(currentFilter).size
                    },
                    screenState = if (filtered.isEmpty()) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Success(filtered.groupedSections())
                    },
                )
            }
                .catch { error ->
                    _state.value = _state.value.copy(
                        screenState = ScreenState.Error(
                            error.message ?: "تعذر تحميل الإشعارات حاليًا.",
                        ),
                    )
                }
                .collect { _state.value = it }
        }
    }

    fun selectFilter(filter: NotificationFilter) {
        selectedFilter.value = filter
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    private fun List<AppNotification>.filterBy(filter: NotificationFilter): List<AppNotification> {
        return when (filter) {
            NotificationFilter.All -> this
            NotificationFilter.Attention -> filter { it.requiresAction }
            NotificationFilter.Requests -> filter { it.category == NotificationCategory.REQUESTS }
            NotificationFilter.Orders -> filter { it.category == NotificationCategory.ORDERS }
            NotificationFilter.Warehouses -> filter { it.category == NotificationCategory.WAREHOUSES }
            NotificationFilter.Compliance -> filter { it.category == NotificationCategory.COMPLIANCE }
            NotificationFilter.Support -> filter { it.category == NotificationCategory.SUPPORT }
        }
    }

    private fun List<AppNotification>.groupedSections(): List<NotificationSection> {
        val attention = filter { !it.read && it.requiresAction }
        val unread = filter { !it.read && !it.requiresAction }
        val archived = filter { it.read }

        return buildList {
            if (attention.isNotEmpty()) add(NotificationSection(NotificationSectionType.Attention, attention))
            if (unread.isNotEmpty()) add(NotificationSection(NotificationSectionType.Unread, unread))
            if (archived.isNotEmpty()) add(NotificationSection(NotificationSectionType.Archived, archived))
        }
    }
}
