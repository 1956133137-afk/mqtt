package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable
import com.yannuo.dgcanteen.util.Constant

class PayCfg() :Parcelable{
//    var campus_id :String ?= "441999527" //园区ID
//    var corp_id :String ?= "1041" //合作方ID
//    var business_id :String ?= "SJ2023032511004" //商家ID
////    var business_name :String ?= null //商家名称
//    var vpos_id :String ?= "V00463775" //柜台号

    var campusId :String ?= null //园区ID
    var corp_id :String ?= Constant.CORP_ID //合作方ID  生产:1046 测试:1041
    var businessId :String ?= null //商家ID
    var businessName :String ?= null //商家名称
    var counterId :String ?= null //柜台号
    var windowId :String ?= null //窗口Id
    var windowName :String ?= null //窗口名称

    constructor(parcel: Parcel) : this() {
        campusId = parcel.readString()
        corp_id = parcel.readString()
        businessId = parcel.readString()
        businessName = parcel.readString()
        counterId = parcel.readString()
        windowId = parcel.readString()
        windowName = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(campusId)
        parcel.writeString(corp_id)
        parcel.writeString(businessId)
        parcel.writeString(businessName)
        parcel.writeString(counterId)
        parcel.writeString(windowId)
        parcel.writeString(windowName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PayCfg> {
        override fun createFromParcel(parcel: Parcel): PayCfg {
            return PayCfg(parcel)
        }

        override fun newArray(size: Int): Array<PayCfg?> {
            return arrayOfNulls(size)
        }
    }


}