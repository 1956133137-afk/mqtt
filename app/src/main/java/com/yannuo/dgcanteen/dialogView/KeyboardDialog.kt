package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.GridLayoutManager
import com.yannuo.dgcanteen.adapters.KeyboardAdapter
import com.yannuo.dgcanteen.databinding.DialogKeyboardBinding
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/12 11:41
 **/
class KeyboardDialog(context: Context) : BaseDialog<DialogKeyboardBinding>(context) {
    private val mContext = context
    private val handler: Handler = Handler(mContext.mainLooper)
    private val keyboardAdapter by lazy { KeyboardAdapter() }
    private val payStr: StringBuilder = StringBuilder()
    private var listener: OnKeyboardCallback? = null

    fun setListener(listener: OnKeyboardCallback) {
        this.listener = listener
    }

    override fun initDialogView() {
        binding = DialogKeyboardBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        initObject()
        initEvent()
    }

    private fun initObject() {
        binding.keyboardRecycler.layoutManager = GridLayoutManager(mContext, 4)
        binding.keyboardRecycler.adapter = keyboardAdapter
        keyboardAdapter.setListener(object : KeyboardAdapter.OnFigureCallback {
            override fun onFigure(figure: String) {
                handler.post {
                    when (figure) {
                        in "0".."9", ".", "×", "+" -> inputFields(figure)
                        "取消" -> {
                            listener?.onKeyboard("-1", false)
                            clearStr(true)
                        }
                        "清除" -> {
                            listener?.onKeyboard("0", false)
                            clearStr(false)
                        }
                        "收款" -> {
                            val value = totalValue()
                            if (value != null) listener?.onKeyboard(String.format("%.02f", value.toDouble()), true)
                        }
                    }
                }
            }
        })
        keyboardAdapter.data = mutableListOf("1", "2", "3", "×", "4", "5", "6", "+", "7", "8", "9", "取消", "清除", "0", ".", "收款")
    }

    fun updateTV(str: String, boolean: Boolean) {
        binding.mvControl.text = str
        if (boolean) clearStr(false)
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener { //回退
            if (payStr.isNotEmpty()) {
                payStr.deleteCharAt(payStr.length - 1)
                binding.inputAmount.text = payStr
            }
        }
    }

    private fun clearStr(boolean: Boolean) {
        payStr.clear()
        binding.inputAmount.text = payStr
        if (boolean) dismiss()
    }

    private fun inputFields(str: String) { //输入金额检测是否合法
        if (payStr.isNotEmpty()) {
            when (payStr[payStr.length - 1]) {
                '+', '×' -> {
                    when (str) {
                        "+", "×" -> {
                            payStr.deleteCharAt(payStr.length - 1)
                            payStr.append(str)
                            binding.inputAmount.text = payStr
                        }
                        else -> inputFigure(str)
                    }
                }
                else -> inputFigure(str)
            }
        } else {
            when (str) {
                ".", "+", "×" -> {
                    inputFigure("0")
                    inputFigure(str)
                }
                else -> inputFigure(str)
            }
        }
    }

    private fun inputFigure(str: String) {
        if (payStr.length < 28) {
            if (isFormJudgment(payStr.append(str).toString())) binding.inputAmount.text = payStr
            else {
                payStr.deleteCharAt(payStr.length - 1)
                ToastShowUtil.show("格式有误")
            }
        } else ToastShowUtil.show("超出显示长度")
    }

    private fun isFormJudgment(str: String): Boolean { //判断格式
        val regex = Regex("""^((0|[1-9]\d{0,9})(\.\d{0,2})?(×|\+))*(0|[1-9]\d{0,9})(\.\d{0,2})?(×|\+)?$""", RegexOption.IGNORE_CASE)
        return regex.matches(str)
    }

    private fun totalValue(): Float? { // = 收款
        if (payStr.isNotEmpty()) {
            if (payStr[payStr.length - 1] == '+' || payStr[payStr.length - 1] == '×') payStr.deleteCharAt(payStr.length - 1)
            var payment = 0.0
            val dataList = payStr.split("+")
            dataList.forEach { compute ->
                if (compute.isNotEmpty()) {
                    val mul = compute.split("×")
                    if (mul.size == 1) payment += mul[0].toDouble()
                    else {
                        var count = 1.0
                        mul.forEach { if (it.isNotEmpty()) count *= it.toDouble() }
                        payment += count
                    }
                }
            }
            payStr.clear()
            val paymentStr = String.format(Locale.CHINA, "%.02f", payment)
            payStr.append(paymentStr)
            binding.inputAmount.text = paymentStr
            return paymentStr.toFloat()
        } else {
            ToastShowUtil.show("请输入收款金额")
            return null
        }
    }

    interface OnKeyboardCallback {
        fun onKeyboard(payAmount: String, status: Boolean)
    }
}