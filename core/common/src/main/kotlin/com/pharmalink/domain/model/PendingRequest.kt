package com.pharmalink.domain.model

data class PendingRequest(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val requestType: String, // ORDER, FACILITY, USER
)
