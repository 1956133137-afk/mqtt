package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

/**
 * 返回UI类 成功或者失败
 * @property
 */
class SimpleForUI() :Parcelable {
    var custName  :String ?= ""  //姓名
    var payment = 0.0F  //支付金额
    var accNo = ""      //支付账户
    var timestamp = ""  //支付时间
    var tranId = ""     //流水号
    var orderId = ""    //订单号
    var errorMsg = ""   //错误信息
    var acc_bal: String ?= ""  //虚拟账户余额

    constructor(parcel: Parcel) : this() {
        custName = parcel.readString()
        payment = parcel.readFloat()
        accNo = parcel.readString().toString()
        timestamp = parcel.readString().toString()
        tranId = parcel.readString().toString()
        orderId = parcel.readString().toString()
        errorMsg = parcel.readString().toString()
        acc_bal = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(custName)
        parcel.writeFloat(payment)
        parcel.writeString(accNo)
        parcel.writeString(timestamp)
        parcel.writeString(tranId)
        parcel.writeString(orderId)
        parcel.writeString(errorMsg)
        parcel.writeString(acc_bal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SimpleForUI> {
        override fun createFromParcel(parcel: Parcel): SimpleForUI {
            return SimpleForUI(parcel)
        }

        override fun newArray(size: Int): Array<SimpleForUI?> {
            return arrayOfNulls(size)
        }
    }


}