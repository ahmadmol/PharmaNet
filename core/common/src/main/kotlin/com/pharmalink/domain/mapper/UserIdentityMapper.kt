package com.pharmalink.domain.mapper

import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.OrganizationType
import com.pharmalink.domain.model.UserIdentity
import com.pharmalink.domain.model.UserSnapshot

fun UserSnapshot.toUserIdentity(): UserIdentity {
    val orgType = when (accountType) {
        AccountType.PHARMACY -> OrganizationType.PHARMACY
        AccountType.WAREHOUSE -> OrganizationType.WAREHOUSE
        AccountType.PUBLIC_USER -> null
        AccountType.ADMIN -> OrganizationType.PLATFORM
    }

    val organizationId = when (accountType) {
        AccountType.PHARMACY -> pharmacyId.takeIf { it.isNotBlank() }
        AccountType.WAREHOUSE -> {
            // Phase 2 compatibility path:
            // prefer explicit warehouse linkage; fallback to legacy carrier while migration is active.
            warehouseId.takeIf { it.isNotBlank() } ?: pharmacyId.takeIf { it.isNotBlank() }
        }
        AccountType.ADMIN, AccountType.PUBLIC_USER -> null
    }

    val organizationName = when (accountType) {
        AccountType.PHARMACY -> pharmacyName.takeIf { it.isNotBlank() }
        AccountType.WAREHOUSE ->
            // Phase 3 compatibility path:
            // prefer explicit warehouse name; fallback to legacy carrier while migration is active.
            warehouseName.takeIf { it.isNotBlank() } ?: pharmacyName.takeIf { it.isNotBlank() }
        AccountType.ADMIN, AccountType.PUBLIC_USER -> null
    }

    return UserIdentity(
        userId = userId,
        role = accountType,
        organizationType = orgType,
        organizationId = organizationId,
        organizationName = organizationName,
        displayName = displayName,
    )
}


