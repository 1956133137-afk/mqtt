package com.yannuo.dgcanteen.model

/**
 * Data class representing a single laundry record.
 */
data class LaundryRecord(
    val index: Int,
    val orderTime: String,
    val pickupTime: String,
    val clothingType: String
)
