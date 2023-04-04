package com.yannuo.dgcanteen.model

class ScanQrResultBean {

    var RESULT : Result = Result.N      //Y、N
    var PAYMENT :String ?= null         //原始金额
    var ACTUAL_PAYMENT :String ?= null   //实际支付金额
    var ACC_NO :String ?= null           //支付账号
    var ACC_TYPE :String ?= null         //账号类型
    var ORDER_ID :String ?= null         //订单号
    var ACC_BAL :String ?= null          //虚账户余额
    var ERRCODE :String ?= null         //错误码
    var ERRMSG :String ?= null          //错误信息

    var code: Int? = null
    var msg: String? = null

    constructor(code: Int?, msg: String?) {
        this.code = code
        this.msg = msg
    }

    override fun toString(): String {
        return "ScanQrResultBean(RESULT=$RESULT, PAYMENT=$PAYMENT, ACTUAL_PAYMENT=$ACTUAL_PAYMENT, ACC_NO=$ACC_NO, ACC_TYPE=$ACC_TYPE, ORDER_ID=$ORDER_ID, ACC_BAL=$ACC_BAL, ERRCODE=$ERRCODE, ERRMSG=$ERRMSG)"
    }



    enum class Result{
        Y, N
    }
}