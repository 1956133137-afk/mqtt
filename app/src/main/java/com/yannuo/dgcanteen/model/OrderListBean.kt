package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: 查询订餐列表
 * Date: 2024/11/14 18:02
 **/
class OrderListBean {
    var campusId: String = ""   //园区id
    var custId: String = ""     //用户唯一标识
    var mealDate: String = ""   //核销日期
    var page: String = ""       //第几页
    var pageSize: String = ""   //条数

    //
    var tranResult: String = ""
    var orderStatus: String = ""

    // 批量
    var batchTranResult: MutableList<String> = mutableListOf()  //支付结果 1-待支付,2-支付失败,3-支付成功,4-已退款,5-已关闭,6-申请退款中,7-部分退款
    var batchOrderStatus: MutableList<String> = mutableListOf() //订单状态 1-处理中,2-配送中,3-已完成配送,4-待核销,5-完成自提,6-已关闭,7-待配送,8-退款中,9-已退款,10-部分退款 11-已部分核销
}

class OrderListReceive {
    var page: String = ""           //查询页码
    var pageSize: String = ""       //查询条数
    var totalPage: String = ""      //总页数
    var totalRecord: String = ""    //总条数
    var list: MutableList<Order> = mutableListOf()
}

class Order {
    var campusId: String = ""       //园区id
    var businessId: String = ""     //商家id
    var businessName: String = ""   //商家名称
    var custId: String = ""         //用户唯一标识
    var pOrderId: String = ""       //母订单号
    var orderId: String = ""        //订单号
    var orderType: String = ""      //订单类型(1-配送，2-自提)
    var orderTime: String = ""      //订单时间
    var orderStatus: String = ""    //订单状态
    var tranResult: String = ""     //支付结果
    var payment: String = ""        //订单金额
    var packagingFee: String = "0.00"   //打包费用
    var deliveryFee: String = "0.00"    //配送费用
    var actualPayment: String = ""  //实付金额
    var refundPayment: String = "0.00"  //退款金额
    var mealDate: String = ""       //餐别日期
    var mealId: String = ""         //餐别id
    var mealName: String = ""       //餐别名称
    var startTime: String = ""      //餐别开始时间
    var endTime: String = ""        //餐别结束时间
    var phone: String = ""          //手机号
    var address: String = ""        //配送地址
    var dcOrderDishesList: MutableList<DcOrderDishes> = mutableListOf()
//    var dcOrderPackageList: MutableList<String> = mutableListOf()
    var updateTime: String = ""     //订单更新时间
}

class DcOrderDishes {
    var dishesId: String = ""       //菜品id
    var dishesName: String = ""     //菜品名称
    var dishesNum: String = ""      //菜品数量
    var dishesPrice: String = ""    //菜品价格
    var unit: String = ""           //菜品单位
    var imgUrl: String = ""         //菜品url
    var windowIdList: String = ""   //窗口列表
    var description: String = ""    //菜品详情
    var isVerification: String = "" //核销状态 1-未核销 2-已核销
    var dishesRefundNum: String = "0"   //退款数量
}