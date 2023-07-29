package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

/**
 * 手输入金额的请求类
 * @property type Int
 */
class OrderPayInfo() :Parcelable {
    var type  = 0  //支付类型

    constructor(parcel: Parcel) : this() {
        type = parcel.readInt()
    }

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        TODO("Not yet implemented")
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