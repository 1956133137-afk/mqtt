package com.yannuo.dgcanteen.model

class PayResultForUI {

    var result : Result = Result.FAIL
    var orderid :String ?= null  //订单号
    var amount :String ?= null   //金额
    var way :String ?= null   //支付方式
    var errormsg :String ?= null

    var cmdty_nm : String ?= null   //商品名称
    var piece :Int = 0     //商品数量
    var timestamp :String ?= null //交易时间

    override fun toString(): String {
        return "PayResultForUI(result=$result, orderid=$orderid, amount=$amount, way=$way, errormsg=$errormsg, cmdty_nm=$cmdty_nm, piece=$piece, timestamp=$timestamp)"
    }

    enum class Result{
        SUCCESS,
        FAIL
    }
}