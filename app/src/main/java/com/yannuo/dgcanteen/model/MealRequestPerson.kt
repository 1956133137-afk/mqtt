package com.yannuo.dgcanteen.model

data class MealRequestPerson(
    var campusId: String = "",
    var businessId: String = ""
)

class MealBean(
    var mealId: Int,
    var mealName: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var businessName: String = "",
    var businessId: String = "",
    var campusId: String = ""
)
