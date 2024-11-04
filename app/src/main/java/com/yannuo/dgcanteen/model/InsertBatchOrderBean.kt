package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/4 11:39
 **/
class InsertBatchOrderBean {
    var campusId: String = ""           //园区id
    var businessId: String = ""         //商家id
    var custId: String = ""             //用户id
    var totalPackagingFee: String = ""  //打包费用 0.00
    var totalPayment: String = ""       //总订单金额 0.00
    var actualTotalPayment: String = "" //实际总订单金额 0.00
    var orderDetail: MutableList<OrderDetail> = mutableListOf()
}

class OrderDetail {
    var personName: String = ""         //用户姓名
    var phone: String = ""              //用户手机号
    var mealDate: String = ""           //餐别日期 yyyy-MM-dd
    var mealId: String = ""             //餐别id
    var isPackage: String = ""          //是否打包 1-是 2-否
    var packagingFee: String = ""       //打包费用 0.00
    var deliveryFee: String = ""        //配送费用 0.00
    var payment: String = ""            //订单金额 0.00
    var actualPayment: String = ""      //实际订单金额 0.00
    var orderType: String = ""          //订单类型(1-配送，2-自提)
    var addressPersonName: String = ""  //自定义姓名
    var addressTel: String = ""         //配送电话
    var address: String = ""            //配送地址
    var dcOrderDishesList: MutableList<OrderDishes> = mutableListOf()   //菜品订单
    var dcOrderPackageList: MutableList<String> = mutableListOf()   //套餐订单
}

class OrderDishes {
    var windowIdList: String = ""       //属于窗口
    var categoryId: String = ""         //类目id
    var dishesId: String = ""           //菜品id
    var dishesName: String = ""         //菜品名称
    var unit: String = ""               //菜品单位
    var dishesPrice: String = ""        //菜品价格
    var dishesNum: String = ""          //菜品数量
    var imgUrl: String = ""             //菜品url
    var status: String = ""             //状态
    var orderMealQuota: String = ""     //是否限购 1-是 2-否
    var orderMealQuotaNum: String = ""  //限购数量 0
}

class InsertBatchOrderReceive {
    var pOderId: String = ""
}