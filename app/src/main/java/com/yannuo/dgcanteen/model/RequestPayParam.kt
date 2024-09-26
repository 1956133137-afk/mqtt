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
    var flag: Int = 1
)

