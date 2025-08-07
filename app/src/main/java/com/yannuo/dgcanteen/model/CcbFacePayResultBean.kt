package com.yannuo.dgcanteen.model

/**
 *
 * @property ACCALIAS String  账户别名
 * @property ACC_BAL String   虚账户余额
 * @property ACC_NO String    支付账户
 * @property ACC_TYPE String  账户类型
 * @property ACTUAL_PAYMENT String   实际支付金额
 * @property BUSINESS_NAME String    商家名称
 * @property CUST_ID String    用户唯一标识
 * @property CUST_NAME String   用户名字
 * @property DISCOUNTAMT String   优惠价格
 * @property DISCOUNTMSG String   优惠名称
 * @property OFFLINE String   离线订单标识
 * @property ORDER_ID String  订单号
 * @property PAYMENT String    原始金额
 * @property PAYTIME String   支付时间
 * @property RESULT String  订单结果
 * @property TRACEID String  交易流水号
 * @property TRAN_RESULT String  支付结果
 * @property ERRCODE: String  错误码
 * @property ERRMSG: String  错误信息
 * @property ACC_LIST List<ACCLIST>   账户实体数组
 * @constructor
 */
class CcbFacePayResultBean {
    var ACC_NO: String = ""
    var ACC_BAL: String = ""
    var ACC_TYPE: String = ""
    var ACTUAL_PAYMENT: String = ""
    var BUSINESS_NAME: String = ""
    var CUST_ID: String = ""
    var CUST_NAME: String = ""
    var REMAIN_BAL: String = ""
    var DISCOUNTAMT: String = ""
    var DISCOUNTMSG: String = ""
    var OFFLINE: String = ""
    var ORDER_ID: String = ""
    var PAYMENT: String = ""
    var PAYTIME: String = ""
    var RESULT: String = ""
    var TRACEID: String = ""
    var TRAN_RESULT: String = ""
    var ERRCODE: String = ""
    var ERRMSG: String = ""
    var ACC_LIST: MutableList<ACCLIST> = mutableListOf()
    override fun toString(): String {
        return "CcbFacePayResultBean(ACC_NO='$ACC_NO', ACC_BAL='$ACC_BAL', REMAIN_BAL='$REMAIN_BAL' ACC_TYPE='$ACC_TYPE', ACTUAL_PAYMENT='$ACTUAL_PAYMENT', BUSINESS_NAME='$BUSINESS_NAME', CUST_ID='$CUST_ID', CUST_NAME='$CUST_NAME', DISCOUNTAMT='$DISCOUNTAMT', DISCOUNTMSG='$DISCOUNTMSG', OFFLINE='$OFFLINE', ORDER_ID='$ORDER_ID', PAYMENT='$PAYMENT', PAYTIME='$PAYTIME', RESULT='$RESULT', TRACEID='$TRACEID', TRAN_RESULT='$TRAN_RESULT', ERRCODE='$ERRCODE', ERRMSG='$ERRMSG', ACC_LIST=$ACC_LIST)"
    }

}

/**
 *
 * @property ACC_BAL String 账户余额
 * @property ACC_NO String   账户ID
 * @property ACC_TYPE String  账户类型
 * @property PAYMENT String  支付金额
 * @property TRAN_ID String 交易流水号
 * @constructor
 */
class ACCLIST(
    var ACC_BAL: String = "",
    var ACC_NO: String = "",
    var ACC_TYPE: String = "",
    var PAYMENT: String = "",
    var TRAN_ID: String = ""
)

