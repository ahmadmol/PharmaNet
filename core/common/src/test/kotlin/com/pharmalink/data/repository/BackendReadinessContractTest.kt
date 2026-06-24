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
            "create_customer_order",
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
            // Phase 2-5 cross-account additions
            "admin_get_all_requests",
            "admin_get_request_detail",
            "admin_force_cancel_order",
            "admin_force_cancel_request",
            "admin_provision_pharmacy_with_owner",
            "admin_provision_warehouse_with_owner",
            "expire_stale_pending_customer_orders",
        )

        rpcNames.forEach { rpc ->
            assertTrue("Missing SQL definition for RPC $rpc", migrationSql.contains("FUNCTION public.$rpc") || migrationSql.contains("FUNCTION $rpc"))
        }
    }

    /**
     * Stronger than name-only existence: for every RPC whose parameter names are known from the
     * Kotlin call sites / param DTOs, assert that at least one SQL definition declares each of
     * those parameter names. This catches parameter drift between Kotlin DTOs and SQL signatures.
     *
     * Only RPCs with parameter signatures that are verifiable from the repository are listed.
     */
    @Test
    fun repositoryRpcParametersMatchSqlSignatures() {
        val expectedParams: Map<String, List<String>> = mapOf(
            "create_customer_order" to listOf(
                "p_medicine_id", "p_medicine_name", "p_quantity", "p_unit", "p_pharmacy_id",
                "p_urgency", "p_request_scope", "p_fulfillment_type", "p_delivery_address",
                "p_delivery_latitude", "p_delivery_longitude", "p_delivery_phone", "p_notes",
                "p_prescription_url",
            ),
            "confirm_customer_order" to listOf("p_order_id", "p_total_price_cents"),
            "cancel_customer_order" to listOf("p_order_id"),
            "customer_accept_order_price" to listOf("p_order_id"),
            "customer_reject_order_price" to listOf("p_order_id"),
            "reject_customer_order" to listOf("p_order_id"),
            "mark_customer_order_ready_for_pickup" to listOf("p_order_id"),
            "mark_customer_order_out_for_delivery" to listOf("p_order_id"),
            "mark_customer_order_delivered" to listOf("p_order_id"),
            "submit_pharmacy_request" to listOf("p_request_id"),
            "warehouse_accept_b2b_request" to listOf("p_request_id", "p_total_price_cents"),
            "warehouse_reject_b2b_request" to listOf("p_request_id", "p_rejection_reason"),
            "warehouse_start_b2b_fulfillment" to listOf("p_request_id"),
            "warehouse_mark_b2b_delivered" to listOf("p_request_id", "p_delivery_note"),
            "pharmacy_accept_b2b_quote" to listOf("p_request_id"),
            "pharmacy_reject_b2b_quote" to listOf("p_request_id", "p_rejection_reason"),
            "get_public_pharmacies_for_medicine" to listOf("p_medicine_id"),
            "admin_get_audit_log_detail" to listOf("p_log_id"),
            // Phase 2-5 cross-account additions
            "admin_get_all_requests" to listOf("p_status", "p_pharmacy_id", "p_warehouse_id", "p_search", "p_limit", "p_offset"),
            "admin_get_request_detail" to listOf("p_request_id"),
            "admin_force_cancel_order" to listOf("p_order_id", "p_reason"),
            "admin_force_cancel_request" to listOf("p_request_id", "p_reason"),
            "admin_provision_pharmacy_with_owner" to listOf("p_name", "p_location", "p_contact_number", "p_license_number", "p_owner_user_id"),
            "admin_provision_warehouse_with_owner" to listOf("p_name", "p_location", "p_contact_number", "p_owner_user_id"),
        )

        expectedParams.forEach { (rpc, params) ->
            val paramBlocks = sqlFunctionParameterBlocks(rpc)
            assertTrue("No SQL definition with a parameter list found for RPC $rpc", paramBlocks.isNotEmpty())
            params.forEach { param ->
                assertTrue(
                    "RPC $rpc is missing SQL parameter $param (Kotlin call expects it)",
                    paramBlocks.any { it.contains(param) },
                )
            }
        }
    }

    /**
     * Returns the parameter-list text (between the outer parentheses) for every SQL definition of
     * the given function name. Handles nested parentheses inside default expressions / type modifiers.
     */
    private fun sqlFunctionParameterBlocks(rpc: String): List<String> {
        val blocks = mutableListOf<String>()
        val header = Regex("FUNCTION\\s+(?:public\\.)?" + Regex.escape(rpc) + "\\s*\\(")
        for (match in header.findAll(migrationSql)) {
            val openParen = match.range.last // index of '('
            var depth = 0
            val sb = StringBuilder()
            var i = openParen
            while (i < migrationSql.length) {
                val c = migrationSql[i]
                when (c) {
                    '(' -> {
                        depth++
                        if (depth > 1) sb.append(c)
                    }
                    ')' -> {
                        depth--
                        if (depth == 0) break
                        sb.append(c)
                    }
                    else -> if (depth >= 1) sb.append(c)
                }
                i++
            }
            blocks.add(sb.toString())
        }
        return blocks
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
