package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.yannuo.dgcanteen.activitys.HostActivity
import com.yannuo.dgcanteen.activitys.viewModel.ProceedsVM
import com.yannuo.dgcanteen.databinding.FragmentInputKeyboardBinding
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.util.Constant
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
    private lateinit var viewModel: ProceedsVM
    private var amount = 0F
    private var value: StringBuilder = StringBuilder()
    private var tvText: StringBuilder = StringBuilder()
    private var symbol = ""
    private val symbolKey = Stack<String>()
    private val valueKey = Stack<Double>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputKeyboardBinding.inflate(inflater, container, false)
        initObject()
        initEvent()

        return binding.root
    }

    private fun initObject() {
        if (!this::viewModel.isInitialized) {
            viewModel = ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
            )[ProceedsVM::class.java]
        }
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
        binding.multiply.setOnClickListener { calculateValue("×") } //×
        binding.addition.setOnClickListener { calculateValue("+") } //+
        binding.equal.setOnClickListener { totalValue() } //=
        binding.payment.setOnClickListener { //收款
            totalValue()
            viewModel.getAmount(value.toString(), ProceedsVM.PayStatus.PAY)
            value = StringBuilder()
            tvText = StringBuilder()
        }
        binding.cancel.setOnClickListener { clearData() } //清除
    }

    private fun inputFields(str: String) { //输入金额检测是否合法
        if (isFormJudgment(tvText.append(str).toString())) {
            value.append(str)
            binding.inputAmount.text = tvText
        } else {
            tvText.deleteCharAt(tvText.length - 1)
            ToastShowUtil.show("格式有误")
        }
    }

    private fun isFormJudgment(str: String): Boolean { //判断格式
        val regex = Regex(
            """^((0|[1-9]\d{0,5})(\.\d{0,2})?(×|\+))*(0|[1-9]\d{0,5})(\.\d{0,2})?$""",
            RegexOption.IGNORE_CASE
        )
        return regex.matches(str)
    }

    private fun calculateValue(str: String) { // × +
        if (value.isNotEmpty()) {
            if (symbol != "") symbolKey.push(symbol)
            valueKey.push(value.toString().toDouble())
            value = StringBuilder()
            symbol = str
            tvText.append(str)
            if (!symbolKey.empty() && symbolKey.peek() == "×") {
                symbolKey.pop()
                valueKey.push(valueKey.pop() * valueKey.pop())
            }
            binding.inputAmount.text = tvText
        }
    }

    private fun totalValue() { // = 收款
        if (value.isNotEmpty()) {
            if (symbol != "") symbolKey.push(symbol)
            valueKey.push(value.toString().toDouble())
            value = StringBuilder()
            symbol = ""
            tvText = StringBuilder()
            while (!symbolKey.empty()) {
                when (symbolKey.pop()) {
                    "+" -> valueKey.push(valueKey.pop() + valueKey.pop())
                    "×" -> valueKey.push(valueKey.pop() * valueKey.pop())
                }
            }
            val str = String.format(Locale.CHINA, "%.02f", valueKey.peek())
            binding.inputAmount.text = str
            value.append(str)
            tvText.append(str)

            if (FaceHandler.getFaceHInstance().lock)ToastShowUtil.show("支付未完成")
            //TODO 跳转页面
            var payType = Constant.PAY_FACE_TYPE  //0 人脸支付 1、刷卡 、2 扫毛
            if (payType == 0) {
                if (!FaceHandler.getFaceHInstance().isFaceInit) {
                    ToastShowUtil.show("人脸服务未启动,请重启软件")
                    return
                }
            }

            val bean = OrderPayInfo()
            bean.type = payType

            val payIntent = Intent(requireContext(), HostActivity::class.java)
            payIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            payIntent.putExtra(Constant.PAY_DATE,bean)
            startActivity(payIntent)
        }
    }

    private fun clearData() { //清除
        amount = 0F
        value = StringBuilder()
        tvText = StringBuilder()
        symbol = ""
        binding.inputAmount.text = null
        while (!valueKey.empty()) {
            valueKey.pop()
        }
        while (!symbolKey.empty()) {
            symbolKey.pop()
        }
    }

}