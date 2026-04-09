package com.pharmalink.core.repository

import com.pharmalink.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

/**
 * Notification Repository Interface
 * Handles notification management operations
 */
interface NotificationRepository {
    suspend fun getNotifications(): Flow<List<AppNotification>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    suspend fun deleteAllNotifications(): Result<Unit>
    fun getUnreadCount(): Flow<Int>
}
