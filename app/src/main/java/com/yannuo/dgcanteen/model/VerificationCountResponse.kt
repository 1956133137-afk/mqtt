package com.yannuo.dgcanteen.model

class VerificationCountResponse {
    var total: TotalCount = TotalCount()
    var mealList: List<Meal> = ArrayList()
}

class TotalCount {
    var dcPerson: String = ""
    var verifyPerson: String = ""
}

class Meal {
    var mealName = ""
    var orderMealPerson = ""
    var verifyMealPerson = ""
}
