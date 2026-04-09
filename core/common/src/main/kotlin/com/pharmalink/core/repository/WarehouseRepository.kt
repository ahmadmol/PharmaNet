package com.pharmalink.core.repository

import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseFilter
import com.pharmalink.domain.model.WarehouseSort
import kotlinx.coroutines.flow.Flow

/**
 * Warehouse Repository Interface
 * Handles warehouse/resource management operations
 */
interface WarehouseRepository {
    suspend fun getWarehouses(): Flow<List<Warehouse>>
    suspend fun getWarehousesByFilter(filter: WarehouseFilter): Flow<List<Warehouse>>
    suspend fun searchWarehouses(query: String): Flow<List<Warehouse>>
    suspend fun getWarehouseById(warehouseId: String): Warehouse?
    suspend fun updateWarehouse(warehouse: Warehouse): Result<Warehouse>
    suspend fun getNearbyWarehouses(location: String): Flow<List<Warehouse>>
}
