package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/13 10:36
 * @Version 1.0
 */
class QueryAllowanceRequest {
    var businessId: String = ""
    var campusId: String = ""
    var orderId: String = ""

    override fun toString(): String {
        return "QueryAllowanceRequest(businessId='$businessId', campusId='$campusId', orderId='$orderId')"
    }
}

class QueryAllowanceResponse {
    var mealName: String = ""
    var standardName: String = ""
    var subsidyMoney: String = ""
    var price: Double = 0.0
    var personName: String = ""
    var standardNum: Int = 0
    var isAllowance: String = ""
    var actualPayment: Double = 0.0
    var leftOfTimes: Int = 0

    override fun toString(): String {
        return "QueryAllowanceResponse(mealName='$mealName', standardName='$standardName', subsidyMoney='$subsidyMoney', price=$price, personName='$personName', standardNum=$standardNum, isAllowance='$isAllowance', actualPayment=$actualPayment, leftOfTimes=$leftOfTimes)"
    }

}