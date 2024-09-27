package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/27 10:24
 **/
class InsertOrderBean {
    var orderId: String = ""            //订单id
    var campusId: String = ""           //园区id
    var businessId: String = ""         //商家号id
    var custId: String = ""             //用户id
    var personName: String = ""         //订餐用户姓名
    var phone: String = ""              //订餐用户手机号
    var mealId: String = ""             //餐别id
    var orderType: String = ""          //订单类型（1-配送，2-自提）
    var address: String = ""            //配送地址
    var deliveryTime: String = ""       //配送时间
    var pickupTime: String = ""         //自提时间
    var remark: String = ""             //备注
    var isPackage: String = ""          //是否打包
    var packagingFee: String = ""       //打包费
    var deliveryFee: String = ""        //配送费
    var payment: String = ""            //订单金额
    var discountPayment: String = ""    //优惠金额
    var actualPayment: String = ""      //实际支付金额
    var orderTime: String = ""          //下单时间
    var payTime: String = ""            //支付时间
    var tranId: String = ""             //支付流水号
    var tranResult: String = ""         //支付结果
    var orderStatus: String = ""        //订单状态
    var delFlag: String = ""            //1-存在，2-删除
    var dcOrderDishesList: MutableList<InsertDish> = mutableListOf()  //订单菜品
}

class InsertDish {
    var orderDishesId: String = ""  //订餐id
    var orderId: String = ""        //订单id
    var dishesId: String = ""       //菜品id
    var dishesName: String = ""     //菜品名称
    var dishesNum: String = ""      //菜品数量
    var dishesPrice: String = ""    //菜品价格
    var imgUrl: String = ""        //菜品图片
    var delFlag: String = ""        //1-存在，2-删除
}

class InsertOrderReceive {
    var orderId: String = ""
}