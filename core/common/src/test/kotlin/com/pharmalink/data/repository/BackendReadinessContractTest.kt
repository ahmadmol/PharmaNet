package com.pharmalink.data.repository

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendReadinessContractTest {

    private val root: File = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private val repositorySource: String =
        File(root, "core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt").readText()

    private val migrationSql: String =
        File(root, "database/migrations").walkTopDown()
            .filter { it.isFile && it.extension == "sql" }
            .joinToString(separator = "\n") { it.readText() }

    @Test
    fun repositoryRpcCallsHaveSqlDefinitions() {
        val rpcNames = listOf(
            "create_public_user_profile",
            "get_public_pharmacies",
            "get_public_pharmacies_for_medicine",
            "get_nearby_orders",
            "update_my_warehouse_location",
            "submit_pharmacy_request",
            "warehouse_accept_b2b_request",
            "warehouse_reject_b2b_request",
            "warehouse_start_b2b_fulfillment",
            "warehouse_mark_b2b_delivered",
            "submit_support_request",
            "get_my_customer_orders",
            "get_pharmacy_customer_orders",
            "cancel_customer_order",
            "confirm_customer_order",
            "reject_customer_order",
            "mark_customer_order_ready_for_pickup",
            "mark_customer_order_out_for_delivery",
            "mark_customer_order_delivered",
            "customer_accept_order_price",
            "customer_reject_order_price",
            "admin_get_all_users",
            "admin_update_user_profile",
            "admin_create_pharmacy",
            "admin_create_warehouse",
            "admin_get_audit_logs",
            "admin_get_audit_log_detail",
            "admin_get_dashboard_stats",
            "admin_get_all_orders",
            "admin_get_order_detail",
            "admin_get_pending_requests",
            "admin_get_recent_activities",
            "admin_get_system_health",
        )

        rpcNames.forEach { rpc ->
            assertTrue("Missing SQL definition for RPC $rpc", migrationSql.contains("FUNCTION public.$rpc") || migrationSql.contains("FUNCTION $rpc"))
        }
    }

    @Test
    fun storageAndInventoryContractsExist() {
        assertTrue(migrationSql.contains("CREATE OR REPLACE VIEW public.warehouse_inventory"))
        assertTrue(migrationSql.contains("'prescriptions', 'prescriptions'"))
        assertTrue(migrationSql.contains("'medicines', 'medicines'"))
        assertTrue(repositorySource.contains("from(\"warehouse_inventory\")"))
        assertFalse(repositorySource.contains("warehouse_inventory table not found, falling back to medicines table"))
    }

    @Test
    fun fakeSupabaseRepositoryBehaviorsStayDisabled() {
        val observeComplianceBody = repositorySource.substringAfter("override fun observeCompliance()")
            .substringBefore("override suspend fun updateProfile")

        assertFalse(observeComplianceBody.contains("ComplianceOverview("))
        assertTrue(
            observeComplianceBody.contains("UnsupportedOperationException") ||
                observeComplianceBody.contains("Backend not confirmed") ||
                observeComplianceBody.contains("disabled"),
        )
        assertFalse(repositorySource.contains("Result.success(ComplianceOverview("))
        val fetchHomeStatsBody = repositorySource.substringAfter("override suspend fun fetchHomeStats()")
            .substringBefore("override suspend fun fetchMedicines")

        assertTrue(
            fetchHomeStatsBody.contains("UnsupportedOperationException") ||
                fetchHomeStatsBody.contains("Backend not confirmed"),
        )
        val warehouseShipmentsBody = repositorySource.substringAfter("override suspend fun getWarehouseShipments")
            .substringBefore("override suspend fun createRequest")

        assertTrue(warehouseShipmentsBody.contains("Result.failure(UnsupportedOperationException"))
        assertFalse(repositorySource.contains("return@runCatching defaultHomeStats()"))
        assertFalse(repositorySource.contains("override suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>> = Result.success(emptyList())"))
    }
}
