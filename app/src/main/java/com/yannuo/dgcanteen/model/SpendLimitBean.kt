package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: 消费限制实体
 * Date: 2023/8/11 15:26
 **/
class SpendLimitBean {
    var campusId: String? = null    //园区ID
    var businessId: String? = null  //商家ID
    var vposId: String? = null      //柜台编号
    var custId: String? = null      //用户编号
    var cidNumber: String? = null    //用户编号
    var payment: String? = null     //支付金额
}