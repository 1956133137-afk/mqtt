package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/7/19 10:41
 **/
open class RequestPayBase {
    var businessId: String = ""
    var businessName: String = ""
    var orderId: String = ""
    var campusId: String = ""
    var corpId: String = ""
    var vposId: String = ""
    var deviceId: String = ""
    var custId: String = ""
    var payment: String = ""
    var actualPayment: String = ""
    var paymentDishes: MutableList<Dish> = mutableListOf()
    var sessionId: String = ""
    var signTime: String = ""
    var offline: String = ""    //0在线 1离线
    var verifyFlag: String = "" //订餐字段 1-需要核销 2-不需要核销
    override fun toString(): String {
        return "RequestPayBase(businessId='$businessId', businessName='$businessName', orderId='$orderId', campusId='$campusId', corpId='$corpId', vposId='$vposId', deviceId='$deviceId', custId='$custId', payment='$payment', actualPayment='$actualPayment', paymentDishes=$paymentDishes, sessionId='$sessionId', signTime='$signTime', offline='$offline', verifyFlag='$verifyFlag')"
    }


}

class Dish {
    var dishesId: String = ""
    var dishesName: String = ""
    var dishesNumber: String = ""
    var dishesPrice: String = ""
}

class CodePayBean : RequestPayBase() {
    var qrCode: String = ""
}

class CardPayBean : RequestPayBase() {
    var cardId: String = ""
}

data class RequestPay(
    var encryptedData: String = "",
    var flag: Int = 1,
    var orderFlag: String = "",
    var isAllowance: String = "",
    var actualMealId: Int? = null,
    var mealId: Int? = null,
    var userMealId: Int? = null
)

