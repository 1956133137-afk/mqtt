package com.yannuo.dgcanteen.util

import com.sunreedmaker.paykeyboard.*
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.interfaces.KeyboardListener


/**
 * Author: filowl
 * Description: 数字键盘
 * Date: 2023/7/11 9:51
 **/
class KeyboardUtil : ICheckListener {
    private val TAG = javaClass.simpleName
    private var keyboard: PayKeyboard? = null
    private var detector: USBDetector? = null
    private var observer: ArrayList<KeyboardListener> = arrayListOf()
    private var connectStatus = false

    companion object {
        val instance: KeyboardUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(KeyboardUtil::class.java) { KeyboardUtil() }
        }
    }

    fun sendPayCommand(payCommand: Byte) {
        keyboard?.sendControl(payCommand)
    }

    // 添加监听
    fun addObserver(listener: KeyboardListener) {
        if (!observer.contains(listener)) observer.add(listener)
    }

    // 移除监听
    fun removeObserver(listener: KeyboardListener) {
        if (observer.contains(listener)) observer.remove(listener)
    }

    fun openKeyboard() {
        if (detector == null) {
            detector = PayKeyboard.getDetector(MyApplication.applicationContext)
            detector?.setListener(this)
        }

        if (keyboard == null || keyboard!!.isReleased) {
            //键盘:PayCommand.MODE_KEYBOARD 计算器:PayCommand.MODE_CALCULATOR
            keyboard = PayKeyboard.get(MyApplication.applicationContext, PayCommand.MODE_KEYBOARD)
            if (keyboard != null) {
                keyboard?.setListener(object : DefaultKeyboardListener() {
                    override fun onRelease() { //释放资源
                        super.onRelease()
                        connectStatus = false
                        LogUtil.w(TAG, "Keyboard release")
                    }

                    override fun onAvailable() { //连接
                        super.onAvailable()
                        connectStatus = true
                        LogUtil.i(TAG, "onAvailable ...")
                    }

                    override fun onException(e: Exception) {
                        LogUtil.w(TAG, "Exception: ${e.message}")
                        if (e.message == "Already open") connectStatus = true
                        else closeKeyboard()
                        super.onException(e)
                    }

                    // 计算器模式
                    override fun onPay(request: IPayRequest) {
                        super.onPay(request)
                        LogUtil.d(TAG, "payAmount:${request.money}")
                        observer.forEach { it.computerMode(request.money) }
                    }

                    //键盘模式 按下
                    override fun onKeyDown(keyCode: Int, keyName: String) {
                        super.onKeyDown(keyCode, keyName)
                        LogUtil.d(TAG, "value:$keyCode code:$keyName")
                        observer.forEach { it.keyboardMode(keyCode, keyName) }
                    }
                })
                keyboard?.open()
            } else {
                LogUtil.i(TAG, "keyboard exists")
            }
        }
    }

    override fun onAttach() { //重连数字键盘
        LogUtil.d(TAG, "onAttach")
    }

    fun closeKeyboard() {
        connectStatus = false
        try {
            if (keyboard != null) {
                keyboard!!.release()
                keyboard = null
            }
            if (detector != null) {
                detector!!.release()
                detector = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
