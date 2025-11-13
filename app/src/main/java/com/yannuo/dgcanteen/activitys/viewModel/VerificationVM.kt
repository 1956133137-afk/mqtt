package com.yannuo.dgcanteen.activitys.viewModel

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
//    private var mealName = ""

    private val mutex = Mutex()
    private var isStatus = false

    private val verifyCount: MutableLiveData<VerificationCountResponse> = MutableLiveData<VerificationCountResponse>()
    private val dishesCount: MutableLiveData<DishesCountResponse> = MutableLiveData<DishesCountResponse>()
    private val dishesCountOfWindow: MutableLiveData<CountDishesOfWindowResponse> = MutableLiveData<CountDishesOfWindowResponse>()

    fun getVerifyCountForUI(): MutableLiveData<VerificationCountResponse> = verifyCount
    fun getDishesCountForUI(): MutableLiveData<CountDishesOfWindowResponse> = dishesCountOfWindow

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

    fun getPayCfg(): PayCfg = mPayCfg ?: PayCfg()

    /**
     * 订餐核销
     */
    fun verification(campusId: String?, businessId: String?, custId: String?, orderId: String?, deviceId: String?, cardId: String?, flag: Int) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            callBackListener?.onOtherListener(-1, "")
            val data = CanteenEncryptionUtil.encryption("CAMPUS_ID=${campusId}&BUSINESS_ID=${businessId}&CUST_ID=${custId}&ORDER_ID=${orderId}&DEVICE_ID=${deviceId}&CARD_ID=${cardId}")
            LogUtil.d(TAG, "加密数据: $data")
            var verification = VerificationRequest().apply {
                this.campusId = campusId.toString()
                this.dcEncryptParam = data
                this.flag = flag
            }
            LogUtil.d(TAG, Gson().toJson(verification))
            val ccbCodeVerification = mRespository.getCcbCodeVerification(verification)
            LogUtil.d(TAG, Gson().toJson(ccbCodeVerification))
            if (ccbCodeVerification.code == "200") {
//                val allMeals = DishesDBHelper.getInstance().queryAllMeals()
//                allMeals.forEach {
//                    if (Date() >= it.startTime && Date() <= it.endTime) {
//                        mealName = it.mealName
//                        LogUtil.d(TAG, "当前餐别: $mealName")
//                    }
//                }
//                LogUtil.d(TAG, Gson().toJson(allMeals))
                if (flag == 0) verifyPay(ccbCodeVerification) else verifyQuery(ccbCodeVerification)
            } else {
                val verificationUI = VerificationUI().apply {
                    errorMsg = ccbCodeVerification.msg.toString()
                    time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                }
                callBackListener?.onOtherListener(10, verificationUI)
            }
        }
    }

    private fun verifyPay(ccbCodeVerification: CanteenResponse<JsonObject>) {
        val json = Gson().fromJson(Gson().toJson(ccbCodeVerification.data), VerificationResponse::class.java)
        val verificationUI = VerificationUI().apply {
            errorMsg = ccbCodeVerification.msg
            personName = json.personName
//            dish = json.verifyDishes
//            dishesList = json.verify[mealName]?.dishesList
            window = json.unVerifyWindowName
//            windows = json.verify[mealName]?.windowList
            unDish = json.unVerifyDishes
            time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
        }
        // 整理核销菜品格式
        val dishList: MutableList<String> = mutableListOf()
        json.verifyDishes.forEach { dishList.add(spliceMsg(it)) }
        verificationUI.dish = dishList.toTypedArray()

        val verifyDishesBean = VerifyDishes()
        verifyDishesBean.personName = verificationUI.personName
        verifyDishesBean.time = verificationUI.time
        verifyDishesBean.apply {
            this.dish = TextUtils.join("|@|", dishList.toTypedArray() ?: arrayOf())
            this.window = TextUtils.join("|@|", json.unVerifyWindowName ?: arrayOf())
            this.unDish = TextUtils.join("|@|", json.unVerifyDishes ?: arrayOf())
        }
        DishesDBHelper.getInstance().insertVerifyDishes(verifyDishesBean)
        callBackListener?.onOtherListener(0, verificationUI)
    }

    private fun verifyQuery(ccbCodeVerification: CanteenResponse<JsonObject>) {
        val queryReceive = Gson().fromJson(ccbCodeVerification.data, CavQueryReceive::class.java)

        val verificationUI = VerificationUI()
        verificationUI.errorMsg = ccbCodeVerification.msg
        verificationUI.personName = queryReceive.personName
        verificationUI.time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())

        if (queryReceive.verify != null && queryReceive.verify.size > 0) {
            val windowList = arrayListOf<String>()
            queryReceive.verify.forEach { (key, value) ->
                val verify = Verify()
                verify.mealName = key
                val verifyReceive = Gson().fromJson<MutableList<VerifyReceive>>(value, object : TypeToken<MutableList<VerifyReceive>>() {}.type)
                verifyReceive.forEach { receive ->
                    windowList.clear()
                    receive.window.split("，").forEach { if (it.isNotEmpty() && !windowList.contains(it)) windowList.add(it) }
                    verify.dishesList.add("${receive.dishes}  →  ${Gson().toJson(windowList).replace("(\\[|\\]|\")".toRegex(), "")}")
                }
                verificationUI.verify.add(verify)
            }
            callBackListener?.onOtherListener(0, verificationUI)
        } else {
            verificationUI.errorMsg = "当前餐别您未订餐"
            callBackListener?.onOtherListener(10, verificationUI)
        }
//        val dishList = arrayListOf<String>()
//        val windowList = arrayListOf<String>()
//        val verifyReceive = Gson().fromJson<MutableList<VerifyReceive>>(queryReceive.verify[mealName], object : TypeToken<MutableList<VerifyReceive>>() {}.type)
//        if (verifyReceive != null) {
//            verifyReceive.forEach { receive ->
//                dishList.add(receive.dishes)
//                receive.window.split("，").forEach { if (it.isNotEmpty() && !windowList.contains(it)) windowList.add(it) }
//            }
//            verificationUI.dishesList = dishList.toTypedArray()
//            verificationUI.windows = windowList.toTypedArray()
//            callBackListener?.onOtherListener(0, verificationUI)
//        } else {
//            verificationUI.errorMsg = "当前餐别您未订餐"
//            callBackListener?.onOtherListener(10, verificationUI)
//        }
    }

    private fun spliceMsg(dish: String): String {
        val dishStr = StringBuilder()
        if (dish.contains('，')) {
            val dishMsg = dish.substring(0, dish.lastIndexOf("，"))
            val dishUnit = dish.substring(dish.lastIndexOf("，") + 1).replace("|", "")
            if (dishMsg.contains('|')) {
                val dishName = dishMsg.substring(0, dishMsg.lastIndexOf("|"))
                val dishPrice = dishMsg.substring(dishMsg.lastIndexOf("|") + 1)
                dishStr.append("${dishName}(${dishPrice})")
            } else dishStr.append(dishMsg)
            dishStr.append("， $dishUnit")
        } else return dish
        return dishStr.toString()
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
                verifyCount.value = json
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
                dishesCount.value = json
            }
        }
    }

    fun getDishesCountOfWindow() {
        if (isStatus) return
        viewModelScope.launch {
            mutex.withLock { isStatus = true }
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
            LogUtil.d(TAG, "订餐统计请求：${Gson().toJson(request)}")
            val res = mRespository.getCountDishes(request)
            if (res.code == "200") {
                val json = Gson().fromJson(Gson().toJson(res.data), CountDishesOfWindowResponse::class.java)
                LogUtil.d(TAG, "订餐统计结果: ${Gson().toJson(json)}")
                dishesCountOfWindow.value = json
            }
            mutex.withLock { isStatus = false }
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
                if (kv.decodeBool(Constant.QUERY_VERIFY, false)) 1 else 0
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