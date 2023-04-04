package com.yannuo.dgcanteen.model

 class CcbScanPayBean {
    var CAMPUS_ID = ""        //园区ID
    var CORP_ID = ""          //合作方ID
    var TXCODE = ""           //交易码 PAY003
    var ccbSafeParam = ""     //加密参数串
    var BUSINESS_ID = ""      //商家编号
    var VPOS_ID = ""          //柜台编号
    var PAYMENT = ""          //支付金额
    var ACTUAL_PAYMENT = ""   //实际支付金额
    var COUPON_INFO = ""      //优惠信息描述
    var ACC_NOS = ""          //用户可使用的账户
    var QR_CODE = ""          //付款码
    var CUST_ID = ""          //用户ID
    var ORDER_ID = ""         //订单编号
    var OFFLINE = ""          //离线标识   0 , 1
    var SIGN_TIME = ""        //离线签单时间 yyyyMMddHHmmss
}