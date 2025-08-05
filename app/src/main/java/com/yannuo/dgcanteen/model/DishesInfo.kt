package com.yannuo.dgcanteen.model


data class DishesInfo(
    var dishesId:String ,        //菜品ID
    var dishesName:String,      //菜品名称
    var mealId:Int ,             //餐别ID
    var windowId:String?,        //档口ID
    var price: Double,          //菜品单价
    var unit:String,            //菜品单位
    var imgUrl:String,         //菜品图片
    var status:Int,             //菜品上下架情况
    var count:Int  = 0             //产品数量
){
    var categoryName:String? = null
}