package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.CountDownTimer
import android.os.Handler
import android.view.Display
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderVerifyVM
import com.yannuo.dgcanteen.adapters.DishVerifyCountAdapter
import com.yannuo.dgcanteen.adapters.InfoAdapter
import com.yannuo.dgcanteen.adapters.OrderVerify2Adapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityOrderVerifyBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.KeyboardDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.OrderVerify
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

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
    private var countDown: CountDownTimer? = null

    private val dishVerifyCountAdapter by lazy { DishVerifyCountAdapter() }
    private var dishVerifyTime: Long = 0L

    private var keyboardDialog: KeyboardDialog? = null
    private var handler: Handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout() {
        binding = ActivityOrderVerifyBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }


    override fun onStart() {
        super.onStart()
        orderVerifyVM.closeIcCard()
        //根据开关决定是否默认开启读卡
        if(mmkv.decodeBool(Constant.ORDER_VERIFY_IC_CARD,false)){
            orderVerifyVM.openIcCard()
        }
        orderVerifyDisplay?.show()
    }

    private fun initObject() {
        mXService = MyService(this)
        NetworkStateManager.getInstance().registerObserver(this)

        val decodeBool = mmkv.decodeBool(Constant.ORDER_VERIFY_CONFIRM, false)
        binding.btnVerifyConfirm.text = if (decodeBool) "员工确认" else "无需确认"
        binding.btnFinishVerify.visibility = if (decodeBool) View.VISIBLE else View.GONE
        binding.verifyDishView.layoutManager = GridLayoutManager(this, 2)
        binding.verifyDishView.adapter = orderVerify2Adapter

        orderVerifyVM.mealTime.observe(this) { binding.mealTime.text = it }
        orderVerifyVM.mOrderVerify.observe(this) {
            if (it.verifyType == 0) {
                countDown?.cancel()
                binding.verifyNameTime.visibility = View.VISIBLE
                binding.verifyNameTime.text = if (it.verifyDishList.size < 1) {
                    onCountDownTimer()
                    orderVerifyVM.setIsVerifyStatus(true)
                    "${orderVerifyVM.getVerifyName()} ${TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())}\n${it.verifyMsg}"
                } else "${orderVerifyVM.getVerifyName()} ${TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())}"
            } else {
                binding.verifyNameTime.visibility = View.GONE
                orderVerifyVM.setIsVerifyStatus(true)
            }
            orderVerify2Adapter.data = it.verifyDishList
        }
        /*提示支付结果*/
        orderVerifyVM.mOrderPay.observe(this) {
            keyboardDialog?.updateTV(it, true)
        }
        binding.dishStatsCount.layoutManager = LinearLayoutManager(this)
        binding.dishStatsCount.adapter = dishVerifyCountAdapter
        orderVerifyVM.dishVerifyCount.observe(this) { dishVerifyCountAdapter.data = it }
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
        binding.btnUpdateDishCount.setOnClickListener {
            val clickIntervalTime = System.currentTimeMillis() - dishVerifyTime
            if (clickIntervalTime > 300000) {
                dishVerifyTime = System.currentTimeMillis()
                orderVerifyVM.getOrderStatsCount()
                ToastShowUtil.show("正在刷新数据")
            } else ToastShowUtil.show("请 ${(300000 - clickIntervalTime) / 1000}s 后再刷新数据")
        }

        binding.btnProceedsMode.setOnClickListener {
            orderVerifyVM.payMode = true
            orderVerifyDisplay?.changeView(0)
            if (keyboardDialog == null) keyboardDialog = KeyboardDialog(this)
            keyboardDialog?.show()
            keyboardDialog?.setListener(object : KeyboardDialog.OnKeyboardCallback {
                override fun onKeyboard(payAmount: String, status: Boolean) {
                    handler.post {
                        orderVerifyVM.isPayStatus = status
                        if (!status && payAmount == "-1") orderVerifyVM.payMode = false
                        if (status) {
                            if(BigDecimal(payAmount).compareTo(BigDecimal(0.00)) < 1){
                                ToastShowUtil.show("无效金额")
                                keyboardDialog?.clearStr(false)
                                return@post
                            }
                            CommonAndDpToPxUtil.speakWork("请支付${payAmount}元")
                            orderVerifyVM.payAmount = payAmount
                            keyboardDialog?.updateTV("等待支付", false)
                        } else {
                            orderVerifyVM.payAmount = ""
                            orderVerifyDisplay?.reset()
                        }
                        orderVerifyDisplay?.changeView(0)
                    }
                }
            })
        }
        binding.btnToggleOrder.setOnClickListener {
            /*切换点餐模式*/
            mmkv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
            val restartIntent = MyApplication.applicationContext.packageManager.getLaunchIntentForPackage(MyApplication.applicationContext.packageName)
            restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(restartIntent)
            exitProcess(0)
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
        binding.modeTips.text = if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) "订餐查询模式" else "订餐核销模式"
        binding.btnToggleOrder.visibility = if (mmkv.decodeBool(Constant.QUICK_SWITCH_MODE, false)) View.VISIBLE else View.GONE
        orderVerifyDisplay?.changeView(0)
        orderVerifyVM.getOrderStatsCount()
    }

    private fun onCountDownTimer() {
        countDown?.cancel()
        val backTime = mmkv.decodeInt(Constant.MEAL_TIME, 10).toLong()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(backTime), 1000) {
            override fun onTick(mil: Long) {
//                binding.btnBack.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
                binding.verifyNameTime.visibility = View.GONE
            }
        }
        countDown?.start()
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
        passwordDialog?.cancel()
        awaitingDialog?.cancel()
        NetworkStateManager.getInstance().unRegisterObserver(this)
    }
}