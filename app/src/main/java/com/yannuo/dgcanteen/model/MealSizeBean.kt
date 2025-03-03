package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/2/19 18:09
 **/
data class MealSizeBean(
    var campusId: String = "",
    var date: String = "",
    var mealId: String = "",
    var custId: String = ""
)

class MealSizeReceive {
    var num: Int = 0
}