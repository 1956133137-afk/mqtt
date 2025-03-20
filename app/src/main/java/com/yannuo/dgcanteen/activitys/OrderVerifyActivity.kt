package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.lifecycle.ViewModelProvider
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderVerifyVM
import com.yannuo.dgcanteen.databinding.ActivityOrderVerifyBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil

class OrderVerifyActivity : BaseActivity<ActivityOrderVerifyBinding>(), NetworkStateManager.NetWorkListener {
    private val orderVerifyVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[OrderVerifyVM::class.java] }
    private val mmkv = MMKV.defaultMMKV()
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog? = null
    private var awaitingDialog: AwaitingDialog? = null
    private var navigation = true

    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private var orderVerifyDisplay: OrderVerifyDisplay? = null

    override fun bindLayout() {
        binding = ActivityOrderVerifyBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    private fun initObject() {
        mXService = MyService(this)
        NetworkStateManager.getInstance().registerObserver(this)

        orderVerifyVM.openIcCard()

        initPresentation()
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            return@setOnLongClickListener true
        }
        binding.btnSetting.setOnClickListener {
            if (passwordDialog == null) passwordDialog = PasswordDialog(this)
            passwordDialog?.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@OrderVerifyActivity, SettingActivity::class.java))
                    }
                })
            }
        }
    }

    private fun initPresentation() {
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        if (orderVerifyDisplay == null) {
            orderVerifyDisplay = OrderVerifyDisplay(this, secondDisplays)
            orderVerifyDisplay?.show()
        }
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
        orderVerifyDisplay?.changeView(0)
    }

    override fun netWorkStatus(statue: String?) {
        runOnUiThread {
            when (statue) {
                "0" -> {
                    mmkv.encode(Constant.SWITCH, false)
                    binding.imServer.setImageResource(R.drawable.ic_server)
                }
                else -> {
                    mmkv.encode(Constant.SWITCH, true)
                    binding.imServer.setImageResource(R.drawable.ic_server_no)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orderVerifyDisplay?.safeCancel()
        orderVerifyVM.closeIcCard()
        passwordDialog?.cancel()
        awaitingDialog?.cancel()
        NetworkStateManager.getInstance().unRegisterObserver(this)
    }
}