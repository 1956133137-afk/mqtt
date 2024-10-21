package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/23 18:17
 **/
data class OrderDishBean(var orderMealDate: String = "", var mealId: String = "", var businessId: String = "", var campusId: String = "")

class OrderDishReceive {
    var batchPackage: MutableList<OrderSetMeal> = mutableListOf()
    var batchDishes: MutableList<OrderDish> = mutableListOf()
}

class OrderSetMeal {
    var mealId: String = ""     //餐别id
    var mealName: String = ""   //餐别名称

    var packageId: String = ""      //套餐菜品id
    var packageName: String = ""    //套餐菜品名称
    var minPrice: String = ""       //菜品价格
    var imgUrl: String = ""         //菜品图片路径

    var orderMealQuota: String = ""     //是否限购 1-是 2-否
    var orderMealQuotaNum: String = ""  //限购数量

    var categoryId: String = "" //类目id
    var windowIdList: String = ""
}

class OrderDish {
    var mealId: String = ""     //餐别id
    var mealName: String = ""   //餐别名称

    var dishesId: String = ""   //菜品id
    var dishesName: String = "" //菜品名称
    var price: String = ""      //菜品价格
    var unit: String = ""       //菜品单位
    var imgUrl: String = ""     //菜品图片路径
    var status: String = ""     //1-上架 2-下架
    var description: String = ""    //菜品描述

    var orderMealQuota: String = ""     //是否限购 1-是 2-否
    var orderMealQuotaNum: String = ""  //限购数量

    var categoryId: String = "" //类目id
    var windowIdList: String = "" //窗口id "adads,1dadad"
}