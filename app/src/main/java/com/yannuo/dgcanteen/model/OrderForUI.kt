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
    var vposId: String = ""
    var custId: String = ""     //用户编号
    var custName: String = ""   //用户姓名
    var orderType: String = ""      //登录方式
    var orderContent: String = ""   //登录内容
    var orderTime: String = ""  //订餐时间
    var timestamp: String = ""  //时间戳
    var ccbToken: String = ""   //token信息
    var offline: String = ""    //离线状态 0在线 1离线
    var errCode: String = ""    //错误代码
    var errMsg: String = ""     //错误信息

    constructor(parcel: Parcel) : this() {
        result = parcel.readString().toString()
        campusId = parcel.readString().toString()
        businessId = parcel.readString().toString()
        vposId = parcel.readString().toString()
        custId = parcel.readString().toString()
        custName = parcel.readString().toString()
        orderType = parcel.readString().toString()
        orderContent = parcel.readString().toString()
        orderTime = parcel.readString().toString()
        timestamp = parcel.readString().toString()
        ccbToken = parcel.readString().toString()
        offline = parcel.readString().toString()
        errCode = parcel.readString().toString()
        errMsg = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(result)
        parcel.writeString(campusId)
        parcel.writeString(businessId)
        parcel.writeString(vposId)
        parcel.writeString(custId)
        parcel.writeString(custName)
        parcel.writeString(orderType)
        parcel.writeString(orderContent)
        parcel.writeString(orderTime)
        parcel.writeString(timestamp)
        parcel.writeString(ccbToken)
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