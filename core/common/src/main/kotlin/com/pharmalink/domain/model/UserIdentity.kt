package com.pharmalink.domain.model

data class UserIdentity(
    val userId: String,
    val role: AccountType,

    val organizationType: OrganizationType?,
    val organizationId: String?,
    val organizationName: String?,

    val displayName: String,

    val permissions: Set<String> = emptySet(),
)
