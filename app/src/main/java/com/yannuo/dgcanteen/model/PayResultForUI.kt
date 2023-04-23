package com.yannuo.dgcanteen.model

class PayResultForUI {

    var result : Result = Result.FAIL
    var cust_name: String ?= null  //用户名
    var custId   : String? = null//智慧食堂用户唯一标识（建行平台）
    var orderid :String ?= null  //订单号
    var payment :String ?= null   //金额
    var acc_no: String ?= null  //支付账户
    var acc_bal: String ?= null  //虚拟账户余额
    var way :String ?= null   //支付方式
    var errormsg :String ?= null

    var cmdty_nm : String ?= null   //商品名称
    var piece :Int = 0     //商品数量
    var timestamp :String ?= null //交易支付时间
    var traceid: String ?= null  //交易流水号
    var dishes :MutableList<DishesInfo> ?=null //菜品列表

    override fun toString(): String {
        return "PayResultForUI(result=$result, orderid=$orderid, payment=$payment, way=$way, errormsg=$errormsg, cmdty_nm=$cmdty_nm, piece=$piece, timestamp=$timestamp)"
    }

    enum class Result{
        SUCCESS,
        FAIL
    }
}