package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: 调用云打印参数
 * Date: 2025/12/8 14:53
 **/
class PrintTicketBean {
    var custId: String = ""     //用户唯一标识
    var personName: String = "" //用户姓名
    var phone: String = ""      //手机号
    var orderId: String = ""    //订单号
    var campusId: String = ""   //园区编号
    var businessId: String = "" //商家编号
    var deviceId: String = ""   //设备序列号
    var orderTime: String = ""  //下单时间
    var payTime: String = ""    //支付时间
    var payment: String = ""    //支付金额
    var actualPayment: String = ""  //实际金额
    var discountAmt: String = ""    //优化金额
    var dishesList: MutableList<TicketDish> = mutableListOf()
}

class TicketDish {
    var dishesId: String = ""    //菜品Id
    var dishesName: String = ""  //菜品名称
    var price: String = ""      //单价
    var quantity: String = ""   //数量
}