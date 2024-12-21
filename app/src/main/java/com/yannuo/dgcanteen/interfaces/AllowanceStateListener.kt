package com.yannuo.dgcanteen.interfaces

import com.yannuo.dgcanteen.model.PayForUI

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/5 18:16
 * @Version 1.0
 */
interface AllowanceStateListener {
    /**
     * 餐别未配置餐标，但有餐标使用
     */
    fun hasAllowance(paymentMap: HashMap<String, String>?, payForUI: PayForUI)
    fun withoutAllowance(paymentMap: HashMap<String, String>?, payForUI: PayForUI)
}