package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.format.DateFormat
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.MealTimeVM
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.adapters.OrderDishCountAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.PeriodicVerificationReceiver
import com.yannuo.dgcanteen.databinding.ActivityCalculateBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.PayResultDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:46
 **/
class CalculateActivity : BaseActivity<ActivityCalculateBinding>(), NetworkStateManager.NetWorkListener {

    private val mXService by lazy { MyService(this) }
    private var navigation = true
    private var awaitPayDialog: AwaitingDialog? = null
    private var payResultDialog: PayResultDialog? = null
    private val passwordDialog by lazy { PasswordDialog(this) }
    private val confirmDialog by lazy { ConfirmDialog(this) }
    private val kv: MMKV = MMKV.defaultMMKV()

    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private var simpleDisplay: SimpleDisplay? = null
    @Volatile
    private var mealTimeDisplay: MealTimeDisplay? = null

    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private lateinit var maps: MutableMap<String, Int>
    private var lastTime = 0L  //上次触发时间
    private var mealId = 0
    private val orderCountAdapter by lazy { OrderDishCountAdapter() }
    private val verificationVM by lazy { ViewModelProvider(this)[VerificationVM::class.java] }
    private val productsVM by lazy { ProductsVM() }
    private val mealTimeVM by lazy {
        ViewModelProvider(this)[MealTimeVM::class.java]
    }
    private val periodicVerificationReceiver = PeriodicVerificationReceiver()
    private var isPayStatus = false

    override fun bindLayout() {
        binding = ActivityCalculateBinding.inflate(layoutInflater)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onInit() {
        mScope = CoroutineScope(Dispatchers.IO)
        initObject()
        initView()
        initEvent()
        checkTime()
        registerVerificationReceiver()
        scheduleVerification()
    }

    //触发定时任务：每15分钟执行一次进行查询
    private fun scheduleVerification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.yannuo.dgcanteen.PERIODIC_VERIFICATION")
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val intervalMillis = 15 * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, pendingIntent)
    }

    //注册查询核销的广播
    private fun registerVerificationReceiver() {
        val filter = IntentFilter("com.yannuo.dgcanteen.PERIODIC_VERIFICATION")
        registerReceiver(periodicVerificationReceiver, filter)
    }



    @RequiresApi(Build.VERSION_CODES.N)
    private fun initObject() {
        productsVM.upDataDishes(true)
        mealId = TimeUtil.CurrentTimeSection()
        LogUtil.d(TAG, "mealId:$mealId")
        EventBus.getDefault().register(this)
        NetworkStateManager.getInstance().registerObserver(this)
        kv.encode(Constant.BTN_CONFIRM_STATE, 0)
        awaitPayDialog = AwaitingDialog(this)
        payResultDialog = PayResultDialog(this)

        maps = mutableMapOf(
            "刷脸" to Constant.PAY_FACE_TYPE,
            "刷卡" to Constant.PAY_IC_TYPE,
            "扫码" to Constant.PAY_CODE_TYPE,
            "刷卡扫码" to Constant.PAY_CODE_IC_TYPE,
        )
        val type = when (kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)) {
            Constant.PAY_FACE_TYPE -> "刷脸支付"
            Constant.PAY_IC_TYPE -> "刷卡支付"
            Constant.PAY_CODE_TYPE -> "扫码支付"
            else -> "刷卡扫码支付"
        }
        maps.remove(type)
        binding.btnVerify.text = if (kv.decodeInt(Constant.VERIFY_MODE) == 0) "刷脸核销" else "订餐核销"
        initVerify()
        kv.encode(Constant.VERIFY_CHANGE, false)

        binding.rvDishOrder.layoutManager = LinearLayoutManager(this)
        binding.rvDishOrder.adapter = orderCountAdapter
        verificationVM.getDishesCountForUI().observe(this) { value ->
            var totalOrderNum = 0
            var verifyTotalOrderNum = 0
            var unVerifyTotalOrderNum = 0
            value.needVerifyTotal.forEach {
                totalOrderNum += it.dishesNum
                it.flag = 0
            }
            value.verifyTotal.forEach {
                verifyTotalOrderNum += it.dishesNum
                it.flag = 1
            }
            value.unVerifyTotal.forEach {
                unVerifyTotalOrderNum += it.dishesNum
                it.flag = 2
            }
            binding.tvTotalOrder.text = totalOrderNum.toString()
            binding.tvTotalVerify.text = verifyTotalOrderNum.toString()
            binding.tvUnVerify.text = unVerifyTotalOrderNum.toString()

            orderCountAdapter.data = value.needVerifyTotal
            orderCountAdapter.addData(value.verifyTotal)
            orderCountAdapter.addData(value.unVerifyTotal)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        initPresentation()
        btnViewChange(binding.btnFixPay, Constant.QUOTA_SWITCH)
        btnViewChange(binding.btnOff, Constant.SWITCH)
        btnViewChange(binding.btnMealTimeMode, Constant.MEAL_TIME_MODE)
        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        } else {
            netWorkStatus("0")
        }
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}\nv${packageManager.getPackageInfo(packageName, 0).versionName}"

        val boolean = kv.decodeInt(Constant.MEAL_TIME_SWITCH, 0) == 1
        binding.btnMealTimeMode.visibility = if (boolean) View.VISIBLE else View.GONE
        binding.btnSetting.width = if (boolean) WindowManager.LayoutParams.WRAP_CONTENT else WindowManager.LayoutParams.MATCH_PARENT
    }

    private fun initPresentation() {
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        if (simpleDisplay == null) {
            simpleDisplay = SimpleDisplay(this, secondDisplays)
            simpleDisplay?.show()
            simpleDisplay?.setActivity(this)
        }
    }

    override fun onResume() {
//        initPresentation()
//        LogUtil.i(TAG,"onResume!")
        super.onResume()
        isPayStatus = false
//        productsVM.upDataDishes(true)
        mealId = TimeUtil.CurrentTimeSection()
        mXService?.hideNavBar = true
        val limit = kv.decodeInt(Constant.USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH, 0)
        if (limit == 0) {
            binding.mealTime.visibility = View.INVISIBLE
        } else {
            binding.mealTime.visibility = View.VISIBLE
        }
        if (simpleDisplay?.isShowing != true) simpleDisplay?.show()
        simpleDisplay?.verifyListView()

        mealTimeDisplay?.safeCancel()
        mealTimeDisplay = MealTimeDisplay(this, secondDisplays)
        if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1) {
            simpleDisplay?.dismiss()
            mealTimeDisplay?.show()
        }
        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) {
            if (awaitPayDialog?.isShowing != true) {
                awaitPayDialog?.show()
                awaitPayDialog?.updateText("查询余次中")
            }
        } else awaitPayDialog?.dismiss()

        maps = mutableMapOf(
            "刷脸支付" to Constant.PAY_FACE_TYPE,
            "刷卡支付" to Constant.PAY_IC_TYPE,
            "扫码支付" to Constant.PAY_CODE_TYPE,
            "刷卡扫码支付" to Constant.PAY_CODE_IC_TYPE,
        )
        val type = when (kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)) {
            Constant.PAY_FACE_TYPE -> "刷脸支付"
            Constant.PAY_IC_TYPE -> "刷卡支付"
            Constant.PAY_CODE_TYPE -> "扫码支付"
            else -> "刷卡扫码支付"
        }
        maps.remove(type)
        maps.entries.forEachIndexed { index, it ->
            when (index) {
                0 -> binding.btnFirst.text = it.key
                1 -> binding.btnSecond.text = it.key
                2 -> binding.btnThird.text = it.key
            }
        }
        //自动核销
        if (!kv.decodeBool(Constant.VERIFY_CHANGE, false) && kv.decodeBool(Constant.AUTO_VERIFY, false)) {
            if ((System.currentTimeMillis() - lastTime) > 1000 && judgePayStatus()) {
                lastTime = System.currentTimeMillis()
                kv.encode(Constant.VERIFY_CHANGE, true)
                startActivity(Intent(this, FaceVerificationActivity::class.java))
                simpleDisplay?.dismiss()
            }
        }
        //更新餐次订单量信息
        if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1) {
            binding.mealTimeBill.visibility = View.VISIBLE
            updateMealTimeBill()
        } else binding.mealTimeBill.visibility = View.GONE
    }

    fun initVerify() {
        if (kv.decodeBool(Constant.CODE_VERIFICATION_SET)) {
            binding.verifyShow.visibility = View.VISIBLE
            verificationVM.getDishesCountOfWindow()
        } else binding.verifyShow.visibility = View.GONE
    }

    override fun onStop() {
        binding.btnConfirm.setBackgroundResource(R.drawable.click_button)
        binding.btnConfirm.setTextColor(Color.BLACK)
        binding.btnConfirm.text = "确认金额"
        kv.encode(Constant.BTN_CONFIRM_STATE, 0)
        simpleDisplay?.dismiss()
        mealTimeDisplay?.dismiss()
        LogUtil.i(TAG, "onstop!")
        super.onStop()
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }

        binding.btnFixPay.setOnClickListener { //固定金额
            if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1) {
                ToastShowUtil.show("餐次模式下不能使用该功能")
                return@setOnClickListener
            }
            kv.encode(Constant.QUOTA_SWITCH, !kv.decodeBool(Constant.QUOTA_SWITCH, false))
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUOTA_CHANGE, null))
        }

        binding.btnOff.setOnClickListener { //开启离线模式
            if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1) {
                ToastShowUtil.show("餐次模式下不能使用该功能")
                return@setOnClickListener
            }
            if (!kv.decodeBool(Constant.SWITCH, false)) {
                confirmDialog.apply {
                    show()
                    binding.tvText.text = "您确定开启离线模式吗"
                    setListener(object : ConfirmDialog.OnConfirmCallback {
                        override fun confirmCallback(flag: Boolean) {
                            if (flag) {
                                kv.encode(Constant.SWITCH, true)
                                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
                            }
                        }
                    })
                }
            } else {
                kv.encode(Constant.SWITCH, false)
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
            }
        }

        binding.btnSetting.setOnClickListener {//设置界面
            passwordDialog.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@CalculateActivity, SettingActivity::class.java))
                    }
                })
            }
        }
        binding.btnConfirm.setOnClickListener {
            // 确定金额
            if ((System.currentTimeMillis() - lastTime) < 1000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            if (kv.decodeInt(Constant.BTN_CONFIRM_STATE, 0) == 1) {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUIT_CONFIRM, null))
                return@setOnClickListener
            }
            // 核销界面
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY, null))
            // 餐次模式
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_MEAL_TIME_BULK_PAY, null))
        }

        binding.btnFirst.setOnClickListener {
            // 刷脸
            if ((System.currentTimeMillis() - lastTime) < 1000 || !judgePayStatus()) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            // 收款模式
            payMoney(binding.btnFirst.text.trim())
        }
        binding.btnSecond.setOnClickListener {
            // 刷卡
            if ((System.currentTimeMillis() - lastTime) < 1000 || !judgePayStatus()) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            // 收款模式
            payMoney(binding.btnSecond.text.trim())
        }
        binding.btnThird.setOnClickListener {
            // 扫码
            if ((System.currentTimeMillis() - lastTime) < 1000 || !judgePayStatus()) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            // 收款模式
            payMoney(binding.btnThird.text.trim())
        }

        binding.btnVerify.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 1000 || !judgePayStatus()) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            if (kv.decodeInt(Constant.VERIFY_MODE) == 0) startActivity(Intent(this, FaceVerificationActivity::class.java))
            else startActivity(Intent(this, CardVerificationActivity::class.java))
            handler.postDelayed({ simpleDisplay?.dismiss() }, 250)
        }

        binding.verifyView.setOnLongClickListener {
            initVerify()
            true
        }

        binding.btnMealTimeMode.setOnClickListener {
            // 打开餐次模式
//            ToastShowUtil.show("打开餐次模式")
//            val intent = Intent(this, MealTimeActivity::class.java)
//            startActivity(intent)
            if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 0) {
                confirmDialog.apply {
                    show()
                    binding.tvText.text = "您确定开启餐次模式吗"
                    setListener(object : ConfirmDialog.OnConfirmCallback {
                        override fun confirmCallback(flag: Boolean) {
                            if (flag) {
                                kv.encode(Constant.MEAL_TIME_MODE, 1)
                                EventBus.getDefault().post(MessageEvent(Constant.EVENT_MEAL_TIME_MODE, null))

                                simpleDisplay?.dismiss()
                                mealTimeDisplay?.dismiss()
                                mealTimeDisplay?.show()
                            }
                        }
                    })
                }
            } else {
                kv.encode(Constant.MEAL_TIME_MODE, 0)
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_MEAL_TIME_MODE, null))
                mealTimeDisplay?.dismiss()
//                simpleDisplay?.dismiss()
//                simpleDisplay = SimpleDisplay(this, secondDisplays)
//                simpleDisplay.setActivity(this)
                simpleDisplay?.show()
            }
//            val mode = kv.decodeInt(Constant.MEAL_TIME_MODE, 0)
//            if (mode == 0) kv.encode(Constant.MEAL_TIME_MODE, 1)
//            else kv.encode(Constant.MEAL_TIME_MODE, 0)
//
//            updateBtnText()
        }
    }

    private fun mealTimePay(key: CharSequence) {
        val isUseMeal = kv.decodeInt(Constant.IS_USE_MEAL)
        if (isUseMeal == 1) {
            // 开餐

            if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)
                && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
            ) { //网络监听
                CommonAndDpToPxUtil.speakWork("设备没有网络，餐次模式暂不支持离线模式")
                ToastShowUtil.show("设备没有网络，餐次模式暂不支持离线模式")
                return
            }

            val bean = OrderPayInfo().apply {
                type = maps[key] ?: Constant.MEAL_TIME_CODE_TYPE
                isAllowance = 1
                orderFlag = "sw"
            }

            val payIntent = Intent(this@CalculateActivity, HostActivity::class.java)
            payIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            payIntent.putExtra(Constant.PAY_DATE, Gson().toJson(bean))
            startActivity(payIntent)

        } else {
            // 未开餐
            CommonAndDpToPxUtil.speakWork("餐别未开餐")
            ToastShowUtil.show("餐别未开餐")
        }
    }

    private fun payMoney(key: CharSequence) {
        val limit = kv.decodeInt(Constant.USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH, 0)
        val isUseMeal = kv.decodeInt(Constant.IS_USE_MEAL)
        if (limit == 1) {
            // 开启限制
            if (isUseMeal == 1) {
                // 开餐
                maps[key].also {
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
                }
            } else {
                // 未开餐
                CommonAndDpToPxUtil.speakWork("餐别未开餐")
                ToastShowUtil.show("餐别未开餐")
            }
        } else {
            // 未开限制
            maps[key].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
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

    private fun updateMealTimeBill() {
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

    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventCalculate(event: MessageEvent) {
        when (event.code) {
            Constant.EVENT_OFF_CHANGE -> handler.post {
                btnViewChange(binding.btnOff, Constant.SWITCH)
            }
            Constant.EVENT_QUOTA_CHANGE -> handler.post {
                btnViewChange(binding.btnFixPay, Constant.QUOTA_SWITCH)
            }
            Constant.EVENT_MEAL_TIME_MODE -> handler.post {
                btnViewChange(binding.btnMealTimeMode, Constant.MEAL_TIME_MODE)
            }
            Constant.EVENT_TENTH -> handler.post {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收mqtt状态变更事件~")
                val connect = event.any as Boolean
                if (connect) {
                    binding.server.setImageResource(R.drawable.ic_server)
                } else {
                    binding.server.setImageResource(R.drawable.ic_server_no)
                }
            }
            Constant.EVENT_OPEN_BTN -> handler.post {
                if (simpleDisplay?.isShowing == true) {
                    val limitStr = kv.decodeString(Constant.LIMIT_AMOUNT, "30").toString()
                    val limitAmount = String.format(Locale.CHINA, "%.02f", limitStr.toFloat()).toFloat()
                    val amount = event.any as String
                    if (amount.toFloat() > limitAmount) {
                        ToastShowUtil.show("单笔金额不得超过 $limitAmount 元")
                        CommonAndDpToPxUtil.speakWork("单笔金额不得超过 $limitAmount 元")
                        return@post
                    }
                    binding.btnConfirm.setBackgroundResource(R.drawable.click_button_gred)
                    binding.btnConfirm.setTextColor(Color.WHITE)
                    binding.btnConfirm.text="取消收款￥${event.any as String}"
                    simpleDisplay?.enableBtn(event.any as String, true)
                }
            }
            Constant.EVENT_SHOW_BULK_PAYMENT -> handler.post {
                if (mealTimeDisplay?.isShowing == true) {
                    val limitStr = kv.decodeString(Constant.LIMIT_AMOUNT, "30").toString()
                    val limitAmount = String.format(Locale.CHINA, "%.02f", limitStr.toFloat()).toFloat()
                    val amount = event.any as String
                    if (amount.toFloat() > limitAmount) {
                        ToastShowUtil.show("单笔金额不得超过 $limitAmount 元")
                        CommonAndDpToPxUtil.speakWork("单笔金额不得超过 $limitAmount 元")
                        return@post
                    }
                    binding.btnConfirm.setBackgroundResource(R.drawable.click_button_gred)
                    binding.btnConfirm.setTextColor(Color.WHITE)
                    binding.btnConfirm.text="取消收款￥${event.any as String}"
                    mealTimeDisplay?.setBulkPayAmount("${event.any}元", true)
                }
            }
            Constant.EVENT_QUIT_CONFIRM -> handler.post {
                kv.encode(Constant.BTN_CONFIRM_STATE, 0)
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_KEYBOARD_CANCEL, null))
                binding.btnConfirm.setBackgroundResource(R.drawable.click_button)
                binding.btnConfirm.setTextColor(Color.parseColor("#4F4F4F"))
                binding.btnConfirm.text="确定金额"
                if (simpleDisplay?.isShowing == true) {
                    simpleDisplay?.enableBtn("", false)
                } else if (mealTimeDisplay?.isShowing == true) {
                    if (event.any as String? != "payResult") {
                        mealTimeDisplay?.setBulkPayAmount("", false)
                    }
//                    mealTimeDisplay?.setBulkPayAmount("", false)
                }
            }
            Constant.EVENT_OFLINE_CHANGE -> handler.post {
                btnViewChange(binding.btnOff, Constant.SWITCH)
            }
            Constant.EVENT_CODE -> handler.post {
                simpleDisplay?.dismiss()
                CommonAndDpToPxUtil.speakWork("请出示核销码或者刷卡")
                val i = Intent(this, CardVerificationActivity::class.java)
                startActivity(i)
            }
            Constant.EVENT_FACE -> handler.post {
                if ((System.currentTimeMillis() - lastTime) > 1000 && judgePayStatus()) {
                    lastTime = System.currentTimeMillis()
                    LogUtil.d(TAG, "EventBus : ${event.code} 接收开启刷脸核销事件")
                    CommonAndDpToPxUtil.speakWork("请刷脸进行核销")
                    startActivity(Intent(this, FaceVerificationActivity::class.java))
                    handler.postDelayed({ simpleDisplay?.dismiss() }, 250)
                }
            }
            Constant.EVENT_VERIFY_CHANGE -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收订餐核销更新UI ${event.any}")
                if (event.any as Boolean) handler.post {
                    initVerify()
                    simpleDisplay?.verifyView()
                }
            }
            Constant.EVENT_SECOND -> handler.post {
                if ((System.currentTimeMillis() - lastTime) > 1000 && judgePayStatus()) {
                    lastTime = System.currentTimeMillis()
                    val type = kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)
                    LogUtil.d(TAG, "支付方式：$type")
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, type))
                }
            }
            Constant.EVENT_FACE_STATUS -> isPayStatus = false
            Constant.EVENT_SHOW_CALCULATE_AWAIT_DIALOG -> handler.post{
                if (awaitPayDialog?.isShowing != true) {
                    awaitPayDialog?.show()
                    awaitPayDialog?.updateText(event.any as String)
                }
            }
            Constant.EVENT_DISMISS_CALCULATE_AWAIT_DIALOG -> handler.post{
                awaitPayDialog?.dismiss()
            }
            Constant.EVENT_SHOW_CALCULATE_PAY_DIALOG -> handler.post {
                payResultDialog?.dismiss()
                val data = event.any as Pair<Boolean, String>
                payResultDialog?.show(data.first, data.second)
            }
            Constant.EVENT_DISMISS_CALCULATE_PAY_DIALOG -> handler.post {
                payResultDialog?.dismiss()
            }
            Constant.UPDATE_MEAL_TIME_BILL -> handler.post{
                updateMealTimeBill()
            }
            Constant.EVENT_FIFTH -> handler.post {
                mealTimeDisplay?.displayMealName()
                mealTimeDisplay?.updateOrderCount()
                btnViewChange(binding.btnMealTimeMode, Constant.MEAL_TIME_MODE)
            }
            Constant.EVENT_MEAL_TIME_SWITCH -> handler.post {
                val boolean = kv.decodeInt(Constant.MEAL_TIME_SWITCH, 0) == 1
                binding.btnMealTimeMode.visibility = if (boolean) View.VISIBLE else View.GONE
                binding.btnSetting.width = if (boolean) WindowManager.LayoutParams.WRAP_CONTENT else WindowManager.LayoutParams.MATCH_PARENT
            }
        }
    }

    private fun judgePayStatus(): Boolean {
        if (!isPayStatus) {
            isPayStatus = true
            return true
        }
        return false
    }

    private fun btnViewChange(button: Button, constant: String) {
        when (constant) {
            Constant.SWITCH -> {
                button.apply {
                    if (kv.decodeBool(Constant.SWITCH, false)) {
                        text = "离线模式开启"
                        setBackgroundResource(R.drawable.click_button_blue)
                        setTextColor(Color.parseColor("#FFFFFF"))
                    } else {
                        text = "离线模式关闭"
                        setBackgroundResource(R.drawable.click_button_white)
                        setTextColor(Color.parseColor("#4F4F4F"))
                    }
                }
            }
            Constant.QUOTA_SWITCH -> {
                button.apply {
                    if (kv.decodeBool(Constant.QUOTA_SWITCH, false)) {
                        text = "定额收款开启"
                        setBackgroundResource(R.drawable.click_button_blue)
                        setTextColor(Color.parseColor("#FFFFFF"))
                    } else {
                        text = "定额收款关闭"
                        setBackgroundResource(R.drawable.click_button_white)
                        setTextColor(Color.parseColor("#4F4F4F"))
                    }
                }
            }
            Constant.MEAL_TIME_MODE -> {
                button.apply {
                    if (kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1) {
                        setBackgroundResource(R.drawable.click_button_blue)
                        setTextColor(Color.parseColor("#FFFFFF"))

                        binding.btnFirst.visibility = View.GONE
                        binding.btnSecond.visibility = View.GONE
                        binding.btnThird.visibility = View.GONE

                        // 显示单量
                        binding.mealTimeBill.visibility = View.VISIBLE
                        updateMealTimeBill()

                    } else {
                        setBackgroundResource(R.drawable.click_button)
                        setTextColor(Color.parseColor("#4F4F4F"))

                        binding.btnFirst.visibility = View.VISIBLE
                        binding.btnSecond.visibility = View.VISIBLE
                        binding.btnThird.visibility = View.VISIBLE

                        // 消失单量
                        binding.mealTimeBill.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun netWorkStatus(statue: String) {
        handler.post {
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

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        passwordDialog.cancel()
        confirmDialog.cancel()
        simpleDisplay?.safeCancel()
        mealTimeDisplay?.safeCancel()
        NetworkStateManager.getInstance().unRegisterObserver(this)
        EventBus.getDefault().unregister(this)
        unregisterReceiver(periodicVerificationReceiver)
    }
}