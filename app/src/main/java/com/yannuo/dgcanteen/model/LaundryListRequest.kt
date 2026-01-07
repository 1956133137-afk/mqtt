package com.yannuo.dgcanteen.model

data class LaundryListRequest(
    val campusId: String,
    val businessId: String,
    val custId: String,
    val page: Int,
    val pageSize: Int
)