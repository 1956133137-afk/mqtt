package com.yannuo.dgcanteen.model

import com.google.gson.annotations.SerializedName

data class LaundryListResponse(
    @SerializedName("list")
    val list: List<DishesInfo>,
    val totalRecord: Int,
    val pageSize: Int,
    val page: Int,
    val totalPage: Int
)