package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/21 14:01
 **/
class PayForUI {
    var result: String = "N"    //订单结果  Y-成功 N-失败
    var businessId = ""         //商家编号
    var businessName = ""       //商家名称
    var campusId = ""           //园区Id
    var corpId = ""             //合作方Id
    var vposId = ""             //柜台编号
    var deviceId = ""           //设备序列号
    var custId = ""             //用户唯一标识
    var username = ""           //用户姓名
    var accNo = ""              //支付账号
    var accBal = ""             //账户金额
    var accType = ""            //账户类型: 01-现金账户，02-餐补账户1,03-餐补账户2，04-餐补账户3，05-餐补账户4，06-餐补账户5
    var accList = ""            //账户类型名称
    var orderId = ""            //订单号
    var traceId = ""            //交易流水号
    var payType = ""            //支付类型  1-刷脸 2-扫码 3-刷卡
    var payContent = ""         //支付内容  payType: 1-刷脸 2-二维码 3-卡号
    var payment = ""            //订单金额
    var actualPayment = ""      //实际支付金额
    var payTime = ""            //支付时间 yyyy-MM-dd HH:mm:ss
    var sessionId = ""          //订单唯一随机标记位
    var signTime = ""           //交易时间 yyyyMMddHHmmss
    var offline = ""            //离线标记 0在线 1离线
    var paymentDishes: MutableList<Dish> = mutableListOf()
    var errCode = ""            //错误码
    var errMsg = ""             //错误信息
}