package com.yannuo.dgcanteen.model

import com.google.gson.JsonArray

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/18 17:42
 **/
class OrderVerifyBean {
    var personName: String = ""

    /*核销查询回调参数*/
    var verify: HashMap<String, JsonArray> = hashMapOf()

    /*订餐核销回调参数*/
    var verifySuccessDishes: MutableList<VerifyDish> = mutableListOf()
    var verifyFail: VerifyFailure = VerifyFailure()
    var unVerifyDishes: MutableList<VerifyDish> = mutableListOf()
    var unVerifyWindowName: Array<String> = arrayOf()
}

class VerifyFailure {
    var verifyFailDishes: MutableList<VerifyDish> = mutableListOf()
    var ErrorMessage: String = ""
}

class VerifyDish {
    var dishesName: String = ""
    var unit: String = ""
    var price: String = ""
    var dishesNum: String = ""
}

class OrderVerify {
    var verifyType: Int = -1 //0-核销成功 1-核销失败 2-待核销
    var verifyMsg: String = ""
    var verifyDishList: MutableList<VerifyDish> = mutableListOf()
}
