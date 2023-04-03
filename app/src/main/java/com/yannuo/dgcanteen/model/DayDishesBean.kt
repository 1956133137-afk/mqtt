package com.yannuo.dgcanteen.model



data class DayDishesBean(
    val mealId: Int,
    val mealName: String,
    val startTime: String?,
    val endTime: String?,
    val selectedDishesList: List<SelectedDishes>
)

data class SelectedDishes(
    val dishesId: String,
    val dishesName: String,
    val imgUrl: String,
    val price: String,
    val unit: String
)