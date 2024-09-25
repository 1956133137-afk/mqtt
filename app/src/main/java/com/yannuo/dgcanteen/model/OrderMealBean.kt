package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/23 18:17
 **/
data class OrderMealBean(var campusId: String = "", var businessId: String = "")

class OrderMealReceive {
    var orderMealId: String = ""
    var campusId: String = ""
    var businessId: String = ""
    var orderControl: String = ""   //是否根据餐别控制
    var delFlag: String = ""        //1-存在，2-删除
    var orderMealList: MutableList<OrderMeal> = mutableListOf()
}

class OrderMeal {
    var mealId: String = ""     //餐别id
    var mealName: String = ""   //餐别名称
    var startTime: String = ""  //开始时间 HH:mm:ss
    var endTime: String = ""    //结束数据 HH:mm:ss
    var orderMealDay: String = ""  //开餐星期"1, 2, 3, 4, 5, 6, 7"
    var delFlag: String = ""    //是否删除 1-是 2-否

    var orderQuota: String = ""     //是否限购 1-是 2-否
    var orderQuotaNum: String = ""  //限购数量

    var orderDelineDate: String = ""    //截止多少天前 0-7
    var orderDelineTime: String = ""    //截止时间 HH:mm:ss

    var isLimitDeliveryTime: String = ""    //限制配送时间 1-开 2-关
    var limitDeliveryStart: String = ""     //限制开始时间 HH:mm
    var limitDeliveryEnd: String = ""       //限制结束时间 HH:mm
}