package com.yannuo.dgcanteen.model

import com.google.gson.annotations.SerializedName

/**
 * 洗衣下单请求体
 */
data class LaundryOrderRequest(
    @SerializedName("campusId")
    val campusId: String,
    @SerializedName("businessId")
    val businessId: String,
    @SerializedName("custId")
    val custId: String,
    @SerializedName("personName")
    val personName: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("payment")
    val payment: String,
    @SerializedName("dcOrderDishesList")
    val dcOrderDishesList: List<LaundryOrderItem>,
    @SerializedName("orderStatus")
    val orderStatus: String,
    @SerializedName("orderType")
    val orderType: String
)

/**
 * 洗衣下单项目
 */
data class LaundryOrderItem(
    @SerializedName("dishesId")
    val dishesId: String,
    @SerializedName("dishesName")
    val dishesName: String,
    @SerializedName("dishesNum")
    val dishesNum: Int,
    @SerializedName("unit")
    val unit: String,
    @SerializedName("imgUrl")
    val imgUrl: String
)
