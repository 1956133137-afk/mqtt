package com.yannuo.dgcanteen.model

/**
 *
 * @property CAMPUS_ID String 园区ID
 * @property CORP_ID String  合作方ID
 * @property PAYMENT String  原始金额(元)
 * @property BUSINESS_ID String   商家ID
 * @property VPOS_ID String  柜台号
 * @property TXCODE String  ZF0001
 * @property REMARK String  备注
 * @property OFFLINE String  离线订单标识 0：联机支付，1：离线补扣
 */
 class CcbFacePayBean {
    var CAMPUS_ID = ""  //园区ID
    var CORP_ID = ""   //合作方ID
    var PAYMENT = ""   //原始金额
    var BUSINESS_ID = ""  //商家ID
    var VPOS_ID = ""  //柜台号
    var TXCODE = "ZF0001"  //支付交易传：ZF0001
    var REMARK = ""  //备注
    var OFFLINE = ""  //离线订单标识

}