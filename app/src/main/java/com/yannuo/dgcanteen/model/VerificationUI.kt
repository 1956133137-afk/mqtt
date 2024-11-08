package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

//核销界面UI
class VerificationUI() : Parcelable{
    var errorMsg : String = ""
    var personName : String = ""
    var dish : Array<String>? = null
    var dishesList : Array<String>? = null
    var window :  Array<String>? = null
    var windows : Array<String>? = null
    var time : String = ""
    var unDish : Array<String>? = null

    constructor(parcel: Parcel) : this() {
        errorMsg = parcel.readString().toString()
        personName = parcel.readString().toString()
        dish = parcel.createStringArray()
        dishesList = parcel.createStringArray()
        window = parcel.createStringArray()
        windows = parcel.createStringArray()
        time = parcel.readString().toString()
        unDish = parcel.createStringArray()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(errorMsg)
        parcel.writeString(personName)
        parcel.writeStringArray(dish)
        parcel.writeStringArray(dishesList)
        parcel.writeStringArray(window)
        parcel.writeStringArray(windows)
        parcel.writeString(time)
        parcel.writeStringArray(unDish)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VerificationUI> {
        override fun createFromParcel(parcel: Parcel): VerificationUI {
            return VerificationUI(parcel)
        }

        override fun newArray(size: Int): Array<VerificationUI?> {
            return arrayOfNulls(size)
        }
    }

}