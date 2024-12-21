package com.yannuo.dgcanteen.model

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/19 16:13
 * @Version 1.0
 */
data class TimeDetail(
    var mealName: String = "", //餐别名称
    var allTime: Int = 0,  //总使用次数
    var externalTime: Int = 0  //顺延出去的次数
)