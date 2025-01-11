package com.yannuo.dgcanteen.model

class CountDishesOfWindowBean {
    var campusId: String = ""
    var businessId: String = ""
    var mealId = 0
    var deviceId = ""
}

class CountDishesOfWindowResponse {
    var dcTotalPersonNum = ""   //总核销人数
    var dcVerifyPersonNum = ""  //已核销人数
    var dcUnverifyPersonNum = ""    //未核销人数
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