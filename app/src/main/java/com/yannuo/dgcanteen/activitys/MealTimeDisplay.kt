package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Color
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.text.format.DateFormat
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.proembed.service.MyService
import com.sun.mail.imap.protocol.BODY
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.MealTimeVM
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.databinding.DisplayMealTimeBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.AllowanceStateListener
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/2 11:01
 * @Version 1.0
 */
class MealTimeDisplay(private val mContext: Context, display: Display): BaseDisplay(mContext, display)
    ,NetworkStateManager.NetWorkListener, CallbackListener, ScanDevice.DataCallBack, OnReadDataListener,
    FaceScanVM.FaceResultListener, AllowanceStateListener {

    private val TAG = javaClass.simpleName
    private val activity = mContext as CalculateActivity
    private val payViewModel by lazy { ViewModelProvider(activity)[PayViewModel::class.java] }
    private val faceScanVM by lazy { FaceScanVM.instance }
    private val viewModel by lazy { ViewModelProvider(activity)[MealTimeVM::class.java] }
    private lateinit var binding: DisplayMealTimeBinding
    private lateinit var awaitPayDialog: AwaitingDialog
//    private lateinit var awaitPayDialog: WaitForPayDialog
    protected lateinit var mScope: CoroutineScope

    private val kv by lazy { MMKV.defaultMMKV() }
    private var mXService: MyService? = null
    private var navigation = true
    private var mealId = -1
    private var countDown: CountDownTimer? = null
    private var countDownTime: Long = 10L

    private var bulkPayShow: Boolean = true


    private val mHandler = CoroutineExceptionHandler{ coroutineContext, throwable ->
        throwable.printStackTrace()
        LogUtil.e(TAG, "error: ${throwable.message}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtil.i(TAG, "MealTimeDisplay onCreate...")
        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = DisplayMealTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mScope = CoroutineScope(Dispatchers.Default + mHandler)
        onInit()
    }

    private fun onInit() {
        payViewModel.setIsSw(1)
        initData()
        checkTime()
        initEvent()
    }

    private fun initData() {
        NetworkStateManager.getInstance().registerObserver(this)
        mXService = MyService(context)
        faceScanVM.setFaceListener(this)
        faceScanVM.bindService()

        if (NetworkStateManager.getInstance().isOnline(context).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }else{
            netWorkStatus("0")
        }

        viewModel.showErrorToast.observe(activity) {
            ToastShowUtil.show(it)
        }

        payViewModel.listener = this
        payViewModel.allowanceStateListener = this

//        if (MyApplication.openFacePay) {
//            this.onOtherListener(1)
//        }
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }

        binding.btnTimeQueryMode.setOnClickListener {
            if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
                kv.encode(Constant.QUERY_TIME_SWITCH, 1)
                binding.btnFacePay.text = "刷脸查询"
                binding.btnTimeQueryMode.setBackgroundResource(R.drawable.click_button_blue)
                binding.btnTimeQueryMode.setTextColor(Color.parseColor("#FFFFFF"))
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_AWAIT_DIALOG, "查询余次中"))
            } else {
                kv.encode(Constant.QUERY_TIME_SWITCH, 0)
                binding.btnFacePay.text = "刷脸消费"
                binding.btnTimeQueryMode.setBackgroundResource(R.drawable.click_button)
                binding.btnTimeQueryMode.setTextColor(Color.parseColor("#4F4F4F"))
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_DISMISS_CALCULATE_AWAIT_DIALOG, null))
            }
//            MyApplication.openFacePay = true
        }

        binding.btnFacePay.setOnClickListener {
            if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) {
                this@MealTimeDisplay.onOtherListener(4, "当前设备无网络，不能使用餐次模式")
                return@setOnClickListener
            }
            if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) {
                // 查询餐次
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_DISMISS_CALCULATE_AWAIT_DIALOG, null))
                faceScanVM.setTimeOut(0)
                faceScanVM.startFacePay(true)
            } else {
                // 支付
                if (TimeUtil.CurrentTimeSection() == 0) {
                    this@MealTimeDisplay.onOtherListener(4, "未开餐，不能使用餐次模式")
                    return@setOnClickListener
                }
                if (payViewModel.bulkPayAmount.value.isNullOrBlank()) {
                    this@MealTimeDisplay.onOtherListener(4, "未确认金额")
                } else {
                    val queryToMeals =
                        DishesDBHelper.getInstance().queryToMeals(TimeUtil.CurrentTimeSection())
                    MyApplication.actulMealName = queryToMeals.mealName
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_DISMISS_CALCULATE_PAY_DIALOG, null))
                    faceScanVM.setTimeOut(30000)
                    faceScanVM.startFacePay(false, payViewModel.bulkPayAmount.value.toString())
                }
            }
        }

//        binding.ibtBack.setOnClickListener {
//            finish()
//        }
    }

    override fun onStart() {
        super.onStart()
        LogUtil.i(TAG, "MealTimeDisplay onStart...")
        payViewModel.mReadCardListener = this
        payViewModel.mScanCodeListener = this
        payViewModel.openPayStatus()

        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
            binding.btnFacePay.text = "刷脸消费"
            binding.btnTimeQueryMode.setBackgroundResource(R.drawable.click_button)
            binding.btnTimeQueryMode.setTextColor(Color.parseColor("#4F4F4F"))
        } else {
            binding.btnFacePay.text = "刷脸查询"
            binding.btnTimeQueryMode.setBackgroundResource(R.drawable.click_button_blue)
            binding.btnTimeQueryMode.setTextColor(Color.parseColor("#FFFFFF"))
        }
    }



    private fun checkTime() {
        mScope.launch {
            while (isActive) {
                val mMealId = TimeUtil.CurrentTimeSection()
                LogUtil.i(TAG, "checkTime mealId: $mMealId")
                mealId = mMealId
                val str = StringBuilder()
                when (mealId) {
                    0 -> {
                        kv.encode(Constant.IS_USE_MEAL, 0)
                        str.append(resources.getString(R.string.unOpen_meal))
                    }
                    else -> {
                        kv.encode(Constant.IS_USE_MEAL, 1)
                        val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
                        if (meal == null) {
                            kv.encode(Constant.IS_USE_MEAL, 0)
                            str.append(resources.getString(R.string.unOpen_meal))
                            LogUtil.e(TAG, "checkTime --> meal is null")
                        } else {
                            str.append(meal.mealName + " ")
                            str.append(
                                DateFormat.format("HH:mm", meal.startTime).toString() + "~"
                            )
                            str.append(DateFormat.format("HH:mm", meal.endTime).toString())
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.mealTime.text = str
                }
                delay(5000)
            }
        }
    }

    override fun netWorkStatus(statue: String) {
        mScope.launch(Dispatchers.Main + mHandler) {
            when (statue) {
                "0" -> {
                    binding.network.setImageResource(R.drawable.ic_wifi)
                    if (kv.decodeBool(Constant.SWITCH, false)) {
                        kv.encode(Constant.SWITCH, false)
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
                    }
                }
                else -> {
                    binding.network.setImageResource(R.drawable.ic_wifi_no)
                }
            }
        }
    }

    override fun show() {
        super.show()
        updateOrderCount()
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i(TAG, "MealTimeDisplay onStop...")
        NetworkStateManager.getInstance().unRegisterObserver(this)
        if (this::awaitPayDialog.isInitialized) awaitPayDialog.cancel()
        payViewModel.closePayStatus()
        payViewModel.listener = null
    }


    fun redraw(view: View) {
        binding.drawHook.clearData()
        binding.drawHook.invalidate()
        binding.drawCross.clearData()
        binding.drawCross.invalidate()
    }

    @SuppressLint("SetTextI18n")
    override fun onOtherListener(event: Int, any: Any?) {
        mScope.launch(Dispatchers.Main + mHandler) {
            try {
                if (this@MealTimeDisplay::awaitPayDialog.isInitialized && awaitPayDialog.isShowing){
                    if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) != 1)
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_DISMISS_CALCULATE_AWAIT_DIALOG, null))
                    awaitPayDialog.dismiss()
//                    MyApplication.openFacePay = false
                }
                when (event) {
                    1 -> { //开始支付
                        if (!this@MealTimeDisplay::awaitPayDialog.isInitialized) awaitPayDialog = AwaitingDialog(context)
                        awaitPayDialog.show()
                        var str = "支付中"
                        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) str = "查询余次中"
                        awaitPayDialog.updateText(str)
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_AWAIT_DIALOG, str))
                        clearScreenData()
                    }
                    2 -> { //异常
                        ToastShowUtil.show("支付异常：$any")
                        LogUtil.d(TAG, "支付异常：$any")
                    }
                    3-> { //支付成功
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUIT_CONFIRM, "payResult"))
                        bulkPayShow = false
                        payViewModel.bulkPayAmount.value = ""
                        countDown?.cancel()
                        val payForUI = any as PayForUI
                        if (payForUI.result == "Y") {
                            binding.paySuccess.visibility = View.VISIBLE
                            binding.payFail.visibility = View.GONE
                            binding.tvSuccessMsg.text = "消费成功 ${payForUI.actualPayment} 元！"
                            EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG, true to "消费成功 ${payForUI.actualPayment} 元！"))
                            binding.drawHook.clearData()
                            binding.drawHook.invalidate()

                            addScreenData(payForUI.swForUI)

                            payViewModel.saveSwOrderRecord(payForUI, 1)

                            updateOrderCount()

                            CommonAndDpToPxUtil.speakWork("消费成功${payForUI.actualPayment}元！")
                        } else {
                            binding.paySuccess.visibility = View.GONE
                            binding.payFail.visibility = View.VISIBLE
                            binding.drawCross.clearData()
                            binding.drawCross.invalidate()
                            binding.tvFailMsg.text = "消费失败！${any.errMsg}"
                            EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG, false to "消费失败！${any.errMsg}"))

                            CommonAndDpToPxUtil.speakWork("消费失败！${any.errMsg}")
                        }
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(countDownTime) + 200, 1000) {
                            override fun onTick(mil: Long) {}

                            override fun onFinish() {
                                clearScreenData()
                            }
                        }
                        countDown?.start()
                        delay(500)
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    4 -> { //支付失败
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUIT_CONFIRM, "payResult"))
                        bulkPayShow = false
                        payViewModel.bulkPayAmount.value = ""
                        countDown?.cancel()
                        binding.paySuccess.visibility = View.GONE
                        binding.payFail.visibility = View.VISIBLE
                        binding.drawCross.clearData()
                        binding.drawCross.invalidate()
                        binding.tvFailMsg.text = "消费失败！${any as String}"
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG, false to "消费失败！${any as String}"))
                        CommonAndDpToPxUtil.speakWork("消费失败！${any as String}")
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(countDownTime) + 200, 1000) {
                            override fun onTick(mil: Long) {}

                            override fun onFinish() {
                                clearScreenData()
                            }
                        }
                        countDown?.start()
                        delay(500)
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    5 -> { //无效码
                        when (any as Int) {
                            1 -> {
                                ToastShowUtil.show("请刷新付款码再支付")
                                CommonAndDpToPxUtil.speakWork("无效码，请刷新付款码再支付")
                            }
                            2 -> {
                                ToastShowUtil.show("请检查网络,不支持离线聚合支付!")
                                CommonAndDpToPxUtil.speakWork("不支持离线聚合支付")
                            }
                            else -> {
                                ToastShowUtil.show("请切换离线码再支付")
                                CommonAndDpToPxUtil.speakWork("请切换离线码再支付")
                            }
                        }
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    6 -> { //异常
                        ToastShowUtil.show("支付异常：${any as? String}")
                        LogUtil.d(TAG, "支付异常：$any")
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    7 -> {
//                        val bean = any as SimpleForUI
//                        val str = when (bean.way!!.toInt()) {
//                            20 -> "微信"
//                            else -> "支付宝"
//                        }
//                        if (bean.state == 0) {
//                            CommonAndDpToPxUtil.speakWork("${str}收款${bean.payment}元")
//                        } else {
//                            CommonAndDpToPxUtil.speakWork("${str}支付失败了")
//                        }
                    }
                    8 -> {
                        countDown?.cancel()
                        val payForUI = any as PayForUI
                        if (payForUI.result == "Y") {
                            binding.paySuccess.visibility = View.VISIBLE
                            binding.payFail.visibility = View.GONE
                            binding.tvSuccessMsg.text = "查询成功！"
                            binding.drawHook.clearData()
                            binding.drawHook.invalidate()

                            addScreenData(payForUI.swForUI)

//                            updateOrderCount()

                            CommonAndDpToPxUtil.speakWork("查询成功！")
                        } else {
                            binding.paySuccess.visibility = View.GONE
                            binding.payFail.visibility = View.VISIBLE
                            binding.drawCross.clearData()
                            binding.drawCross.invalidate()
                            binding.tvFailMsg.text = "查询失败！${any.errMsg}"

                            CommonAndDpToPxUtil.speakWork("查询失败！${any.errMsg}")
                        }
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(countDownTime) + 200, 1000) {
                            override fun onTick(mil: Long) {}

                            override fun onFinish() {
                                clearScreenData()
                            }
                        }
                        countDown?.start()
                        delay(500)
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    9 -> {
                        countDown?.cancel()
                        binding.paySuccess.visibility = View.GONE
                        binding.payFail.visibility = View.VISIBLE
                        binding.drawCross.clearData()
                        binding.drawCross.invalidate()
                        binding.tvFailMsg.text = "查询失败！${any as String}"
                        CommonAndDpToPxUtil.speakWork("查询失败！${any as String}")
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(countDownTime) + 200, 1000) {
                            override fun onTick(mil: Long) {}

                            override fun onFinish() {
                                clearScreenData()
                            }
                        }
                        countDown?.start()
                        delay(500)
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "${e.cause} ${e.message}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateOrderCount() {
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        val breakfastBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "早餐")
        val lunchBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "午餐")
        val dinnerBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "晚餐")
        val supperBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "夜宵")
        binding.tvBreakfastBill.text = "早餐：${breakfastBill.size.toString()} 单"
        binding.tvLunchBill.text = "午餐：${lunchBill.size.toString()} 单"
        binding.tvDinnerBill.text = "晚餐：${dinnerBill.size.toString()} 单"
        binding.tvSupperBill.text = "夜宵：${supperBill.size.toString()} 单"
    }

    @SuppressLint("SetTextI18n")
    private fun addScreenData(swForUI: SwForUI) {
        LogUtil.i(TAG, "addScreenData swForUI: $swForUI")
        binding.tvUsername.text = "姓名：${swForUI.username}"
        binding.tvBalance.text = "余额：${swForUI.balance} 元"
        var breakfastTime = swForUI.breakfastTime
        var lunchTime = swForUI.lunchTime
        var dinnerTime = swForUI.dinnerTime
        var supperTime = swForUI.supperTime
        if (swForUI.result == "Y") {
            //显示餐标
            binding.llRuleInfo.visibility = View.VISIBLE
            if (swForUI.useMealRuleName.isNullOrBlank()) binding.llRuleInfo.visibility = View.GONE
            binding.tvRuleUse.visibility = if (swForUI.restTime == 0) View.GONE else View.VISIBLE
            binding.tvRestTime.visibility = if (swForUI.restTime == 0) View.GONE else View.VISIBLE
            binding.tvRuleSubsidy.visibility = if (swForUI.restTime == 0) View.GONE else View.VISIBLE
            binding.tvMealRule.text = "餐标：${swForUI.useMealRuleName}"
            binding.tvRuleUse.text = "耗次：${swForUI.everyUseTime} 次"
            binding.tvRestTime.text = "余次：${swForUI.restTime - swForUI.everyUseTime.trim().toInt()} 次"
            if ((!swForUI.standardMealName.isNullOrBlank() && swForUI.actualMealName != swForUI.standardMealName)) {
                binding.tvRulePrice.visibility = View.GONE
                if (payViewModel.bulkPayAmount.value.isNullOrBlank()) {
                    binding.postponePay.text = "单价：${swForUI.actualPrice} 元"
                    binding.postponePayInfo.visibility = View.VISIBLE
                }
            } else {
                binding.postponePayInfo.visibility = View.GONE
                binding.postponePay.text = "单价："
                binding.tvRulePrice.visibility = View.VISIBLE
            }
            if (binding.bulkPay.text.contains("元")) {
                if (swForUI.standardMealName.isNullOrBlank()) binding.llRuleInfo.visibility = View.GONE
                else {
                    binding.tvRulePrice.visibility = View.GONE
                    binding.llRuleInfo.visibility = View.VISIBLE
                }
            }
            binding.tvRulePrice.text = "单价：${swForUI.rulePrice.toString()} 元"
            binding.tvRuleSubsidy.text = "补贴：${swForUI.subsidy} 元"

            when (swForUI.standardMealName) {
                "早餐" -> breakfastTime -= swForUI.everyUseTime.trim().toInt()
                "午餐" -> lunchTime -= swForUI.everyUseTime.trim().toInt()
                "晚餐" -> dinnerTime -= swForUI.everyUseTime.trim().toInt()
                "夜宵" -> supperTime -= swForUI.everyUseTime.trim().toInt()
            }
        } else binding.llRuleInfo.visibility = View.GONE
        binding.tvBreakfast.text = "早餐：$breakfastTime 次"
        binding.tvLunch.text = "午餐：$lunchTime 次"
        binding.tvDinner.text = "晚餐：$dinnerTime 次"
        binding.tvSupper.text = "夜宵：$supperTime 次"
    }

    private fun clearScreenData() {

        binding.paySuccess.visibility = View.GONE
        binding.tvSuccessMsg.text = "消费成功！"
        binding.payFail.visibility = View.GONE
        binding.tvFailMsg.text = "消费失败！"

        binding.tvUsername.text = "姓名："
        binding.tvBalance.text = "余额："
        binding.tvBreakfast.text = "早餐："
        binding.tvLunch.text = "午餐："
        binding.tvDinner.text = "晚餐："
        binding.tvSupper.text = "夜宵："

        binding.llRuleInfo.visibility = View.GONE
        binding.tvMealRule.text = "餐标："
        binding.tvRuleUse.text = "耗次："
        binding.tvRestTime.text = "余次："
        binding.tvRulePrice.text = "单价："
        binding.tvRuleSubsidy.text = "补贴："

        binding.postponePayInfo.visibility = View.GONE
        binding.postponePay.text = "单价："

        if (!bulkPayShow) {
            binding.bulkPayInfo.visibility = View.GONE
            binding.bulkPay.text = "零点："
            payViewModel.bulkPayAmount.value = ""
        }
    }

    fun setBulkPayAmount(amount: String, isShow: Boolean) {
        bulkPayShow = true
        binding.bulkPay.text = "零点：$amount 元"
        payViewModel.bulkPayAmount.value = amount
        binding.bulkPayInfo.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    override fun onData(data: String) {
        if (payViewModel.getPayState() == PayViewModel.PayStatus.INVALID || data.isEmpty()) return
        payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
        LogUtil.d(TAG, "二维码 :$data")
        mScope.launch(Dispatchers.IO + mHandler) {
            // 获取custId
            var custId: String? = null
            LogUtil.i(TAG, "申万支付...")
            if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) {
                this@MealTimeDisplay.onOtherListener(4, "当前设备无网络，不能使用餐次模式")
                return@launch
            }
            if (TimeUtil.CurrentTimeSection() == 0 && kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
                this@MealTimeDisplay.onOtherListener(4, "未开餐，不能使用餐次模式")
                return@launch
            }
            custId = payViewModel.getCustId("2", data)
            if (custId.isNullOrBlank()) {
                LogUtil.e(TAG, "未查询到该人员信息：content -> $data, type -> 2")
                this@MealTimeDisplay.onOtherListener(4, "未查询到该人员信息")
                return@launch
            }
            // todo 查询余次
            if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) {
                payViewModel.queryRestTimeAndBalance(custId, "2", data)
                return@launch
            }
            payViewModel.paymentLogicHelper(custId, "2", data, true)
        }
    }

    override fun numberOfIcCard(number: String?) {
        if (payViewModel.getPayState() == PayViewModel.PayStatus.INVALID || number == null) return
        payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
        val icCard = number.trim().uppercase()
        LogUtil.d(TAG, "卡号 :$icCard")
        mScope.launch(Dispatchers.IO + mHandler) {
            // 获取custId
            var custId: String? = null
            LogUtil.i(TAG, "申万支付...")
            if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) {
                this@MealTimeDisplay.onOtherListener(4, "当前设备无网络，不能使用餐次模式")
                return@launch
            }
            if (TimeUtil.CurrentTimeSection() == 0 && kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
                this@MealTimeDisplay.onOtherListener(4, "未开餐，不能使用餐次模式")
                return@launch
            }
            custId = payViewModel.getCustId("3", icCard)
            if (custId.isNullOrBlank()) {
                LogUtil.e(TAG, "未查询到该人员信息：content -> $icCard, type -> 3")
                this@MealTimeDisplay.onOtherListener(4, "未查询到该人员信息")
                return@launch
            }
            // todo 查询余次
            if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) {
                payViewModel.queryRestTimeAndBalance(custId, "3", number)
                return@launch
            }
            payViewModel.paymentLogicHelper(custId, "3", icCard, true)
        }
    }

    private fun querySecondPay(custId: String, mealName: String): Boolean {
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        val swPayOrderTables = DishesDBHelper.getInstance()
            .querySwPayOrderListByCustIdDate(custId, date, mealName)
        LogUtil.i(TAG, "querySecondPay swPayOrderTables.size: ${swPayOrderTables.size}")
        return swPayOrderTables.size > 0
    }

    override fun onFacePay(payForUI: PayForUI) {
        LogUtil.i(TAG, "刷脸消费：${payForUI}")
        payForUI.swForUI.actualMealName = MyApplication.actulMealName
        MyApplication.actulMealName = ""
        payViewModel.showFacePayResult(payForUI)
    }

    override fun onFaceQuery(bean: CcbFacePayResultBean) {
        LogUtil.i(TAG, "查询刷脸：${bean.toString()}")
        payViewModel.queryRestTimeAndBalance("", "1", bean)
    }

    override fun hasAllowance() {

    }

    override fun withoutAllowance(paymentMap: HashMap<String, String>?, payForUI: PayForUI) {
        mScope.launch(Dispatchers.Main + mHandler) {
            val confirmDialog = ConfirmDialog(context)
            confirmDialog.setListener(object : ConfirmDialog.OnConfirmCallback{
                override fun confirmCallback(flag: Boolean) {
                    if (flag) {
                        // 继续支付
                        mScope.launch(Dispatchers.IO + mHandler) {
                            payViewModel.paymentLogicHelper(payForUI.custId, payForUI.payType, payForUI.payContent, true, true, payForUI, paymentMap)
                        }
                    } else {
                        this@MealTimeDisplay.onOtherListener(4, "取消支付")
                    }
                }
            })
            confirmDialog.show()
            confirmDialog.setTextMsg("您在本次餐别已没有优惠，是否继续支付？")
        }
    }


}