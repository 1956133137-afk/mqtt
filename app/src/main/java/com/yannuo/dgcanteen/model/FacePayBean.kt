package com.yannuo.dgcanteen.model


/**
 *
 * @property ACC_LIST List<ACCLIST>  //账户实体数组
 * @property ACC_NO String      //支付账户
 * @property ACC_TYPE String        //账户类型
 * @property ACTUAL_PAYMENT String      //实际支付金额
 * @property CUST_ID String     //用户唯一标识
 * @property ERRCODE String     //错误码
 * @property ERRMSG String      //错误码
 * @property ORDER_ID String        //订单号
 * @property PAYMENT String     //原始金额
 * @property RESULT String      //订单结果
 * @property TRAN_RESULT String     //支付结果
 * @constructor
 */
data class FacePayBean(
    val ACC_LIST: List<ACCLIST>,
    val ACC_NO: String,
    val ACC_TYPE: String,
    val ACTUAL_PAYMENT: String,
    val CUST_ID: String,
    val ERRCODE: String,
    val ERRMSG: String,
    val ORDER_ID: String,
    val PAYMENT: String,
    val RESULT: String,
    val TRAN_RESULT: String
)

/**
 *
 * @property ACC_BAL String //账户余额
 * @property ACC_NO String   //账户ID
 * @property ACC_TYPE String  //账户类型
 * @property PAYMENT String  //支付金额
 * @property TRAN_ID String //交易流水号
 * @constructor
 */
data class ACCLIST(
    val ACC_BAL: String,
    val ACC_NO: String,
    val ACC_TYPE: String,
    val PAYMENT: String,
    val TRAN_ID: String
)