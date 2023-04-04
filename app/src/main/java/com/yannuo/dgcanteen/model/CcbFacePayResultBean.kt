package com.yannuo.dgcanteen.model


///**
// *
// * @property ACC_LIST List<ACCLIST>  //账户实体数组
// * @property ACC_NO String      //支付账户
// * @property ACC_TYPE String        //账户类型
// * @property ACTUAL_PAYMENT String      //实际支付金额
// * @property CUST_ID String     //用户唯一标识
// * @property ERRCODE String     //错误码
// * @property ERRMSG String      //错误码
// * @property ORDER_ID String        //订单号
// * @property PAYMENT String     //原始金额
// * @property RESULT String      //订单结果
// * @property TRAN_RESULT String     //支付结果
// * @constructor
// */
//data class FacePayBean(
//    val ACC_LIST: List<ACCLIST>,
//    val ACC_NO: String,
//    val ACC_TYPE: String,
//    val ACTUAL_PAYMENT: String,
//    val CUST_ID: String,
//    val ERRCODE: String,
//    val ERRMSG: String,
//    val ORDER_ID: String,
//    val PAYMENT: String,
//    val RESULT: String,
//    val TRAN_RESULT: String
//)



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
class CcbFacePayResultBean{
    var ACCALIAS: String ?= null
    var ACC_BAL: String ?= null
    var ACC_NO: String ?= null
    var ACC_TYPE: String ?= null
    var ACTUAL_PAYMENT: String ?= null
    var BUSINESS_NAME: String ?= null
    var CUST_ID: String ?= null
    var CUST_NAME: String ?= null
    var DISCOUNTAMT: String ?= null
    var DISCOUNTMSG: String ?= null
    var OFFLINE: String ?= null
    var ORDER_ID: String ?= null
    var PAYMENT: String ?= null
    var PAYTIME: String ?= null
    var RESULT: String ?= null
    var TRACEID: String ?= null
    var TRAN_RESULT: String ?= null
    var ERRCODE: String ?= null
    var ERRMSG: String ?= null
    var ACC_LIST: String ?= null



    /**
     *
     * @property ACC_BAL String 账户余额
     * @property ACC_NO String   账户ID
     * @property ACC_TYPE String  账户类型
     * @property PAYMENT String  支付金额
     * @property TRAN_ID String 交易流水号
     * @constructor
     */
   inner class ACCLIST(
        var ACC_BAL: String,
        var ACC_NO: String,
        var ACC_TYPE: String,
        var PAYMENT: String,
        var TRAN_ID: String
    )
}

