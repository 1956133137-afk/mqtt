package com.yannuo.dgcanteen.model

import com.google.gson.JsonArray

class VerificationResponse {
    var personName: String = ""
//    var verify: MutableMap<String, Verify> = mutableMapOf()
    var verifyDishes: Array<String> = arrayOf()
    var unVerifyDishes: Array<String> = arrayOf()
    var unVerifyWindowName: Array<String> = arrayOf()
}

//class Verify {
//    var dishesList: Array<String> = arrayOf()
//    var windowList: Array<String> = arrayOf()
//}

class CavQueryReceive {
    var personName: String = ""
    var verify: HashMap<String, JsonArray> = hashMapOf()
}

class VerifyReceive {
    var dishes: String = "" //菜品名称、数量
    var window: String = "" //窗口名称 窗口A,窗口B
}