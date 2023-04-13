package com.yannuo.dgcanteen.model

class ScanAnalysisBean {

    var RESULT : Result = Result.N      //Y、N
    var CUST_ID :String ?= null         //用户唯一标识
    var CID_NO :String ?= null          //用户编号（学工号）
    var ERRCODE :String ?= null         //错误码
    var ERRMSG :String ?= null          //错误信息

    constructor(code: Int?, msg: String?) {
        this.ERRCODE = code.toString()
        this.ERRMSG = msg
    }

    override fun toString(): String {
        return "ScanAnalysisBean(RESULT=$RESULT, PAYMENT=$CUST_ID, ACTUAL_PAYMENT=$CID_NO, ERRCODE=$ERRCODE, ERRMSG=$ERRMSG)"
    }

    enum class Result{
        Y, N
    }
}