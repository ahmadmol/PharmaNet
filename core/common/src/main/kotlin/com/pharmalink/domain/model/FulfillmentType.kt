package com.pharmalink.domain.model

/**
 * Defines how the order will be fulfilled.
 */
enum class FulfillmentType {
    /**
     * Customer picks up the order from the pharmacy.
     * For B2C orders only.
     * Does not require deliveryAddress or deliveryPhone.
     */
    PICKUP,
    
    /**
     * Pharmacy delivers the order to the customer.
     * For B2C orders only.
     * Requires deliveryAddress and deliveryPhone.
     */
    DELIVERY,
}
