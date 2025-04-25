package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/24 18:06
 **/
class SelectDateBean() {
    var date: String = ""
    var value: String = ""
    var mealList: MutableList<OrderMeal> = mutableListOf()

    constructor(date: String) : this() {
        this.date = date
    }

    constructor(date: String, value: String) : this() {
        this.date = date
        this.value = value
    }
}