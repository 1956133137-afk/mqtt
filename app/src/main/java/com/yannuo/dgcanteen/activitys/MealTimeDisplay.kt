package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.UnderlineSpan
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.proembed.service.MyService
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
import java.math.BigDecimal
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
//        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
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
        faceScanVM.setFaceListener(this)
        if (NetworkStateManager.getInstance().isOnline(context).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }else{
            netWorkStatus("0")
        }
        mXService = MyService(context)
        faceScanVM.bindService()


        viewModel.showErrorToast.observe(activity) {
            ToastShowUtil.show(it)
        }


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
                binding.btnFacePay.text = "刷脸支付"
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
//                    if (kv.decodeInt(Constant.FACE_TIME_AMOUNT_SWITCH, 0) == 1) {
//                        val amount = kv.decodeString(Constant.FACE_TIME_AMOUNT, "")
//                        if (amount.isNullOrBlank()) {
//                            this@MealTimeDisplay.onOtherListener(4, "刷脸定时金额错误")
//                            return@setOnClickListener
//                        }
//                        val queryToMeals =
//                            DishesDBHelper.getInstance().queryToMeals(TimeUtil.CurrentTimeSection())
//                        MyApplication.actulMealName = queryToMeals.mealName
//                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_DISMISS_CALCULATE_PAY_DIALOG, null))
//                        faceScanVM.setTimeOut(30000)
//                        faceScanVM.startFacePay(false, amount)
//                    } else this@MealTimeDisplay.onOtherListener(4, "未确认金额")
                    this@MealTimeDisplay.onOtherListener(4, "未确认金额")
                } else {
//                    if (kv.decodeInt(Constant.FACE_TIME_AMOUNT_SWITCH, 0) == 1) {
//                        this@MealTimeDisplay.onOtherListener(4, "使用键盘输入金额模式，需要先关闭刷脸定时金额模式")
//                        return@setOnClickListener
//                    }
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

    private fun toBillDetail(number: Int) {
        val queryAllMeals = DishesDBHelper.getInstance().queryAllMeals()
        val mealTable = queryAllMeals[number - 1]

    }

    override fun onStart() {
        super.onStart()
        LogUtil.i(TAG, "MealTimeDisplay onStart...")
        payViewModel.mReadCardListener = this
        payViewModel.mScanCodeListener = this
        payViewModel.openPayStatus(Constant.PAY_CODE_IC_TYPE)
        payViewModel.listener = this
        payViewModel.allowanceStateListener = this

        displayMealName()

        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
            binding.btnFacePay.text = "刷脸支付"
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
//                LogUtil.i(TAG, "checkTime mealId: $mMealId")
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
        clearScreenData()
        if (this::awaitPayDialog.isInitialized) awaitPayDialog.cancel()
        payViewModel.closePayStatus()
        payViewModel.listener = null
        payViewModel.allowanceStateListener = null
    }

    override fun dismiss() {
        LogUtil.i(TAG, "MealTimeDisplay dismiss...")
//        NetworkStateManager.getInstance().unRegisterObserver(this)
//        payViewModel.listener = null
        super.dismiss()
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
                        var str = "正在支付"
                        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) str = "正在查询"
                        if (any != null) str = any as String
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
                        LogUtil.i(TAG, "支付结果回显 payForUI：$payForUI")
                        if (payForUI.result == "Y") {
                            binding.paySuccess.visibility = View.VISIBLE
                            binding.payFail.visibility = View.GONE
                            binding.tvSuccessMsg.text = "支付成功 ${payForUI.actualPayment} 元！"
                            EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG, true to "支付成功 ${payForUI.actualPayment} 元！"))
                            binding.drawHook.clearData()
                            binding.drawHook.invalidate()
                            //右上角的icon
                            binding.loadingView.visibility = View.GONE
                            binding.payFailRightTop.visibility = View.GONE
                            binding.paySuccessRightTop.visibility = View.VISIBLE
                            binding.paySuccessRightTop.clearData()
                            binding.paySuccessRightTop.invalidate()
                            binding.payTipRightTop.text = "支付成功"
                            binding.bulkPay.text = "${String.format("%.02f", payForUI.actualPayment.trim().toFloat())}元"

                            addScreenData(payForUI.swForUI)

                            payViewModel.saveSwOrderRecord(payForUI, 1)

                            updateOrderCount()
                            // 更新calculate activity的订单量信息
                            EventBus.getDefault().post(MessageEvent(Constant.UPDATE_MEAL_TIME_BILL, null))

                            CommonAndDpToPxUtil.speakWork("支付成功${payForUI.actualPayment}元！")
                        } else {
                            binding.paySuccess.visibility = View.GONE
                            binding.payFail.visibility = View.VISIBLE
                            binding.drawCross.clearData()
                            binding.drawCross.invalidate()
                            binding.tvFailMsg.text = "支付失败！${any.errMsg}"
                            //右上角的icon
                            binding.loadingView.visibility = View.GONE
                            binding.payFailRightTop.visibility = View.VISIBLE
                            binding.payFailRightTop.clearData()
                            binding.payFailRightTop.invalidate()
                            binding.paySuccessRightTop.visibility = View.GONE
                            binding.payTipRightTop.text = "支付失败"

                            EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG, false to "支付失败！${any.errMsg}"))

                            CommonAndDpToPxUtil.speakWork("支付失败！${any.errMsg}")
                        }
                        val time = kv.decodeLong(Constant.MEAL_TIME_PAY_RESULT_TIME, 10L)
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
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
                        //右上角的icon
                        binding.loadingView.visibility = View.GONE
                        binding.payFailRightTop.visibility = View.VISIBLE
                        binding.payFailRightTop.clearData()
                        binding.payFailRightTop.invalidate()
                        binding.paySuccessRightTop.visibility = View.GONE

                        binding.tvFailMsg.text = "支付失败！${any as String}"
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG, false to "支付失败！${any as String}"))
                        CommonAndDpToPxUtil.speakWork("支付失败！${any as String}")
                        val time = kv.decodeLong(Constant.MEAL_TIME_PAY_RESULT_TIME, 10L)
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
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
                        val time = kv.decodeLong(Constant.MEAL_TIME_QUERY_BALANCE_TIME, 10L)
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
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
                        val time = kv.decodeLong(Constant.MEAL_TIME_QUERY_BALANCE_TIME, 10L)
                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
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
    fun updateOrderCount() {
        if (!this::binding.isInitialized) return
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        val queryAllMeals = DishesDBHelper.getInstance().queryAllMeals()
        if (queryAllMeals.size >= 1) {
            binding.tvMeal01Bill.visibility = View.VISIBLE
            val meal01Bill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, queryAllMeals[0].mealName)
            val text = "${queryAllMeals[0].mealName}：${meal01Bill.size} 单"
//            val spannableString = SpannableString(text).apply {
//                setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//            }
            binding.tvMeal01Bill.text = text
        } else binding.tvMeal01Bill.visibility = View.GONE
        if (queryAllMeals.size >= 2) {
            binding.tvMeal02Bill.visibility = View.VISIBLE
            val meal02Bill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, queryAllMeals[1].mealName)
            binding.tvMeal02Bill.text = "${queryAllMeals[1].mealName}：${meal02Bill.size} 单"
        } else binding.tvMeal02Bill.visibility = View.GONE
        if (queryAllMeals.size >= 3) {
            binding.tvMeal03Bill.visibility = View.VISIBLE
            val meal03Bill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, queryAllMeals[2].mealName)
            binding.tvMeal03Bill.text = "${queryAllMeals[2].mealName}：${meal03Bill.size} 单"
        } else binding.tvMeal03Bill.visibility = View.GONE
        if (queryAllMeals.size >= 4) {
            binding.tvMeal04Bill.visibility = View.VISIBLE
            val meal04Bill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, queryAllMeals[3].mealName)
            binding.tvMeal04Bill.text = "${queryAllMeals[3].mealName}：${meal04Bill.size} 单"
        } else binding.tvMeal04Bill.visibility = View.GONE

    }

    @SuppressLint("SetTextI18n")
    private fun addScreenData(swForUI: SwForUI) {
        LogUtil.i(TAG, "addScreenData swForUI: $swForUI")
        binding.tvUsername.text = swForUI.username
        val balance = BigDecimal(swForUI.balance)
        binding.tvBalance.text = "${balance.setScale(2, BigDecimal.ROUND_HALF_UP)} 元"
        val queryAllMeals = DishesDBHelper.getInstance().queryAllMeals()
        var meal01Time = swForUI.meal01Time
        var meal02Time = swForUI.meal02Time
        var meal03Time = swForUI.meal03Time
        var meal04Time = swForUI.meal04Time
        if (swForUI.result == "Y") {
            //显示餐标
            binding.llRuleInfo.visibility = View.VISIBLE
            if (swForUI.useMealRuleName.isNullOrBlank() ) binding.llRuleInfo.visibility = View.GONE
            binding.tvRuleUse.visibility = if (swForUI.restTime == 0 && swForUI.everyUseTime != "0") View.GONE else View.VISIBLE
            binding.tvRestTime.visibility = if (swForUI.restTime == 0) View.GONE else View.VISIBLE
            binding.tvRuleSubsidy.visibility = if (swForUI.restTime == 0 && swForUI.everyUseTime != "0") View.GONE else View.VISIBLE
            binding.tvMealRule.text = "餐标：${swForUI.useMealRuleName}"
            binding.tvRuleUse.text = "耗次：${swForUI.everyUseTime} 次"
            binding.tvRestTime.text = "日余次：${swForUI.restTime - swForUI.everyUseTime.trim().toInt()} 次"
            if ((!swForUI.standardMealName.isNullOrBlank() && swForUI.actualMealName != swForUI.standardMealName) || swForUI.useMealRuleName == "无补贴") {
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
            if (binding.bulkPay.text.contains("元") && binding.bulkPayInfo.visibility == View.VISIBLE) {
                if (swForUI.standardMealName.isNullOrBlank()) binding.llRuleInfo.visibility = View.GONE
                else {
                    binding.tvRulePrice.visibility = View.GONE
                    binding.llRuleInfo.visibility = View.VISIBLE
                }
            }
            binding.tvRulePrice.text = "单价：${swForUI.rulePrice.toString()} 元"
            binding.tvRuleSubsidy.text = "补贴：${swForUI.subsidy} 元"

            var no = -1
            for ((i, v) in queryAllMeals.withIndex()) {
                if (v.mealName == swForUI.standardMealName ) {
                    no = i + 1
                }
            }
            when (no) {
                1 -> meal01Time -= swForUI.everyUseTime.trim().toInt()
                2 -> meal02Time -= swForUI.everyUseTime.trim().toInt()
                3 -> meal03Time -= swForUI.everyUseTime.trim().toInt()
                4 -> meal04Time -= swForUI.everyUseTime.trim().toInt()
            }
        } else binding.llRuleInfo.visibility = View.GONE
        val size = queryAllMeals.size

        if (size <= 0) binding.tvMeal01.visibility = View.INVISIBLE
        else {
            binding.tvMeal01.visibility = View.VISIBLE
            binding.tvMeal01.text = "${queryAllMeals[0].mealName}：$meal01Time 次"
        }
        if (size <= 1) binding.tvMeal02.visibility = View.INVISIBLE
        else {
            binding.tvMeal02.visibility = View.VISIBLE
            binding.tvMeal02.text = "${queryAllMeals[1].mealName}：$meal02Time 次"
        }
        if (size <= 2) binding.tvMeal03.visibility = View.INVISIBLE
        else {
            binding.tvMeal03.visibility = View.VISIBLE
            binding.tvMeal03.text = "${queryAllMeals[2].mealName}：$meal03Time 次"
        }
        if (size <= 3) binding.tvMeal04.visibility = View.INVISIBLE
        else {
            binding.tvMeal04.visibility = View.VISIBLE
            binding.tvMeal04.text = "${queryAllMeals[3].mealName}：$meal04Time 次"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun clearScreenData() {


        binding.paySuccess.visibility = View.GONE
        binding.tvSuccessMsg.text = "支付成功！"
        binding.payFail.visibility = View.GONE
        binding.tvFailMsg.text = "支付失败！"

        binding.tvUsername.text = ""
        binding.tvBalance.text = ""

        displayMealName()

        binding.llRuleInfo.visibility = View.GONE
        binding.tvMealRule.text = "餐标："
        binding.tvRuleUse.text = "耗次："
        binding.tvRestTime.text = "日余次："
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

    fun displayMealName() {
        if (!this::binding.isInitialized) return
        val queryAllMeals = DishesDBHelper.getInstance().queryAllMeals()
        LogUtil.i(TAG, "queryAllMeals: $queryAllMeals")
        val size = queryAllMeals.size

        if (size <= 0) binding.tvMeal01.visibility = View.INVISIBLE
        else {
            binding.tvMeal01.visibility = View.VISIBLE
            binding.tvMeal01.text = "${queryAllMeals[0].mealName}："
        }
        if (size <= 1) binding.tvMeal02.visibility = View.INVISIBLE
        else {
            binding.tvMeal02.visibility = View.VISIBLE
            binding.tvMeal02.text = "${queryAllMeals[1].mealName}："
        }
        if (size <= 2) binding.tvMeal03.visibility = View.INVISIBLE
        else {
            binding.tvMeal03.visibility = View.VISIBLE
            binding.tvMeal03.text = "${queryAllMeals[2].mealName}："
        }
        if (size <= 3) binding.tvMeal04.visibility = View.INVISIBLE
        else {
            binding.tvMeal04.visibility = View.VISIBLE
            binding.tvMeal04.text = "${queryAllMeals[3].mealName}："
        }
    }

    fun setBulkPayAmount(amount: String, isShow: Boolean) {
        bulkPayShow = true
        binding.bulkPay.text = amount
        payViewModel.bulkPayAmount.value = amount.replace("元", "")
        binding.bulkPayInfo.visibility = if (isShow) View.VISIBLE else View.GONE
        binding.loadingView.visibility = View.VISIBLE
        binding.paySuccessRightTop.visibility = View.GONE
        binding.payFailRightTop.visibility = View.GONE
        binding.payTipRightTop.text = "请支付"
    }

    override fun onData(data: String) {
        if (payViewModel.getPayState() == PayViewModel.PayStatus.INVALID || data.isEmpty()) return
        payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
        LogUtil.d(TAG, "二维码 :$data")
        mScope.launch(Dispatchers.IO + mHandler) {
            try {
                // 获取custId
                var custId: String? = null
                LogUtil.i(TAG, "申万支付...")
                if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) {
                    if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) this@MealTimeDisplay.onOtherListener(9, "当前设备无网络，不能使用餐次模式")
                    else this@MealTimeDisplay.onOtherListener(4, "当前设备无网络，不能使用餐次模式")
                    return@launch
                }
                if (TimeUtil.CurrentTimeSection() == 0 && kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
                    this@MealTimeDisplay.onOtherListener(4, "未开餐，不能使用餐次模式")
                    return@launch
                }
                this@MealTimeDisplay.onOtherListener(1, "确认人员中")
                custId = payViewModel.getCustId("2", data)
                if (custId.isNullOrBlank()) {
                    LogUtil.e(TAG, "未查询到该人员信息：content -> $data, type -> 2")
                    if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) this@MealTimeDisplay.onOtherListener(9, "未查询到该人员信息")
                    else this@MealTimeDisplay.onOtherListener(4, "未查询到该人员信息")
                    return@launch
                }
                // todo 查询余次
                if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) {
                    payViewModel.queryRestTimeAndBalance(custId, "2", data)
                    return@launch
                }
                payViewModel.paymentLogicHelper(custId, "2", data, true)
            } catch (e: java.lang.Exception) {
                LogUtil.i(TAG, "error: ${e.message}")
                e.printStackTrace()
                if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) this@MealTimeDisplay.onOtherListener(9, e.message)
                else this@MealTimeDisplay.onOtherListener(4, e.message)
            }
        }
    }

    override fun numberOfIcCard(number: String?) {
        if (payViewModel.getPayState() == PayViewModel.PayStatus.INVALID || number == null) return
        payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
        val icCard = number.trim().uppercase()
        LogUtil.d(TAG, "卡号 :$icCard")
        mScope.launch(Dispatchers.IO + mHandler) {
            try {
                // 获取custId
                var custId: String? = null
                LogUtil.i(TAG, "申万支付...")
                if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) {
                    if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) this@MealTimeDisplay.onOtherListener(9, "当前设备无网络，不能使用餐次模式")
                    else this@MealTimeDisplay.onOtherListener(4, "当前设备无网络，不能使用餐次模式")
                    return@launch
                }
                if (TimeUtil.CurrentTimeSection() == 0 && kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 0) {
                    this@MealTimeDisplay.onOtherListener(4, "未开餐，不能使用餐次模式")
                    return@launch
                }
                this@MealTimeDisplay.onOtherListener(1, "确认人员中")
                custId = payViewModel.getCustId("3", number)
                if (custId.isNullOrBlank()) {
                    if (custId == null) return@launch
                    LogUtil.e(TAG, "未查询到该人员信息：content -> $icCard, type -> 3")
                    if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) this@MealTimeDisplay.onOtherListener(9, "未查询到该人员信息")
                    else this@MealTimeDisplay.onOtherListener(4, "未查询到该人员信息")
                    return@launch
                }
                // todo 查询余次
                if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) {
                    payViewModel.queryRestTimeAndBalance(custId, "3", number)
                    return@launch
                }
                payViewModel.paymentLogicHelper(custId, "3", icCard, true)
            } catch (e: java.lang.Exception) {
                LogUtil.i(TAG, "error: ${e.message}")
                e.printStackTrace()
                if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) this@MealTimeDisplay.onOtherListener(9, e.message)
                else this@MealTimeDisplay.onOtherListener(4, e.message)
            }
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
        LogUtil.i(TAG, "刷脸支付：${payForUI}")
        payForUI.swForUI.actualMealName = MyApplication.actulMealName
        MyApplication.actulMealName = ""
        payViewModel.showFacePayResult(payForUI)
    }

    override fun onFaceQuery(bean: CcbFacePayResultBean) {
        LogUtil.i(TAG, "查询刷脸：${bean.toString()}")
        payViewModel.queryRestTimeAndBalance("", "1", bean)
    }

    override fun hasAllowance(paymentMap: HashMap<String, String>?, payForUI: PayForUI) {
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
            confirmDialog.setTextMsg("您的本次支付将由卡内余额支付${payForUI.actualPayment}元，是否继续支付？")
        }
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
            confirmDialog.setTextMsg("您的本次支付将由卡内余额支付${payForUI.actualPayment}元，是否继续支付？")
        }
    }


}