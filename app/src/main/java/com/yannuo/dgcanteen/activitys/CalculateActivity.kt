package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Handler
import android.view.Display
import android.view.View
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.adapters.OrderDishCountAdapter
import com.yannuo.dgcanteen.common.PeriodicVerificationReceiver
import com.yannuo.dgcanteen.databinding.ActivityCalculateBinding
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
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
    private val passwordDialog by lazy { PasswordDialog(this) }
    private val confirmDialog by lazy { ConfirmDialog(this) }
    private val kv: MMKV = MMKV.defaultMMKV()

    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private var simpleDisplay: SimpleDisplay? = null

    private val handler = Handler()
    private lateinit var maps: MutableMap<String, Int>
    private var lastTime = 0L  //上次触发时间
    private var mealId = 0
    private val orderCountAdapter by lazy { OrderDishCountAdapter() }
    private val viewModel by lazy { ViewModelProvider(this)[VerificationVM::class.java] }
    private val productsVM by lazy { ProductsVM() }
    private val periodicVerificationReceiver = PeriodicVerificationReceiver()

    override fun bindLayout() {
        binding = ActivityCalculateBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initView()
        initEvent()
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


    private fun initObject() {
        productsVM.upDataDishes(true)
        mealId = TimeUtil.CurrentTimeSection()
        LogUtil.d(TAG, "mealId:$mealId")
        EventBus.getDefault().register(this)
        NetworkStateManager.getInstance().registerObserver(this)

        maps = mutableMapOf(
            "刷脸" to Constant.PAY_FACE_TYPE,
            "刷卡" to Constant.PAY_IC_TYPE,
            "扫码" to Constant.PAY_CODE_TYPE,
            "刷卡扫码" to Constant.PAY_CODE_IC_TYPE,
        )
        val type = when (kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)) {
            Constant.PAY_FACE_TYPE -> "刷脸"
            Constant.PAY_IC_TYPE -> "刷卡"
            Constant.PAY_CODE_TYPE -> "扫码"
            else -> "刷卡扫码"
        }
        maps.remove(type)
        binding.btnVerify.text = if (kv.decodeInt(Constant.VERIFY_MODE) == 0) "刷脸核销" else "订餐核销"
        initVerify()
        kv.encode(Constant.VERIFY_CHANGE, false)
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        initPresentation()
        btnViewChange(binding.btnFixPay, Constant.QUOTA_SWITCH)
        btnViewChange(binding.btnOff, Constant.SWITCH)
        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        } else {
            netWorkStatus("0")
        }
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}\n" +
                "v${packageManager.getPackageInfo(packageName, 0).versionName}"

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
//        productsVM.upDataDishes(true)
        mealId = TimeUtil.CurrentTimeSection()
        mXService?.hideNavBar = true
        if (simpleDisplay?.isShowing != true) simpleDisplay?.show()
        maps = mutableMapOf(
            "刷脸" to Constant.PAY_FACE_TYPE,
            "刷卡" to Constant.PAY_IC_TYPE,
            "扫码" to Constant.PAY_CODE_TYPE,
            "刷卡扫码" to Constant.PAY_CODE_IC_TYPE,
        )
        val type = when (kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)) {
            Constant.PAY_FACE_TYPE -> "刷脸"
            Constant.PAY_IC_TYPE -> "刷卡"
            Constant.PAY_CODE_TYPE -> "扫码"
            else -> "刷卡扫码"
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
            kv.encode(Constant.VERIFY_CHANGE, true)
//            faceVerification()
            val i = Intent(this, FaceVerificationActivity::class.java)
            startActivity(i)
        }
    }

    fun initVerify() {
        if (kv.decodeBool(Constant.CODE_VERIFICATION_SET)) {
            binding.verifyShow.visibility = View.VISIBLE
            viewModel.getDishesCountOfWindow()
            viewModel.getDishesCountForUI().observe(this) { value ->
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
                val linearManager = LinearLayoutManager(this)
                binding.rvDishOrder.layoutManager = linearManager
                binding.rvDishOrder.adapter = orderCountAdapter
            }
        } else binding.verifyShow.visibility = View.GONE
    }

    override fun onStop() {
        binding.btnConfirm.setBackgroundResource(R.drawable.click_button)
        binding.btnConfirm.setTextColor(Color.BLACK)
        binding.btnConfirm.text = "确认金额"
        simpleDisplay?.dismiss()
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
            kv.encode(Constant.QUOTA_SWITCH, !kv.decodeBool(Constant.QUOTA_SWITCH, false))
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUOTA_CHANGE, null))
        }

        binding.btnOff.setOnClickListener { //开启离线模式
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
            if ((System.currentTimeMillis() - lastTime) < 1000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY, null))
        }

        binding.btnFirst.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            maps[binding.btnFirst.text.trim()].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
        }
        binding.btnSecond.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            maps[binding.btnSecond.text.trim()].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
        }
        binding.btnThird.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            maps[binding.btnThird.text.trim()].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
        }

        binding.btnVerify.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            if (kv.decodeInt(Constant.VERIFY_MODE) == 0) {
//                faceVerification()
                val i = Intent(this, FaceVerificationActivity::class.java)
                startActivity(i)
            } else {
                simpleDisplay?.dismiss()
                val i = Intent(this, CardVerificationActivity::class.java)
                startActivity(i)
            }
        }

        binding.verifyView.setOnLongClickListener {
            initVerify()
            true
        }
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
                    binding.btnConfirm.text = "确定金额￥${event.any as String}"
                    simpleDisplay?.enableBtn(event.any as String)
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
                LogUtil.d(TAG, "EventBus : ${event.code} 接收开启刷脸核销事件")
                CommonAndDpToPxUtil.speakWork("请刷脸进行核销")
//                faceVerification()
                val i = Intent(this, FaceVerificationActivity::class.java)
                startActivity(i)
            }
            Constant.EVENT_VERIFY_CHANGE -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收订餐核销更新UI ${event.any}")
                val o = event.any as Boolean
                if (o) {
                    runOnUiThread { initVerify() }
                }
            }
            Constant.EVENT_SECOND -> handler.post {
                val type = kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)
                LogUtil.d(TAG, "支付方式：$type")
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, type))
            }
        }
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
        NetworkStateManager.getInstance().unRegisterObserver(this)
        EventBus.getDefault().unregister(this)
        unregisterReceiver(periodicVerificationReceiver)
    }
}