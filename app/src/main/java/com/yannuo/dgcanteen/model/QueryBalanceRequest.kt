package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/3 16:11
 * @Version 1.0
 */
class QueryBalanceRequest {
    var campusId: String = ""
    var cardId: String = ""
    var qrCode: String = ""

    override fun toString(): String {
        return "QueryBalanceRequest(campusId='$campusId', cardId='$cardId', qrCode='$qrCode')"
    }
}

class QueryBalanceResponse {
    var RESULT: String = "N"
    var CAMPUS_ID: String = ""
    var CUST_ID: String = ""
    var ERRCODE: String = ""
    var ERRMSG: String = ""
    var REMAIN_BAL: String = ""
    var personName: String = ""
    var personNumber: String = ""
    var sex: Int = 3
    var payCount = 0.0
    var dailyLimit = 0.0
    var ACC_DATA = mutableListOf<Acc>()

    override fun toString(): String {
        return "QueryBalanceResponse(RESULT='$RESULT', CAMPUS_ID='$CAMPUS_ID', CUST_ID='$CUST_ID', ERRCODE='$ERRCODE', ERRMSG='$ERRMSG', REMAIN_BAL='$REMAIN_BAL', personName='$personName', personNumber='$personNumber', sex=$sex, payCount=$payCount, dailyLimit=$dailyLimit, ACC_DATA=$ACC_DATA)"
    }
}

class Acc {
    var ACC_NO: String = ""
    var ACC_TYPE: String = ""
    var MONEY: String = ""
}