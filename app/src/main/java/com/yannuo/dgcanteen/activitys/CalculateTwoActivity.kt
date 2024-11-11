package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityCalculateTwoBinding
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

class CalculateTwoActivity : BaseActivity<ActivityCalculateTwoBinding>(), NetworkStateManager.NetWorkListener {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private val mmkv = MMKV.defaultMMKV()
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog? = null
    private var navigation = true
    private val modeMap: LinkedHashMap<String, Int> = linkedMapOf(
        "刷脸" to Constant.PAY_FACE_TYPE, "刷卡" to Constant.PAY_IC_TYPE, "扫码" to Constant.PAY_CODE_TYPE, "刷卡扫码" to Constant.PAY_CODE_IC_TYPE
    )
    private var lastTime = 0L  //上次触发时间

    override fun bindLayout() {
        binding = ActivityCalculateTwoBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initData()
        initEvent()
    }

    private fun initObject() {
        mXService = MyService(this)
        EventBus.getDefault().register(this)
        NetworkStateManager.getInstance().registerObserver(this)
        FaceScanVM.instance.bindService()
    }

    private fun initData() {
        if (NetworkStateManager.getInstance().isOnline(this).not()) binding.network.setImageResource(R.drawable.ic_wifi_no) else netWorkStatus("0")
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}\nv${packageManager.getPackageInfo(packageName, 0).versionName}"
        changeModeUI()
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }
        binding.btnSetting.setOnClickListener {
            if (passwordDialog == null) passwordDialog = PasswordDialog(this)
            passwordDialog?.show()
            passwordDialog?.binding?.tvBack?.text = "输入密码"
            passwordDialog?.setListener(object : CloseEvent {
                override fun onEvent(code: Int, msg: String?) {
                    startActivity(Intent(this@CalculateTwoActivity, SettingActivity::class.java))
                }
            })
        }
        binding.btnSelectMode.setOnClickListener {
            mmkv.encode(Constant.CODE_VERIFICATION_SET, !mmkv.decodeBool(Constant.CODE_VERIFICATION_SET))
            changeModeUI()
        }
        binding.btnFirst.setOnClickListener {
            if (judgeRepeatClick()) return@setOnClickListener
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TRAN_MODE, modeMap[binding.btnFirst.text.trim().substring(0, 2)]))
        }
        binding.btnSecond.setOnClickListener {
            if (judgeRepeatClick()) return@setOnClickListener
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TRAN_MODE, modeMap[binding.btnSecond.text.trim().substring(0, 2)]))
        }
        binding.btnThird.setOnClickListener {
            if (judgeRepeatClick()) return@setOnClickListener
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TRAN_MODE, modeMap[binding.btnThird.text.trim().substring(0, 2)]))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventCalculate(event: MessageEvent) {
        handler.post {
            when (event.code) {
                Constant.EVENT_TENTH -> {
                    LogUtil.d(TAG, "EventBus : ${event.code} 接收mqtt状态变更事件~")
                    val connect = event.any as Boolean
                    binding.server.setImageResource(if (connect) R.drawable.ic_server else R.drawable.ic_server_no)
                }
                Constant.EVENT_TRAN_MODE -> {
                    val verifyBool = mmkv.decodeBool(Constant.CODE_VERIFICATION_SET)
                    LogUtil.d(TAG, "verify:$verifyBool value:${event.any}")
                    if (verifyBool) {

                    } else EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, event.any))
                }
            }
        }
    }

    private fun judgeRepeatClick(): Boolean {
        if (System.currentTimeMillis() - lastTime < 1000L) return true
        lastTime = System.currentTimeMillis()
        return false
    }

    @SuppressLint("SetTextI18n")
    private fun changeModeUI() {
        val verifyBool = mmkv.decodeBool(Constant.CODE_VERIFICATION_SET)
        binding.btnSelectMode.text = if (verifyBool) "核销模式" else "收款模式"
        val modeStr = if (verifyBool) "核销" else "支付"
        modeMap.entries.forEachIndexed { index, map ->
            when (index) {
                0 -> binding.btnFirst.text = "${map.key}$modeStr"
                1 -> binding.btnSecond.text = "${map.key}$modeStr"
                2 -> binding.btnThird.text = "${map.key}$modeStr"
            }
        }
    }

    override fun netWorkStatus(statue: String?) {
        handler.post {
            when (statue) {
                "0" -> {
                    binding.network.setImageResource(R.drawable.ic_wifi)
                    if (mmkv.decodeBool(Constant.SWITCH, false)) {
                        mmkv.encode(Constant.SWITCH, false)
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
        super.onDestroy()
        NetworkStateManager.getInstance().unRegisterObserver(this)
        EventBus.getDefault().unregister(this)
        passwordDialog?.cancel()
    }
}