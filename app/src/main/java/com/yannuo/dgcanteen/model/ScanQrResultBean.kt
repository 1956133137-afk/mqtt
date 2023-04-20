package com.yannuo.dgcanteen.model

class ScanQrResultBean {

    var RESULT : Result = Result.N      //Y、N
    var PAYMENT :String ?= null         //原始金额
    var ACTUAL_PAYMENT :String ?= null   //实际支付金额
    var ACC_NO :String ?= null           //支付账号
    var ACC_TYPE :String ?= null         //账号类型
    var ORDERID :String ?= null         //订单号
    var ACC_BAL :String ?= null          //虚账户余额
    var REMAIN_BAL :String ?= null
    var ERRCODE :String ?= null         //错误码
    var ERRMSG :String ?= null          //错误信息

    constructor(code: Int, msg: String?) {
        this.ERRCODE = code.toString()
        this.ERRMSG = msg
    }

    constructor(code: String?, msg: String?) {
        this.ERRCODE = code.toString()
        this.ERRMSG = msg
    }

    enum class Result{
        Y, N
    }
}