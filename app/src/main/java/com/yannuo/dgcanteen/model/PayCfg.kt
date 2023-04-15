package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

class PayCfg() :Parcelable{
//    var campus_id :String ?= "441999527" //园区ID
//    var corp_id :String ?= "1041" //合作方ID
//    var business_id :String ?= "SJ2023032511004" //商家ID
////    var business_name :String ?= null //商家名称
//    var vpos_id :String ?= "V00463775" //柜台号

    var campus_id :String ?= null //园区ID
    var corp_id :String ?= null //合作方ID
    var business_id :String ?= null //商家ID
    //    var business_name :String ?= null //商家名称
    var vpos_id :String ?= null //柜台号

    constructor(parcel: Parcel) : this() {
        campus_id = parcel.readString()
        corp_id = parcel.readString()
        business_id = parcel.readString()
//        business_name = parcel.readString()
        vpos_id = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(campus_id)
        parcel.writeString(corp_id)
        parcel.writeString(business_id)
//        parcel.writeString(business_name)
        parcel.writeString(vpos_id)
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