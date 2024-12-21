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
    var meal01Time: Int = 0  //meal01次数
    var meal02Time: Int = 0    //meal02次数
    var meal03Time: Int = 0   //meal03次数
    var meal04Time: Int = 0   //meal04次数

    var actualMealId: String = ""      //实际支付餐别id
    var actualMealName: String = ""     //实际支付餐别名称
    var actualPrice: String = ""        //实际单价
    var standardMealId: String = ""     //餐标餐标id
    var standardMealName: String = ""         //餐别名称
    var useMealRuleId: String = ""      //餐标id
    var useMealRuleName: String = ""  //使用的餐标名称
    var everyUseTime: String = "0"        //每次使用餐标耗次
    var restTime: Int = 0             //今日剩余使用次数
    var rulePrice: Float = 0.0f     //餐标单价
    var subsidy: String = "0.0"       //补贴

    override fun toString(): String {
        return "SwForUI(result='$result', username='$username', balance='$balance', meal01Time=$meal01Time, meal02Time=$meal02Time, meal03Time=$meal03Time, meal04Time=$meal04Time, actualMealId='$actualMealId', actualMealName='$actualMealName', actualPrice='$actualPrice', standardMealId='$standardMealId', standardMealName='$standardMealName', useMealRuleId='$useMealRuleId', useMealRuleName='$useMealRuleName', everyUseTime='$everyUseTime', restTime=$restTime, rulePrice=$rulePrice, subsidy='$subsidy')"
    }


}