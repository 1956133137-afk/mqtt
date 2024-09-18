package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/21 9:17
 **/
class ResponsePay {
    var RESULT = "N"
    var CAMPUS_ID = ""
    var BUSINESS_ID = ""
    var VPOS_ID = ""
    var CUST_ID = ""
    var PAYMENT = ""
    var ACTUAL_PAYMENT = ""
    var CREATE_TIME = ""
    var ORDERID = ""
    var TRACEID = ""
    var ACC_TYPE = ""
    var ACC_NO = ""
    var ACC_BAL = ""
    var TRAN_ID = ""
    var PAY_METHOD = ""
    var OFFLINE = ""
    var SIGN = ""
    var ACC_LIST: MutableList<ACCLIST> = mutableListOf()
    var REMARK = ""
    var TRAN_RESULT = ""
    var ERRCODE = ""
    var ERRMSG = ""
}