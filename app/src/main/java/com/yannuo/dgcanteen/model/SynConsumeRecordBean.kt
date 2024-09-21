package com.yannuo.dgcanteen.model

import com.yannuo.dgcanteen.greendao.entity.AccListTable

class SynConsumeRecordBean {
    var deviceSerialNumber: String = "" //设备序列号
    var businessId: String = ""         //商家Id
    var counterId: String = ""          //柜台号
    var consumptionType: String = ""    //消费类型：1：刷脸，2：扫码，3：刷卡
    var RESULT: String = ""             //订单结果: N：失败，Y：成功
    var CUST_ID: String = ""            //用户唯一标识
    var PAYMENT: String = ""            //原始金额
    var ACTUAL_PAYMENT: String = ""     //实际支付金额
    var ACC_NO: String = ""             //支付账户
    var ACC_BAL: String = ""            //虚拟账户金额
    var ACC_TYPE: String = ""           //账户类型：01-现金账户，02-餐补账户1,03-餐补账户2，04-餐补账户3，05-餐补账户4，06-餐补账户5
    var TRACEID: String = ""            //交易流水号
    var ORDER_ID: String = ""           //订单号
    var TRAN_RESULT: String = ""        //支付结果：1：待支付，2：支付失败，3：支付成功
    var OFFLINE: String = ""            //离线订单标识：0：联机支付，1离线补扣
    var ERRCODE: String = ""            //错误码
    var ERRMSG: String = ""             //错误信息
    var ACCALIAS: String = ""           //账户类型名称
    var ACC_LIST: MutableList<ACCLIST> = mutableListOf() //所有账户信息
    var PAYTIME: String = ""            //支付时间
    var BUSINESS_NAME: String = ""      //商家名称
    var paymentDishesList: MutableList<Dish> = mutableListOf()  //消费的菜品集合
}

data class PaymentDishesList(
    var dishesId: String,       //所关联的菜品Id
    var dishesName: String,     //菜品名称
    var dishesNumber: Int,      //菜品数量
    var dishesPrice: Double     //菜品单价
)

