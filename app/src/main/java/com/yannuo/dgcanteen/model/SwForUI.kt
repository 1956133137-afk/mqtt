package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/27 17:20
 * @Version 1.0
 */
class SwForUI {
    var result: String = ""  //支付结果
    var username: String = "" //姓名
    var balance: String = ""  //余额
    var breakfastTime: Int = 0  //早餐次数
    var lunchTime: Int = 0    //午餐次数
    var dinnerTime: Int = 0   //晚餐次数
    var supperTime: Int = 0   //夜宵次数

    var actualMealName: String = ""     //实际支付餐别名称
    var actualPrice: String = ""        //实际单价
    var standardMealName: String = ""         //餐别名称
    var useMealRuleName: String = ""  //使用的餐标名称
    var everyUseTime: String = "0"        //每次使用餐标耗次
    var restTime: Int = 0             //剩余使用次数
    var rulePrice: Float = 0.0f     //餐标单价
    var subsidy: String = "0.0"       //补贴

    override fun toString(): String {
        return "SwForUI(result='$result', username='$username', balance='$balance', breakfastTime=$breakfastTime, lunchTime=$lunchTime, dinnerTime=$dinnerTime, supperTime=$supperTime, actualMealName='$actualMealName', standardMealName='$standardMealName', useMealRuleName='$useMealRuleName', everyUseTime='$everyUseTime', restTime=$restTime, rulePrice=$rulePrice, subsidy='$subsidy')"
    }


}