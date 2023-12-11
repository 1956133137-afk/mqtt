package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Handler
import android.view.Display
import android.widget.Button
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ActivityCalculateBinding
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:46
 **/
class CalculateActivity : BaseActivity<ActivityCalculateBinding>(),
    NetworkStateManager.NetWorkListener {

    private var mXService: MyService? = null
    private var navigation = true
    private var passwordDialog: PasswordDialog ?= null
    private var confirmDialog: ConfirmDialog ?= null
    private lateinit var kv: MMKV
    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private lateinit var simpleDisplay: SimpleDisplay
    private val handler = Handler()
    private lateinit var maps :MutableMap<String, Int >
    private var lastTime = 0L  //上次触发时间

    override fun bindLayout() {
        binding = ActivityCalculateBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initView()
        initEvent()
    }

    private fun initObject() {
        EventBus.getDefault().register(this)
        NetworkStateManager.getInstance().registerObserver(this)
        if (!this::kv.isInitialized) kv = MMKV.defaultMMKV()
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)

        maps = mutableMapOf( "刷脸" to Constant.PAY_FACE_TYPE ,
            "刷卡" to Constant.PAY_IC_TYPE ,
            "扫码"  to Constant.PAY_CODE_TYPE,
            "刷卡扫码" to Constant.PAY_CODE_IC_TYPE,
        )
        val type = when (kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)) {
            Constant.PAY_FACE_TYPE -> "刷脸"
            Constant.PAY_IC_TYPE -> "刷卡"
            Constant.PAY_CODE_TYPE -> "扫码"
            else -> "刷卡扫码"
        }
        maps.remove(type)
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        btnViewChange(binding.btnFixPay, Constant.QUOTA_SWITCH)
        btnViewChange(binding.btnOff, Constant.SWITCH)
        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}\n" +
                "v${packageManager.getPackageInfo(packageName, 0).versionName}"

    }

    override fun onResume() {
        initPresentation()
        LogUtil.i(TAG,"onResume!")
        super.onResume()
        mXService?.hideNavBar = true

        maps = mutableMapOf( "刷脸" to Constant.PAY_FACE_TYPE ,
            "刷卡" to Constant.PAY_IC_TYPE ,
            "扫码"  to Constant.PAY_CODE_TYPE,
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
            when(index){
                0->{
                    binding.btnFirst.text = it.key
                }
                1->{
                    binding.btnSecond.text = it.key
                }
                2->{
                    binding.btnThird.text = it.key
                }
            }
        }
    }

    private fun initPresentation() {
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        simpleDisplay = SimpleDisplay(this, secondDisplays)
        simpleDisplay.setActivity(this)
        simpleDisplay.show()
    }

    override fun onStop() {
        binding.btnConfirm.setBackgroundResource(R.drawable.click_button)
        binding.btnConfirm.setTextColor(Color.BLACK)
        binding.btnConfirm.text = "确认金额"
        simpleDisplay.cancel()
        LogUtil.i(TAG,"onstop!")
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
            if (confirmDialog == null) confirmDialog = ConfirmDialog(this)
            if (!kv.decodeBool(Constant.SWITCH, false)) {
                confirmDialog?.apply {
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
            passwordDialog?.apply {
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
            if ((System.currentTimeMillis() - lastTime) < 1000 )return@setOnClickListener
            lastTime = System.currentTimeMillis()
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY, null))
        }

        binding.btnFirst.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000 )return@setOnClickListener
            lastTime = System.currentTimeMillis()
            maps[binding.btnFirst.text.trim()].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
        }
        binding.btnSecond.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000 )return@setOnClickListener
            lastTime = System.currentTimeMillis()
            maps[binding.btnSecond.text.trim()].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
        }
        binding.btnThird.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000 )return@setOnClickListener
            lastTime = System.currentTimeMillis()
            maps[binding.btnThird.text.trim()].also {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, it))
            }
        }

//        binding.btnBalance.setOnClickListener {
//            val intent = Intent(this@CalculateActivity, BalanceActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//            startActivity(intent)
//            finish()
//        }
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
                if (simpleDisplay.isShowing) {
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
                    binding.btnConfirm.text="确定金额￥${event.any as String}"
                    simpleDisplay.enableBtn(event.any as String)
                }
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
                        kv.encode(Constant.SWITCH, !kv.decodeBool(Constant.SWITCH))
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
        passwordDialog?.cancel()
        confirmDialog?.cancel()
        NetworkStateManager.getInstance().unRegisterObserver(this)
        EventBus.getDefault().unregister(this)
    }

}