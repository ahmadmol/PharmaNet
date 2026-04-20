package com.pharmalink.domain.model

sealed class TenantContext {

    data class PharmacyContext(
        val pharmacyId: String,
    ) : TenantContext()

    data class WarehouseContext(
        val warehouseId: String,
    ) : TenantContext()

    data object PlatformContext : TenantContext()

    data object PublicContext : TenantContext()
}
