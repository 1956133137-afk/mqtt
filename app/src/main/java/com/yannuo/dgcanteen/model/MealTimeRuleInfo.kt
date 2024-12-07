package com.yannuo.dgcanteen.model

import android.os.Parcel
import android.os.Parcelable

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/14 17:11
 * @Version 1.0
 */
class MealTimeRuleInfo(): Parcelable {
    var id: Int? = null
    var mealName: String = ""
    var flag: Int = 2 // 是否支持顺延，1支持，2不支持
    var mealNum: String = "0" // 每日限制次数
    var standardName: String = "" // 规则名称
    var standardNum: String = "" // 每次收款使用的餐次次数
    var subsidyMoney: String = "0" // 餐补金额
    var userTypeId: String = "0"  // 人员Id
    var userTypeName: String? = null
    var price: Float = 0.0f // 餐别金额
    var price1: Float? = null // 外来人员金额

    var NO: Int = -1 // 排序编号

    var type: Int = 0 // 支付类型


    constructor(parcel: Parcel) : this() {
        mealName = parcel.readString().toString()
        flag = parcel.readInt()
        mealNum = parcel.readString().toString()
        standardName = parcel.readString().toString()
        standardNum = parcel.readString().toString()
        subsidyMoney = parcel.readString().toString()
        userTypeId = parcel.readString().toString()
        userTypeName = parcel.readString().toString()
        price = parcel.readFloat()
        price1 = parcel.readFloat()
        type = parcel.readInt()
        NO = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mealName)
        parcel.writeInt(flag)
        parcel.writeString(mealNum)
        parcel.writeString(standardName)
        parcel.writeString(standardNum)
        parcel.writeString(subsidyMoney)
        parcel.writeString(userTypeId)
        parcel.writeString(userTypeName ?: "")
        parcel.writeFloat(price)
        parcel.writeFloat(price1 ?: 0.0f)
        parcel.writeInt(type)
        parcel.writeInt(NO)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "MealTimeRuleInfo(id=$id, mealName='$mealName', flag=$flag, mealNum='$mealNum', standardName='$standardName', standardNum='$standardNum', subsidyMoney='$subsidyMoney', userTypeId='$userTypeId', userTypeName=$userTypeName, price=$price, price1=$price1, NO=$NO, type=$type)"
    }


    companion object CREATOR : Parcelable.Creator<MealTimeRuleInfo> {
        override fun createFromParcel(parcel: Parcel): MealTimeRuleInfo {
            return MealTimeRuleInfo(parcel)
        }

        override fun newArray(size: Int): Array<MealTimeRuleInfo?> {
            return arrayOfNulls(size)
        }
    }


}