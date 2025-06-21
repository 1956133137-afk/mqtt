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
    var isLimit: Boolean = false//是否限购
    var limitSize: Int = 1      //限购份数
    var size: Int = 0           //已订份数
    var dishList: MutableList<DishBean> = mutableListOf()
}

class DishBean {
    var dishId: String = ""
    var dishName: String = ""
    var dishPrice: String = ""
    var dishUnit: String = ""
    var dishCount: Int = 0
    var imgUrl: String = ""
    var windowIdList: String = ""
    var stockNum: String = ""   //库存数
    var description: String = "" //菜品描述
    var orderMealQuota: String = "" //是否限购 1限购 2不限购
    var orderMealQuotaNum: String = ""  //限购数量
    var categoryId: String = "" //菜品类别
}