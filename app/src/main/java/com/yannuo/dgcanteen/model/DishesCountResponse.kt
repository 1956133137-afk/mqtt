package com.yannuo.dgcanteen.model

class DishesCountResponse {
    var countDishes: List<DishesCount> = ArrayList()
}
class DishesCount {
    var mealId = ""
    var mealName = ""
    var dishes: List<Dishes> = ArrayList()
}

class Dishes {
    var dishesName = ""
    var totalDishesNum = 0
    var verifyDishesNum = 0
}