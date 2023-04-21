package com.yannuo.dgcanteen.model

class ScanQueryBean {

    var RESULT : Result = Result.N      //Y、N
    var CUST_ID :String ?= null         //用户唯一标识
    var PAYMENT :String ?= null         //原始金额
    var ACTUAL_PAYMENT :String ?=null      //实际支付金额
    var TRAN_RESULT :String ?=null         //支付结果
    var ERRCODE :String ?= null         //错误码
    var ERRMSG :String ?= null          //错误信息

    enum class Result{
        Y, N
    }
}