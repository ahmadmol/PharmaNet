package com.pharmalink.domain.model

enum class ComplianceDocumentStatus {
    VALID,
    EXPIRING_SOON,
    MISSING,
}

data class ComplianceDocument(
    val id: String,
    val title: String,
    val status: ComplianceDocumentStatus,
    val statusLabel: String,
    val expiryLabel: String,
    val note: String,
)

data class SupplierComplianceItem(
    val id: String,
    val supplierName: String,
    val statusLabel: String,
    val nextReviewLabel: String,
    val note: String,
)

data class ComplianceOverview(
    val pharmacyId: String,
    val licenseStatusLabel: String,
    val licenseNumber: String,
    val licenseExpiryLabel: String,
    val summaryLabel: String,
    val alerts: List<String>,
    val documents: List<ComplianceDocument>,
    val supplierItems: List<SupplierComplianceItem>,
)
