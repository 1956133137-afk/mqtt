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

    constructor(parcel: Parcel) : this() {
        type = parcel.readInt()
        payment = parcel.readFloat()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
        parcel.writeFloat(payment)
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