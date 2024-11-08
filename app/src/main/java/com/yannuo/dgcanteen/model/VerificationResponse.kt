package com.yannuo.dgcanteen.model

class VerificationResponse {
    var personName: String = ""
    var verify: MutableMap<String, Verify> = mutableMapOf()
    var verifyDishes: Array<String> = arrayOf()
    var unVerifyDishes: Array<String> = arrayOf()
    var unVerifyWindowName: Array<String> = arrayOf()
}

class Verify {
    var dishesList: Array<String> = arrayOf()
    var windowList: Array<String> = arrayOf()
}
