package com.yannuo.dgcanteen.model

class CountDishesOfWindowBean {
    var campusId: String = ""
    var businessId: String = ""
    var mealId = 0
    var deviceId = ""
}

class CountDishesOfWindowResponse {
    var unVerifyTotal: List<DishesCounts> = arrayListOf()
    var verifyTotal: List<DishesCounts> = arrayListOf()
    var needVerifyTotal: List<DishesCounts> = arrayListOf()
}

class DishesCounts {
    var dishesName = ""
    var dishesNum = 0
    var unit = ""
    var flag = 0
}