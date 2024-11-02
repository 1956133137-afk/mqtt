package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 15:02
 **/
class DateMenu {
    var date: String = ""
    var value: String = ""
    var mealList: MutableList<MealMenu> = mutableListOf()
}

class MealMenu {
    var mealId: String = ""
    var mealName: String = ""
    var startTime: String = ""  //开始时间 HH:mm:ss
    var endTime: String = ""    //结束数据 HH:mm:ss
    var dishList: MutableList<DishBean> = mutableListOf()
}

class DishBean {
    var dishId: String = ""
    var dishName: String = ""
    var dishPrice: String = ""
    var dishUnit: String = ""
    var dishCount: Int = 0
    var imgUrl: String = ""
}