package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderVerifyVM
import com.yannuo.dgcanteen.adapters.OrderVerify2Adapter
import com.yannuo.dgcanteen.databinding.ActivityOrderVerifyBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.OrderVerify
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.TimeUtil
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

    private val orderVerify2Adapter by lazy { OrderVerify2Adapter() }

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

        val decodeBool = mmkv.decodeBool(Constant.ORDER_VERIFY_CONFIRM, false)
        binding.btnVerifyConfirm.text = if (decodeBool) "员工确认" else "无需确认"
        binding.btnFinishVerify.visibility = if (decodeBool) View.VISIBLE else View.GONE
        binding.verifyDishView.layoutManager = GridLayoutManager(this, 2)
        binding.verifyDishView.adapter = orderVerify2Adapter

        orderVerifyVM.openIcCard()
        orderVerifyVM.mealTime.observe(this) { binding.mealTime.text = it }
        orderVerifyVM.mOrderVerify.observe(this) {
            if (it.verifyType == 0) binding.verifyNameTime.visibility = View.VISIBLE
            else {
                binding.verifyNameTime.visibility = View.GONE
                orderVerifyVM.setIsVerifyStatus(true)
            }
            binding.verifyNameTime.text = "${orderVerifyVM.getVerifyName()} ${TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())}"
            orderVerify2Adapter.data = it.verifyDishList
        }

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
        binding.btnVerifyConfirm.setOnClickListener {
            val boolean = !mmkv.decodeBool(Constant.ORDER_VERIFY_CONFIRM, false)
            binding.btnVerifyConfirm.text = if (boolean) "员工确认" else "无需确认"
            binding.btnFinishVerify.visibility = if (boolean) View.VISIBLE else View.GONE
            mmkv.encode(Constant.ORDER_VERIFY_CONFIRM, boolean)
        }
        binding.btnFinishVerify.setOnClickListener { orderVerifyVM.mOrderVerify.postValue(OrderVerify()) }
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