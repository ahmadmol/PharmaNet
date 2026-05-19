package com.pharmalink.domain.model

data class RecentActivity(
    val id: String,
    val action: String,
    val userName: String,
    val timestamp: String,
    val status: String, // SUCCESS, FAILED, PENDING
)
