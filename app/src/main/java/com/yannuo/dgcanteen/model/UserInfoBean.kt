package com.yannuo.dgcanteen.model

/**
 * userId   用户ID
 * campusId 园区ID
 * custId   智慧食堂用户唯一标识
 * cardId   卡号
 * personName 人员姓名
 * personNumber 学号/工号
 * image    相片URL
 * idCard   身份证号码
 * grade    年级
 * Class    班级
 * sex  性别
 */

data class UserInfoBean(
    val userId: String,
    val campusId: String,
    val custId: String,
    val cardId: String,
    val personName: String,
    val personNumber: String,
    val image: String,
    val idCard: String,
    val grade: String,
    val Class: String,
    val sex: Int
)
