package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.adapters.PayUserAdapter
import com.yannuo.dgcanteen.adapters.VerifyUserAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityCalculateTwoBinding
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.VerifyDishes
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.LinkedHashMap

class CalculateTwoActivity : BaseActivity<ActivityCalculateTwoBinding>(), NetworkStateManager.NetWorkListener {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private val mRespository by lazy { PayRepositoryOfPay() }
    private val verifyUserAdapter by lazy { VerifyUserAdapter() }
    private val payUserAdapter by lazy { PayUserAdapter() }
    private val confirmDialog by lazy { ConfirmDialog(this) }
    private val mmkv = MMKV.defaultMMKV()
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog? = null
    private var navigation = true
    private val modeMap: LinkedHashMap<String, Int> = linkedMapOf(
        "刷脸" to Constant.PAY_FACE_TYPE, "刷卡" to Constant.PAY_IC_TYPE, "扫码" to Constant.PAY_CODE_TYPE, "刷卡扫码" to Constant.PAY_CODE_IC_TYPE
    )
    private var lastTime = 0L  //上次触发时间
    private var currentDate: String = ""
    private var mealName = ""

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
//        buttonIsUsable(true)
        val verificationUI = VerificationUI().apply {
            errorMsg = "${throwable.message}"
            time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
        }
        val i = Intent(applicationContext, FaceVerificationActivity::class.java)
        i.putExtra("id", 10)
        i.putExtra("verify", Gson().toJson(verificationUI))
        startActivity(i)
    }

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

        currentDate = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        binding.verifyOrderView.layoutManager = LinearLayoutManager(this)
        binding.verifyOrderView.adapter = verifyUserAdapter
        verifyUserAdapter.data = DishesDBHelper.getInstance().queryVerifyUserToTen(currentDate)

        binding.payOrderView.layoutManager = LinearLayoutManager(this)
        binding.payOrderView.adapter = payUserAdapter
        payUserAdapter.data = DishesDBHelper.getInstance().queryPayOrderUserToTen(currentDate)
    }

    override fun onResume() {
        super.onResume()
        updateRecord()
    }

    private fun updateRecord() {
        currentDate = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        if (mmkv.decodeBool(Constant.CODE_VERIFICATION_SET)) {
            val verifyUser = DishesDBHelper.getInstance().queryVerifyUser(currentDate)
            if (verifyUser == null) verifyUserAdapter.clear()
            else if (!verifyUserAdapter.data.contains(verifyUser)) {
                verifyUserAdapter.insertDataTop(verifyUser, 10)
                binding.verifyOrderView.smoothScrollToPosition(0)
            }
        }
        val payOrderUser = DishesDBHelper.getInstance().queryPayOrderUser(currentDate)
        if (payOrderUser == null) payUserAdapter.clear()
        else if (!payUserAdapter.data.contains(payOrderUser)) {
            payUserAdapter.insertDataTop(payOrderUser, 10)
            binding.payOrderView.smoothScrollToPosition(0)
        }
    }

    private fun initData() {
        if (NetworkStateManager.getInstance().isOnline(this).not()) binding.network.setImageResource(R.drawable.ic_wifi_no) else netWorkStatus("0")
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}\nv${packageManager.getPackageInfo(packageName, 0).versionName}"
        btnViewChange()
        changeModeUI()
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }
        binding.btnOff.setOnClickListener {
            if (!mmkv.decodeBool(Constant.SWITCH, false)) {
                confirmDialog.show()
                confirmDialog.binding.tvText.text = "您确定开启离线模式吗"
                confirmDialog.setListener(object : ConfirmDialog.OnConfirmCallback {
                    override fun confirmCallback(flag: Boolean) {
                        if (flag) {
                            mmkv.encode(Constant.SWITCH, true)
                            EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
                        }
                    }
                })
            } else {
                mmkv.encode(Constant.SWITCH, false)
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
            }
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

    private fun btnViewChange() {
        binding.btnOff.apply {
            if (mmkv.decodeBool(Constant.SWITCH, false)) {
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

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventCalculate(event: MessageEvent) {
        handler.post {
            when (event.code) {
                Constant.EVENT_TENTH -> {
                    LogUtil.d(TAG, "EventBus : ${event.code} 接收mqtt状态变更事件~")
                    val connect = event.any as Boolean
                    binding.server.setImageResource(if (connect) R.drawable.ic_server else R.drawable.ic_server_no)
                }
                Constant.EVENT_OFF_CHANGE, Constant.EVENT_OFLINE_CHANGE -> btnViewChange()
                Constant.EVENT_TRAN_MODE -> {
                    val verifyBool = mmkv.decodeBool(Constant.CODE_VERIFICATION_SET)
                    LogUtil.d(TAG, "verify:$verifyBool value:${event.any}")
                    if (verifyBool) {
                        val value = event.any as Int
                        if (value == 0) {
//                            buttonIsUsable(false)
//                            FaceScanVM.instance.bindService()
//                            FaceScanVM.instance.startFacePay(true)
//                            FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
//                                override fun onFacePay(payForUI: PayForUI) {
//
//                                }
//
//                                override fun onFaceQuery(bean: CcbFacePayResultBean) {
//                                    verification(bean)
//                                }
//                            })
                            startActivity(Intent(this, FaceVerificationActivity::class.java))
                        } else {
                            CommonAndDpToPxUtil.speakWork("请出示核销码或者刷卡")
                            startActivity(Intent(this, CardVerificationActivity::class.java))
                        }
                    } else EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, event.any))
                }
                Constant.EVENT_NETWORK_EXCEPTION -> {
                    val status: Int = event.any as Int
                    binding.ivNetExc.visibility = if (status == 0) View.GONE else View.VISIBLE
                    if (status == 1 && !confirmDialog.isShowing) {
                        confirmDialog.show()
                        confirmDialog.setBackTime("网络异常，订单将转离线！", 10L)
                    }
                }
                Constant.EVENT_APP_ONLINE_STATUS -> handler.post {
                    val status = (mmkv.decodeInt(Constant.APP_ONLINE_STATUS, 0) == 1)
                    binding.ivNetExc.visibility = if (!status) View.GONE else View.VISIBLE
                }
            }
        }
    }

//    private fun buttonIsUsable(boolean: Boolean) {
//        handler.post {
//            binding.btnFirst.isEnabled = boolean
//            binding.btnSecond.isEnabled = boolean
//            binding.btnFirst.isEnabled = boolean
//        }
//    }

//    private fun verification(bean: CcbFacePayResultBean) {
//        lifecycleScope.launch(Dispatchers.IO + mHandler) {
//            var flag = 10
//            val verificationUI = VerificationUI().apply {
//                errorMsg = bean.ERRMSG
//                personName = bean.CUST_NAME
//                time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
//            }
//            if (bean.RESULT == "Y") {
//                val mPayCfg = mmkv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
//                val verifyOrderBean = VerifyOrderBean().apply {
//                    CAMPUS_ID = mPayCfg.campusId
//                    BUSINESS_ID = mPayCfg.businessId
//                    CUST_ID = bean.CUST_ID
//                    DEVICE_ID = CommonAndDpToPxUtil.getDeviceSerial()
//                }
//                val verifyRequest = VerificationRequest().apply {
//                    dcEncryptParam = getCavEncryptParam(verifyOrderBean)
//                    this.flag = 0
//                }
//                val verification = mRespository.getCcbCodeVerification(verifyRequest)
//                if (verification.code == "200") {
//                    val verificationResponse = Gson().fromJson(Gson().toJson(verification.data), VerificationResponse::class.java)
//                    LogUtil.d(TAG, Gson().toJson(verificationResponse))
//                    val allMeals = DishesDBHelper.getInstance().queryAllMeals()
//                    allMeals.forEach {
//                        if (Date() >= it.startTime && Date() <= it.endTime) mealName = it.mealName
//                    }
//                    verificationUI.apply {
//                        errorMsg = verification.msg
//                        personName = verificationResponse.personName
//                        dish = verificationResponse.verifyDishes
//                        dishesList = verificationResponse.verify[mealName]?.dishesList
//                        window = verificationResponse.unVerifyWindowName
//                        windows = verificationResponse.verify[mealName]?.windowList
//                        unDish = verificationResponse.unVerifyDishes
//                        time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
//                    }
//                    val verifyDishesBean = VerifyDishes().apply {
//                        this.personName = verificationResponse.personName
//                        this.dish = verificationResponse.verifyDishes.toString()
//                        this.window = verificationResponse.unVerifyWindowName.toString()
//                        this.unDish = verificationResponse.unVerifyDishes.toString()
//                        this.time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
//                    }
//                    DishesDBHelper.getInstance().insertVerifyDishes(verifyDishesBean)
//                    flag = 0
//                } else verificationUI.errorMsg = verification.msg
//            }
//            buttonIsUsable(true)
//            val i = Intent(applicationContext, FaceVerificationActivity::class.java)
//            i.putExtra("id", flag)
//            i.putExtra("verify", Gson().toJson(verificationUI))
//            startActivity(i)
//        }
//    }

//    private fun getCavEncryptParam(bean: VerifyOrderBean): String {
//        val encryptStr = StringBuilder()
//        encryptStr.append("CAMPUS_ID=${bean.CAMPUS_ID}")
//            .append("&BUSINESS_ID=${bean.BUSINESS_ID}")
//            .append("&CUST_ID=${bean.CUST_ID}")
//            .append("&ORDER_ID=${bean.ORDER_ID}")
//            .append("&DEVICE_ID=${bean.DEVICE_ID}")
//            .append("&CARD_ID=${bean.CARD_ID}")
//        LogUtil.d(TAG, encryptStr.toString())
//        return CanteenEncryptionUtil.encryption(encryptStr.toString())
//    }

    private fun judgeRepeatClick(): Boolean {
        if (System.currentTimeMillis() - lastTime < 1000L) return true
        lastTime = System.currentTimeMillis()
        return false
    }

    @SuppressLint("SetTextI18n")
    private fun changeModeUI() {
        val verifyBool = mmkv.decodeBool(Constant.CODE_VERIFICATION_SET)
        var modeStr = ""
        if (verifyBool) {
            binding.payRecord.visibility = View.INVISIBLE
            binding.verifyRecord.visibility = View.VISIBLE
            binding.btnSelectMode.text = "核销模式"
            modeStr = "核销"
        } else {
            binding.payRecord.visibility = View.VISIBLE
            binding.verifyRecord.visibility = View.INVISIBLE
            binding.btnSelectMode.text = "收款模式"
            modeStr = "支付"
        }
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
                    if (mmkv.decodeBool(Constant.SWITCH, false) && mmkv.decodeInt(Constant.APP_ONLINE_STATUS) == 0) {
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
        confirmDialog.cancel()
    }
}