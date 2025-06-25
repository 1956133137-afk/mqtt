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
    var ACC_NO = ""
    var ACC_TYPE = ""
    var ACC_BAL = ""
    var REMAIN_BAL = ""
    var TRAN_ID = ""
    var PAY_METHOD = ""
    var OFFLINE = ""
    var CONSUMPTION_TYPE = ""
    var SIGN = ""
    var ACC_LIST: MutableList<ACCLIST> = mutableListOf()
    var REMARK = ""
    var TRAN_RESULT = ""
    var discountMsg: String = ""    //优惠信息
    var ERRCODE = ""
    var ERRMSG = ""
    override fun toString(): String {
        return "ResponsePay(RESULT='$RESULT', CAMPUS_ID='$CAMPUS_ID', BUSINESS_ID='$BUSINESS_ID', VPOS_ID='$VPOS_ID', CUST_ID='$CUST_ID', PAYMENT='$PAYMENT', ACTUAL_PAYMENT='$ACTUAL_PAYMENT', CREATE_TIME='$CREATE_TIME', ORDERID='$ORDERID', TRACEID='$TRACEID', ACC_NO='$ACC_NO', ACC_TYPE='$ACC_TYPE', ACC_BAL='$ACC_BAL', REMAIN_BAL='$REMAIN_BAL', TRAN_ID='$TRAN_ID', PAY_METHOD='$PAY_METHOD', OFFLINE='$OFFLINE', CONSUMPTION_TYPE='$CONSUMPTION_TYPE', SIGN='$SIGN', ACC_LIST=$ACC_LIST, REMARK='$REMARK', TRAN_RESULT='$TRAN_RESULT', ERRCODE='$ERRCODE', ERRMSG='$ERRMSG')"
    }


}