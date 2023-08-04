package com.yannuo.dgcanteen.activitys

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
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:46
 **/
class CalculateActivity : BaseActivity<ActivityCalculateBinding>(),
    NetworkStateManager.NetWorkListener {

    private var mXService: MyService? = null
    private var navigation = true
    private lateinit var passwordDialog: PasswordDialog
    private lateinit var kv: MMKV
    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private lateinit var simpleDisplay: SimpleDisplay
    private val handler = Handler()

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
    }

    private fun initView() {
        btnViewChange(binding.btnFixPay, Constant.QUOTA_SWITCH)
        btnViewChange(binding.btnOff, Constant.SWITCH)
        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }
    }

    override fun onResume() {
        initPresentation()
        super.onResume()
//        mXService?.hideNavBar = true
    }

    private fun initPresentation() {
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        simpleDisplay = SimpleDisplay(this, secondDisplays)
        simpleDisplay.show()
    }

    override fun onStop() {
        simpleDisplay.cancel()
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
            kv.encode(Constant.SWITCH, !kv.decodeBool(Constant.SWITCH, false))
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
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
        NetworkStateManager.getInstance().unRegisterObserver(this)
        EventBus.getDefault().unregister(this)
    }

}