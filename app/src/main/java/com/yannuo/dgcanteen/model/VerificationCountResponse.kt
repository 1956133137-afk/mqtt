package com.yannuo.dgcanteen.model

class VerificationCountResponse {
    var total: TotalCount = TotalCount()
    var mealList: List<Meal> = ArrayList()
}

class TotalCount {
    var totalOrderNum: String = ""
    var verifyTotalOrderNum: String = ""
    var unVerifyTotalOrderNum: String = ""
}

class Meal {
    var mealId = ""
    var mealName = ""
    var mealOrderNum = ""
    var verifyMealOrderNum = ""
}
