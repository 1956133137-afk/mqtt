package com.yannuo.dgcanteen.interfaces

/**
 * Author: filowl
 * Description: 数字键盘
 * Date: 2024/2/22 15:40
 **/
interface KeyboardListener {
    // 键盘模式
    fun keyboardMode(keyCode: Int, keyName: String)

    // 计算模式
    fun computerMode(value: Double)
}