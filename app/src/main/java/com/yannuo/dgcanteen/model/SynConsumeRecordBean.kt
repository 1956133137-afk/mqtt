package com.yannuo.dgcanteen.model



class SynConsumeRecordBean{
    var deviceSerialNumber :String ?= null //设备序列号
    var businessId :String ?= null            //商家Id
    var counterId :String ?= null          //柜台号
    var RESULT :String ?= null             //订单结果: N：失败，Y：成功
    var CUST_ID :String ?= null            //用户唯一标识
    var PAYMENT :Double ?= null            //原始金额
    var ACTUAL_PAYMENT :Double ?= null     //实际支付金额
    var ACC_NO :String ?= null             //支付账户
    var ACC_BAL :Double ?= null            //虚拟账户金额
    var ACC_TYPE :Int ?= null              //账户类型：01-现金账户，02-餐补账户1,03-餐补账户2，04-餐补账户3，05-餐补账户4，06-餐补账户5
    var TRACEID :String ?= null            //交易流水号
    var ORDER_ID :String ?= null           //订单号
    var TRAN_RESULT :Int ?= null           //支付结果：1：待支付，2：支付失败，3：支付成功
    var OFFLINE :Int ?= null               //离线订单标识：0：联机支付，1离线补扣
    var ERRCODE :String ?= null            //错误码
    var ERRMSG :String ?= null             //错误信息
    var ACCALIAS :String ?= null           //账户类型名称
    var PAYTIME :String ?= null            //支付时间
    var BUSINESS_NAME :String ?= null      //商家名称
    lateinit var paymentDishesList : MutableList<PaymentDishesList>  //消费的菜品集合

}


data class PaymentDishesList(
    var dishesId :String,       //所关联的菜品Id
    var dishesName :String,     //菜品名称
    var dishesNumber :Int,      //菜品数量
    var dishesPrice :Double     //菜品单价
)

