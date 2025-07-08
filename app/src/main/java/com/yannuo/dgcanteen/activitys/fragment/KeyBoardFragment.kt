package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.sunreedmaker.paykeyboard.PayCommand
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.HostActivity
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentInputKeyboardBinding
import com.yannuo.dgcanteen.interfaces.KeyboardListener
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 17:26
 **/
class KeyBoardFragment : Fragment(), KeyboardListener {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentInputKeyboardBinding
    private var tvText: StringBuilder = StringBuilder()
    private val kv = MMKV.defaultMMKV()
//    private var mLock = false
//    private val handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputKeyboardBinding.inflate(inflater, container, false)
        initObject()
        initView()
        initEvent()
        return binding.root
    }

    private fun initObject() {
        EventBus.getDefault().register(this)
        val type = kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)
//        if (type == 0)

    }

    private fun initView() {
        btnClickable(!kv.decodeBool(Constant.QUOTA_SWITCH))
        if (kv.decodeBool(Constant.QUOTA_SWITCH)) {
//            kv.decodeString(Constant.QUOTA_AMOUNT)?.also {
//                tvText.append(it)
//                binding.inputAmount.text = tvText
//            }
            tvText.append(kv.decodeString(Constant.QUOTA_AMOUNT))
            binding.inputAmount.text = tvText
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
        binding.multiply.setOnClickListener { inputFields("×") } //×
        binding.addition.setOnClickListener { inputFields("+") } //+
        binding.equal.setOnClickListener { totalValue() } //=
        binding.payment.setOnClickListener { //收款
            if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1) {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_MEAL_TIME_BULK_PAY, true))
                return@setOnClickListener
            }
            val limit = kv.decodeInt(Constant.USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH, 0) // 受餐别时间限制
            val isUseMeal = kv.decodeInt(Constant.IS_USE_MEAL) // 当前时间是否开餐
            if (limit == 1) {
                if (isUseMeal == 1) {
                    collectMoney()
                } else {
                    ToastShowUtil.show("餐别未开餐")
                    CommonAndDpToPxUtil.speakWork("餐别未开餐")
                }
            } else collectMoney()
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

    override fun onResume() {
        super.onResume()
//        mLock = true
        LogUtil.i(TAG,"键盘解锁...")
        KeyboardUtil.instance.addObserver(this)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun eventKeyBoard(event: MessageEvent) {
        when (event.code) {
            Constant.EVENT_QUOTA_CHANGE ->
            {
//                handler.post {R
                    btnClickable(!kv.decodeBool(Constant.QUOTA_SWITCH))
                    tvText = StringBuilder()
                    if (kv.decodeBool(Constant.QUOTA_SWITCH)) {
                        tvText.append(kv.decodeString(Constant.QUOTA_AMOUNT, "0.00"))
                    }
                    binding.inputAmount.text = tvText
//                }
            }

            Constant.EVENT_OTHER_PAY ->{
//                handler.post {

                    collectMoney(event.any as Int)
//                }
            }

            Constant.EVENT_VERIFY ->{
                checkVerify()
            }

            Constant.EVENT_MEAL_TIME_BULK_PAY -> {
                checkVerify()
            }

            Constant.EVENT_KEYBOARD_CANCEL -> {
                tvText = StringBuilder()
                binding.inputAmount.text = null
            }
        }
    }

    //判断商家信息
    private fun judgePayCfg(): Boolean {
        val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
        return payCfg?.campusId != null && payCfg.corpId != null && payCfg.businessId != null && payCfg.counterId != null
    }

    private fun btnClickable(boolean: Boolean) {
        binding.figureZero.isClickable = boolean
        binding.figureOne.isClickable = boolean
        binding.figureTwo.isClickable = boolean
        binding.figureThr.isClickable = boolean
        binding.figureFou.isClickable = boolean
        binding.figureFiv.isClickable = boolean
        binding.figureSix.isClickable = boolean
        binding.figureSev.isClickable = boolean
        binding.figureEig.isClickable = boolean
        binding.figureNin.isClickable = boolean
        binding.point.isClickable = boolean
        binding.multiply.isClickable = boolean
        binding.addition.isClickable = boolean
        binding.equal.isClickable = boolean
        binding.backspace.isClickable = boolean
        binding.cancel.isClickable = boolean
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
        if (tvText.isNotEmpty()) {
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

    private fun payPageJump(amount: Float,ways : Int? = null) {
//        if (FaceHandler.getFaceHInstance().lock) ToastShowUtil.show("支付未完成")
        //TODO 跳转页面
        val bean = OrderPayInfo().apply {
            type = ways ?: kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)
            payment = amount
        }

        if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)
            && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
        ) { //网络监听
            CommonAndDpToPxUtil.speakWork("设备没有网络或者开启离线模式")
            ToastShowUtil.show("设备没有网络或者开启离线模式")
            return
        }
//        if (mLock.not())return
//        mLock = false
        val payIntent = Intent(requireContext(), HostActivity::class.java)
        payIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        payIntent.putExtra(Constant.PAY_DATE, Gson().toJson(bean))
        startActivity(payIntent)
    }

    private fun checkVerify(){
//        if (!judgePayCfg()) {
//            ToastShowUtil.show("商户信息不完整，请检查商户信息")
//            return
//        }
        if(tvText.isNullOrEmpty()){
            ToastShowUtil.show("请输入金额")
            CommonAndDpToPxUtil.speakWork("请输入金额")
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE_STATUS, ""))
            return
        }
        val limitStr = kv.decodeString(Constant.LIMIT_AMOUNT, "30").toString()
        val limitAmount = String.format(Locale.CHINA, "%.02f", limitStr.toFloat()).toFloat()
        val amount = String.format(Locale.CHINA, "%.02f", tvText.toString().toFloat())
        if (tvText.isNotEmpty() && kv.decodeBool(Constant.QUOTA_SWITCH)) {
            if (amount.toFloat() > limitAmount) {
                ToastShowUtil.show("单笔金额不得超过 $limitAmount 元")
                CommonAndDpToPxUtil.speakWork("单笔金额不得超过 $limitAmount 元")
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE_STATUS, ""))
                return
            }
        }
        if (kv.decodeInt(Constant.BTN_CONFIRM_STATE, 0) == 0) {
            kv.encode(Constant.BTN_CONFIRM_STATE, 1)
            CommonAndDpToPxUtil.speakWork("请支付$amount 元")
            // 核销模式
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_OPEN_BTN, amount))
            // 餐次模式
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_BULK_PAYMENT, amount))
        }
    }

    private fun collectMoney(ways: Int? = null){
        LogUtil.i(TAG,"${ways}")
//        if (!judgePayCfg()) {
//            ToastShowUtil.show("商户信息不完整，请检查商户信息")
//            return
//        }
        if(tvText.isNullOrEmpty()){
            ToastShowUtil.show("请输入金额")
            CommonAndDpToPxUtil.speakWork("请输入金额")
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE_STATUS, ""))
            return
        }
        val limitStr = kv.decodeString(Constant.LIMIT_AMOUNT, "30").toString()
        val limitAmount = String.format(Locale.CHINA, "%.02f", limitStr.toFloat()).toFloat()
        if (tvText.isNotEmpty() && kv.decodeBool(Constant.QUOTA_SWITCH)) {
            val amount = String.format(Locale.CHINA, "%.02f", tvText.toString().toFloat())
            if (amount.toFloat() > limitAmount) {
                ToastShowUtil.show("单笔金额不得超过 $limitAmount 元")
                CommonAndDpToPxUtil.speakWork("单笔金额不得超过 $limitAmount 元")
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE_STATUS, ""))
                return
            }
            payPageJump(amount.toFloat(),ways)
        } else {
            val count = totalValue()
            if (count != null) {
                if (count > limitAmount) {
                    ToastShowUtil.show("单笔金额不得超过 $limitAmount 元")
                    CommonAndDpToPxUtil.speakWork("单笔金额不得超过 $limitAmount 元")
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE_STATUS, ""))
                    return
                }
                payPageJump(count,ways)
                tvText = StringBuilder()
                binding.inputAmount.text = null
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        KeyboardUtil.instance.removeObserver(this)
        super.onDestroy()
    }

    override fun keyboardMode(keyCode: Int, keyName: String) {
        Handler(Looper.getMainLooper()).post {
            LogUtil.d(TAG, "keyCode:$keyCode keyName:$keyName")
            when (keyCode) {
                99 -> { inputFields(".") }
                98 -> { inputFields("0") }
                89 -> { inputFields("1") }
                90 -> { inputFields("2")}
                91 -> { inputFields("3")}
                92 -> { inputFields("4")}
                93 -> { inputFields("5")}
                94 -> { inputFields("6")}
                95 -> { inputFields("7")}
                96 -> { inputFields("8")}
                97 -> { inputFields("9")}
                85 -> { inputFields("×")}
                87 -> { inputFields("+")}
                46 -> { totalValue()}
                41 -> {
                    tvText = StringBuilder()
                    binding.inputAmount.text = null
                }
                42 -> {
                    if (tvText.isNotEmpty()) {
                        tvText.deleteCharAt(tvText.length - 1)
                        binding.inputAmount.text = tvText
                    }
                }
            }
        }
    }

    override fun computerMode(value: Double) {
        Handler(Looper.getMainLooper()).post {
            Log.d(TAG, "value:$value")
            val payMoney = String.format("%.02f", value)
            KeyboardUtil.instance.sendPayCommand(PayCommand.SCREEN_CLEAR)
            binding.inputAmount.text = payMoney
            collectMoney()
        }
    }
}