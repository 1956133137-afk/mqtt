package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/7 18:42
 * @Version 1.0
 */
data class PersonRestMealTime(
    var mealId: Int,
    var updateCount: Int,
    var custId: String,
    var mealName: String? = null
) {
}

data class PersonRestMealTimeRequest(
    var businessId: String = "",
    var campusId: String = "",
    var custId: String = "",
) {
}