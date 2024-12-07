package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/12 15:33
 * @Version 1.0
 */
data class MealRestTimeRequest(
    var businessId: String,
    var campusId: String,
    var custId: String,
    var mealId: Int
//    userTypeId: String
) {
}

data class MealRestTimeReceive(
    var useTimes: Int
){}