package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/23 11:38
 **/
class OrderForUI() : Parcelable {
    var result: String = "N"    //订餐结果
    var campusId: String = ""
    var businessId: String = ""
    var businessName: String = ""
    var deviceId: String = ""
    var vposId: String = ""
    var corpId: String = ""
    var custId: String = ""     //用户编号
    var custName: String = ""   //用户姓名
    var accBal: String = ""     //用户余额
    var orderType: String = ""  //登录方式 1-刷脸 2-扫码 3-刷卡
    var orderContent: String = ""   //登录内容
    var orderTime: String = ""  //订餐时间
    var timestamp: String = ""  //时间戳
    var ccbToken: String = ""   //token信息
    var orderDate: String = ""  //订餐日期
    var mealId: String = ""     //餐别id
    var mealName: String = ""   //餐别名称
    var distribute: String = "0"//配送方式 1-配送 2-自提
    var deliveryTime: String = ""   //配送时间
    var phone: String = ""      //联系电话
    var address: String = ""    //配送地址
    var remark: String = ""     //备注信息
    var payment: String = ""    //订单金额
    var actualPayment: String = ""  //实付金额
    var discountPayment: String = ""//优惠金额
    var isDeviceDiscount: String = "0"  //优惠规则 0-后台计算优惠 1-设备端计算优惠
    var payTime: String = ""    //支付时间
    var orderId: String = ""    //订单Id
    var verifyFlag: String = "" //核销状态
    var verificationCode: String = ""        //保温箱二维码
    var packagingFee: String = "0.00"   //打包费用
    var deliveryFee: String = "0.00"    //配送费用
    var dishList: MutableList<DishBean> = mutableListOf()
    var menuList: MutableList<DateMenu> = mutableListOf()
    var offline: String = ""    //离线状态 0在线 1离线
    var errCode: String = ""    //错误代码
    var errMsg: String = ""     //错误信息

    constructor(parcel: Parcel) : this() {
        result = parcel.readString().toString()
        campusId = parcel.readString().toString()
        businessId = parcel.readString().toString()
        businessName = parcel.readString().toString()
        vposId = parcel.readString().toString()
        corpId = parcel.readString().toString()
        custId = parcel.readString().toString()
        custName = parcel.readString().toString()
        accBal = parcel.readString().toString()
        orderType = parcel.readString().toString()
        orderContent = parcel.readString().toString()
        orderTime = parcel.readString().toString()
        timestamp = parcel.readString().toString()
        ccbToken = parcel.readString().toString()
        orderDate = parcel.readString().toString()
        mealId = parcel.readString().toString()
        mealName = parcel.readString().toString()
        distribute = parcel.readString().toString()
        deliveryTime = parcel.readString().toString()
        phone = parcel.readString().toString()
        address = parcel.readString().toString()
        remark = parcel.readString().toString()
        payment = parcel.readString().toString()
        actualPayment = parcel.readString().toString()
        payTime = parcel.readString().toString()
        orderId = parcel.readString().toString()
        offline = parcel.readString().toString()
        errCode = parcel.readString().toString()
        errMsg = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(result)
        parcel.writeString(campusId)
        parcel.writeString(businessId)
        parcel.writeString(businessName)
        parcel.writeString(vposId)
        parcel.writeString(corpId)
        parcel.writeString(custId)
        parcel.writeString(custName)
        parcel.writeString(accBal)
        parcel.writeString(orderType)
        parcel.writeString(orderContent)
        parcel.writeString(orderTime)
        parcel.writeString(timestamp)
        parcel.writeString(ccbToken)
        parcel.writeString(orderDate)
        parcel.writeString(mealId)
        parcel.writeString(mealName)
        parcel.writeString(distribute)
        parcel.writeString(deliveryTime)
        parcel.writeString(phone)
        parcel.writeString(address)
        parcel.writeString(remark)
        parcel.writeString(payment)
        parcel.writeString(actualPayment)
        parcel.writeString(payTime)
        parcel.writeString(orderId)
        parcel.writeString(offline)
        parcel.writeString(errCode)
        parcel.writeString(errMsg)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderForUI> {
        override fun createFromParcel(parcel: Parcel): OrderForUI {
            return OrderForUI(parcel)
        }

        override fun newArray(size: Int): Array<OrderForUI?> {
            return arrayOfNulls(size)
        }
    }
}