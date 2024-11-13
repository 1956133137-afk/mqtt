package com.yannuo.dgcanteen.activitys.viewModel

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.NTScanHelp
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.greendao.entity.VerifyDishes
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.util.CanteenEncryptionUtil.getAnalysisCode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class VerificationVM : ViewModel(), ScanDevice.DataCallBack, OnReadDataListener {
    var showToastEvent: MutableLiveData<String>
    var loadingEvent: MutableLiveData<Boolean>
    private val TAG = javaClass.simpleName
    private val mRespository by lazy {
        PayRepositoryOfPay()
    }
    private var mPayCfg: PayCfg? = null
    private var exceptionHandler: CoroutineExceptionHandler
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    private lateinit var mCodeDevice: ScanDevice
    private lateinit var mCardDevice: SerialPortHelper
    private lateinit var ntHelp: NTScanHelp
    private var codeStatus = CodeStatus.INVALID
    private var cardStatus = CardStatus.INVALID
    private var mealName = ""

    private val _verifyCount = MutableLiveData<VerificationCountResponse>()
    val verifyCount: MutableLiveData<VerificationCountResponse>
        get() = _verifyCount

    private val _dishesCount = MutableLiveData<DishesCountResponse>()
    val dishesCount: MutableLiveData<DishesCountResponse>
        get() = _dishesCount

    private val counts = MutableLiveData<CountDishesOfWindowResponse>()
    val dishesCountOfWindow: MutableLiveData<CountDishesOfWindowResponse>
        get() = counts

    enum class CodeStatus {
        INVALID, PAY
    }

    enum class CardStatus {
        INVALID, PAY
    }

    private var callBackListener: CallbackListener? = null

    init {
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
        val sn = Utils.getSN()

        if (mPayCfg == null || TextUtils.isEmpty(mPayCfg!!.campusId) ||
            TextUtils.isEmpty(mPayCfg!!.businessId) || TextUtils.isEmpty(mPayCfg!!.counterId)
        ) {
            LogUtil.e(TAG, "未配置支付环境")
        }
        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()
        mCodeDevice = ScanDevice()
        mCardDevice = SerialPortHelper()
        ntHelp = NTScanHelp()
        exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG, "协程异常： $throwable ${throwable.stackTraceToString()}")
            showToastEvent.postValue("错误： ${throwable.message}")
            loadingEvent.postValue(false)
        }
    }

    //打开扫码,刷卡
    fun openQrCode() {
        codeStatus = CodeStatus.PAY
        cardStatus = CardStatus.PAY
        mCodeDevice.openScan()
        mCardDevice.openSerialPort("/dev/ttyS4", 9600)
        mCodeDevice.setCallbackListener(this)
        mCardDevice.readDataListener = this
        ntHelp.OpenScanCode(this, MyApplication.applicationContext)
    }

    //修改支付状态
    fun setPayStatus() {
        codeStatus = CodeStatus.PAY
        cardStatus = CardStatus.PAY
    }

    //关闭扫码,刷卡
    fun closeQrCode() {
        codeStatus = CodeStatus.INVALID
        cardStatus = CardStatus.INVALID
        if (this::mCodeDevice.isInitialized) {
            mCodeDevice.setCallbackListener(null)
            mCodeDevice.closeScan()
        }
        if (this::mCardDevice.isInitialized) {
            mCardDevice.readDataListener = null
            mCardDevice.closeSerialPort()
        }
        if (this::ntHelp.isInitialized) {
            ntHelp.CloseScanCode()
        }
    }

    fun getPayCfg(): PayCfg? {
        return mPayCfg
    }

    /**
     * 订餐核销
     */
    fun verification(campusId: String?, businessId: String?, custId: String?, orderId: String?, deviceId: String?, cardId: String?, flag: Int) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            val data =
                CanteenEncryptionUtil.encryption("CAMPUS_ID=${campusId}&BUSINESS_ID=${businessId}&CUST_ID=${custId}&ORDER_ID=${orderId}&DEVICE_ID=${deviceId}&CARD_ID=${cardId}")
            LogUtil.d(TAG, "加密数据: $data")
            var verification = VerificationRequest().apply {
                this.dcEncryptParam = data
                this.flag = flag
            }
            LogUtil.d(TAG, Gson().toJson(verification))
            val ccbCodeVerification = mRespository.getCcbCodeVerification(verification)
            LogUtil.d(TAG, Gson().toJson(ccbCodeVerification))
            if (ccbCodeVerification.code == "200") {
                val toJson = Gson().toJson(ccbCodeVerification.data)
                val json = Gson().fromJson(toJson, VerificationResponse::class.java)
                val allMeals = DishesDBHelper.getInstance().queryAllMeals()
                allMeals.forEach {
                    if (Date() >= it.startTime && Date() <= it.endTime) {
                        mealName = it.mealName
                    }
                }
                val verificationUI = VerificationUI().apply {
                    errorMsg = ccbCodeVerification.msg
                    personName = json.personName
                    dish = json.verifyDishes
                    dishesList = json.verify[mealName]?.dishesList
                    window = json.unVerifyWindowName
                    windows = json.verify[mealName]?.windowList
                    unDish = json.unVerifyDishes
                    time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                }
                if (flag == 0) {
                    val verifyDishesBean = VerifyDishes().apply {
                        this.personName = json.personName
                        this.dish = json.verifyDishes.toString()
                        this.window = json.unVerifyWindowName.toString()
                        this.unDish = json.unVerifyDishes.toString()
                        this.time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                    }
                    DishesDBHelper.getInstance().insertVerifyDishes(verifyDishesBean)
                }
                callBackListener?.onOtherListener(0, verificationUI)
            } else {
                LogUtil.w(TAG, "${ccbCodeVerification.msg}")
                val verificationUI = VerificationUI().apply {
                    errorMsg = ccbCodeVerification.msg.toString()
                    time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                }
                callBackListener?.onOtherListener(10, verificationUI)
            }
        }
    }

    //设置回调监听
    fun setListener(listener: CallbackListener) {
        this.callBackListener = listener
    }

    fun getVerifyCount() {
        viewModelScope.launch {
            val campusId = if (mPayCfg == null) "" else mPayCfg!!.campusId
            val businessId = if (mPayCfg == null) "" else mPayCfg!!.businessId
            val mealId = TimeUtil.CurrentTimeSection()
            val deviceId = Utils.getSN()
            val request = VerificationCountRequest(campusId.toString(), businessId.toString())
            val res = mRespository.getCcbCountDCofDay(request)
            if (res.code == "200") {
                val json = Gson().fromJson(Gson().toJson(res.data), VerificationCountResponse::class.java)
                _verifyCount.value = json
            }
        }
    }

    fun getDishesCount() {
        viewModelScope.launch {
            val campusId = if (mPayCfg == null) "" else mPayCfg!!.campusId
            val businessId = if (mPayCfg == null) "" else mPayCfg!!.businessId
            val request = VerificationCountRequest(campusId.toString(), businessId.toString())
            val res = mRespository.getDishesCount(request)
            if (res.code == "200") {
                val json = Gson().fromJson(Gson().toJson(res.data), DishesCountResponse::class.java)
                _dishesCount.value = json
            }
        }
    }

    fun getDishesCountOfWindow() {
        viewModelScope.launch {
            val campusId = if (mPayCfg == null) "" else mPayCfg!!.campusId
            val businessId = if (mPayCfg == null) "" else mPayCfg!!.businessId
            val mealId = TimeUtil.CurrentTimeSection()
            val deviceId = Utils.getSN()
            val request = CountDishesOfWindowBean().apply {
                this.campusId = campusId
                this.businessId = businessId
                this.mealId = mealId
                this.deviceId = deviceId
            }
            val res = mRespository.getCountDishes(request)
            if (res.code == "200") {
                val json = Gson().fromJson(Gson().toJson(res.data), CountDishesOfWindowResponse::class.java)
                counts.value = json
            }
        }
    }

    override fun onData(data: String) {
        viewModelScope.launch {
            if (!kv.decodeBool(Constant.QUERY_VERIFY)) {
                if (codeStatus == CodeStatus.INVALID) return@launch
                codeStatus = CodeStatus.INVALID
            }
            val qrData = data.trim().replace("\r\n", "")
            val info = getAnalysisCode(qrData)
            Log.d(TAG, "onData: $info")
            verification(
                info["CAMPUS_ID"],
                info["BUSINESS_ID"],
                info["CUST_ID"],
                info["ORDER_ID"],
                info["DEVICE_ID"],
                info["CARD_ID"],
                if (kv.decodeBool(Constant.QUERY_VERIFY,false)) 1 else 0
            )
        }
    }

    override fun numberOfIcCard(number: String?) {
        viewModelScope.launch {
            if (!kv.decodeBool(Constant.QUERY_VERIFY)) {
                if (cardStatus == CardStatus.INVALID) return@launch
                cardStatus = CardStatus.INVALID
            }
            val cardData = number?.trim()?.replace("\r\n", "")
            LogUtil.d(TAG, "卡号：$cardData")
            val campusId = if (mPayCfg == null) "" else mPayCfg!!.campusId
            val businessId = if (mPayCfg == null) "" else mPayCfg!!.businessId
            val sn = Utils.getSN()
            verification(
                campusId,
                businessId,
                null,
                null,
                sn,
                cardData,
                if (kv.decodeBool(Constant.QUERY_VERIFY, false)) 1 else 0
            )
        }
    }
}