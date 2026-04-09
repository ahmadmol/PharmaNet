package com.pharmalink.feature.resources.presentation

/**
 * Maps UI [ResourceFilter] chips to [WarehouseFilter] used by [ResourcesViewModel].
 */
fun WarehouseFilter.toResourceFilter(): ResourceFilter = when (this) {
    WarehouseFilter.ALL -> ResourceFilter.ALL
    WarehouseFilter.NEARBY -> ResourceFilter.NEARBY
    WarehouseFilter.COLD_CHAIN -> ResourceFilter.SUPPLY_CHAIN
}

fun ResourceFilter.toWarehouseFilter(): WarehouseFilter = when (this) {
    ResourceFilter.ALL -> WarehouseFilter.ALL
    ResourceFilter.NEARBY -> WarehouseFilter.NEARBY
    ResourceFilter.SUPPLY_CHAIN -> WarehouseFilter.COLD_CHAIN
    ResourceFilter.AVAILABLE_NOW -> WarehouseFilter.ALL
    ResourceFilter.FAST_DELIVERY -> WarehouseFilter.NEARBY
}
