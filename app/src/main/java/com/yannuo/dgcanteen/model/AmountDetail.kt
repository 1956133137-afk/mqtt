package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/19 16:59
 * @Version 1.0
 */
data class AmountDetail(
    var mealName: String = "",  //餐别名称
    var payment: Double = 0.0, //餐别付款总金额
    var actualPayment: Double = 0.0 //餐别实付总金额
)