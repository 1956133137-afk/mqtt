package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

/**
 * 手输入金额的请求类
 * @property type Int
 */
class OrderPayInfo() :Parcelable {
    var type  = 0  //支付类型
    var payment = 0.0F //支付金额
    var isAllowance = 2 // 1 使用餐次   2 其他
    var orderFlag = "" // 企业

    constructor(parcel: Parcel) : this() {
        type = parcel.readInt()
        payment = parcel.readFloat()
        isAllowance = parcel.readInt()
        orderFlag = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
        parcel.writeFloat(payment)
        parcel.writeInt(isAllowance)
        parcel.writeString(orderFlag)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderPayInfo> {
        override fun createFromParcel(parcel: Parcel): OrderPayInfo {
            return OrderPayInfo(parcel)
        }

        override fun newArray(size: Int): Array<OrderPayInfo?> {
            return arrayOfNulls(size)
        }
    }


}