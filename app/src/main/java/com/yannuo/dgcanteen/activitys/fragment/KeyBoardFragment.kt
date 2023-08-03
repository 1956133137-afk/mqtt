package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.activitys.HostActivity
import com.yannuo.dgcanteen.databinding.FragmentInputKeyboardBinding
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 17:26
 **/
class KeyBoardFragment : Fragment() {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentInputKeyboardBinding
    private var tvText: StringBuilder = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputKeyboardBinding.inflate(inflater, container, false)
        initObject()
        initEvent()

        return binding.root
    }

    private fun initObject() {

    }

    private fun initEvent() {
        binding.figureZero.setOnClickListener { inputFields("0") }  //0
        binding.figureOne.setOnClickListener { inputFields("1") }   //1
        binding.figureTwo.setOnClickListener { inputFields("2") }   //2
        binding.figureThr.setOnClickListener { inputFields("3") }   //3
        binding.figureFou.setOnClickListener { inputFields("4") }   //4
        binding.figureFiv.setOnClickListener { inputFields("5") }   //5
        binding.figureSix.setOnClickListener { inputFields("6") }   //6
        binding.figureSev.setOnClickListener { inputFields("7") }   //7
        binding.figureEig.setOnClickListener { inputFields("8") }   //8
        binding.figureNin.setOnClickListener { inputFields("9") }   //9
        binding.point.setOnClickListener { inputFields(".") }       //.
        binding.multiply.setOnClickListener { inputFields("×") } //×
        binding.addition.setOnClickListener { inputFields("+") } //+
        binding.equal.setOnClickListener { totalValue() } //=
        binding.payment.setOnClickListener { //收款
            val count = totalValue()
            if (count != null) {
                payPageJump(count)
                tvText = StringBuilder()
                binding.inputAmount.text = null
            }
        }
        binding.backspace.setOnClickListener { //回退
            if (tvText.isNotEmpty()) {
                tvText.deleteCharAt(tvText.length - 1)
                binding.inputAmount.text = tvText
            }
        }
        binding.cancel.setOnClickListener { //清除
            tvText = StringBuilder()
            binding.inputAmount.text = null
        }
    }

    private fun inputFields(str: String) { //输入金额检测是否合法
        if (tvText.isNotEmpty()) {
            when (tvText[tvText.length - 1]) {
                '+', '×' -> {
                    when (str) {
                        "+", "×" -> {
                            tvText.deleteCharAt(tvText.length - 1)
                            tvText.append(str)
                            binding.inputAmount.text = tvText
                        }
                        else -> inputFigure(str)
                    }
                }
                else -> inputFigure(str)
            }
        } else inputFigure(str)
    }

    private fun inputFigure(str: String) {
        if (tvText.length < 28) {
            if (isFormJudgment(tvText.append(str).toString())) {
                binding.inputAmount.text = tvText
            } else {
                tvText.deleteCharAt(tvText.length - 1)
                ToastShowUtil.show("格式有误")
            }
        } else {
            ToastShowUtil.show("超出显示长度")
        }
    }

    private fun isFormJudgment(str: String): Boolean { //判断格式
        val regex = Regex(
            """^((0|[1-9]\d{0,5})(\.\d{0,2})?(×|\+))*(0|[1-9]\d{0,5})(\.\d{0,2})?(×|\+)?$""",
            RegexOption.IGNORE_CASE
        )
        return regex.matches(str)
    }

    private fun totalValue(): Float? { // = 收款
        if (tvText[tvText.length - 1] == '+' || tvText[tvText.length - 1] == '×') {
            ToastShowUtil.show("格式有误")
            return null
        }
        var payment = 0.0F
        val dataList = tvText.split("+")
        dataList.forEach { compute ->
            if (compute.isNotEmpty()) {
                val mul = compute.split("×")
                if (mul.size == 1) {
                    payment += (compute).toFloat()
                } else {
                    var count = 1.0F
                    mul.forEach {
                        if (it.isNotEmpty()) count *= it.toFloat()
                    }
                    payment += count
                }
            }
        }
        if (tvText.isNotEmpty()) {
            tvText = StringBuilder()
            val str = String.format(Locale.CHINA, "%.02f", payment)
            tvText.append(str)
            binding.inputAmount.text = str
            return tvText.toString().toFloat()
        } else {
            ToastShowUtil.show("请输入收款金额")
            return null
        }
    }

    private fun payPageJump(amount: Float) {
        if (FaceHandler.getFaceHInstance().lock) ToastShowUtil.show("支付未完成")
        //TODO 跳转页面
        //刷卡
        val bean = OrderPayInfo().apply {
            type = Constant.PAY_IC_TYPE
            payment = amount
        }

        if (bean.type == 0) {
            if (!FaceHandler.getFaceHInstance().isFaceInit) {
                ToastShowUtil.show("人脸服务未启动,请重启软件")
                return
            }
        }
        LogUtil.d(TAG, "支付")

        val payIntent = Intent(requireContext(), HostActivity::class.java)
        payIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        payIntent.putExtra(Constant.PAY_DATE, bean)
        startActivity(payIntent)
    }
}