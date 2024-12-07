package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/12 15:34
 * @Version 1.0
 */
data class MealTimeRuleRequest(
    var businessId: String,
    var campusId: String,
    var custId: String
//    var userTypeId: String
) {
}

data class MealTimeRuleReceive(
    var id: Int? = null,
    var mealName: String = "",
    var flag: Int = 2, // 是否支持顺延，1支持，2不支持
    var mealNum: String = "0", // 每日限制次数
    var standardName: String = "", // 规则名称
    var standardNum: String = "", // 每次收款使用的餐次次数
    var subsidyMoney: String = "0", // 餐补金额
    var userTypeId: String = "0",  // 人员Id
    var userTypeName: String? = null,
    var price: Float = 0.0f, // 餐别金额
    var price1: Float? = null // 外来人员金额

)
