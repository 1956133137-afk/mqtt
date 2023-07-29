package com.yannuo.dgcanteen.activitys

import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProceedsVM
import com.yannuo.dgcanteen.databinding.ActivityCalculateBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:46
 **/
class CalculateActivity : BaseActivity<ActivityCalculateBinding>() {

    private var mXService: MyService? = null
    private var navigation = true
    private lateinit var passwordDialog: PasswordDialog
    private lateinit var kv: MMKV
    private val handler = Handler()
//    private lateinit var viewModel: ProceedsVM

    override fun bindLayout() {
        binding = ActivityCalculateBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initView()
        initEvent()
    }

    private fun initObject() {
//        viewModel = ViewModelProvider(
//            this,
//            ViewModelProvider.AndroidViewModelFactory(application)
//        )[ProceedsVM::class.java]
        EventBus.getDefault().register(this)
        if (!this::kv.isInitialized) kv = MMKV.defaultMMKV()
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)
    }

    private fun initView() {
        btnViewChange(binding.btnFixPay, Constant.QUOTA_SWITCH)
        btnViewChange(binding.btnOff, Constant.SWITCH)
    }

    override fun onResume() {
        super.onResume()
//        mXService?.hideNavBar = true
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }

        binding.btnFixPay.setOnClickListener { //固定金额
            binding.btnFixPay.apply {
                if (kv.decodeBool(Constant.QUOTA_SWITCH, false)) {
                    text = "定额收款关闭"
                    setBackgroundResource(R.drawable.click_button_white)
                    setTextColor(Color.parseColor("#4F4F4F"))
                    kv.encode(Constant.QUOTA_SWITCH, false)
                } else {
                    text = "定额收款开启"
                    setBackgroundResource(R.drawable.click_button_blue)
                    setTextColor(Color.parseColor("#FFFFFF"))
                    kv.encode(Constant.QUOTA_SWITCH, true)
                }
            }
        }

        binding.btnOff.setOnClickListener { //开启离线模式
            binding.btnOff.apply {
                if (kv.decodeBool(Constant.SWITCH, false)) {
                    text = "离线模式关闭"
                    setBackgroundResource(R.drawable.click_button_white)
                    setTextColor(Color.parseColor("#4F4F4F"))
                    kv.encode(Constant.SWITCH, false)
                } else {
                    text = "离线模式开启"
                    setBackgroundResource(R.drawable.click_button_blue)
                    setTextColor(Color.parseColor("#FFFFFF"))
                    kv.encode(Constant.SWITCH, true)
                }
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

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        passwordDialog.cancel()
        EventBus.getDefault().unregister(this)
    }

}