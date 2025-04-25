package com.yannuo.dgcanteen.activitys.viewModel

import android.text.format.DateFormat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.NTScanHelp
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.AccListTable
import com.yannuo.dgcanteen.greendao.entity.PayDishTable
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashMap

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/18 10:53
 **/
class OrderVerifyVM : ViewModel(), OnReadDataListener, ScanDevice.DataCallBack {
    private val TAG = javaClass.simpleName
    private val mmkv = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val mRepository by lazy { PayRepositoryOfPay() }
    private val mutex: Mutex = Mutex()
    private var requestStatus = false
    private var statsStatus = false
    val dishVerifyCount: MutableLiveData<MutableList<InfoBean>> = MutableLiveData<MutableList<InfoBean>>(mutableListOf())

    private val mCardHandle = SerialPortHelper()
    private val mScanHandle = ScanDevice()
    private val mNTHandle = NTScanHelp()
    private var listener: VerifyCallBack? = null

    private var isVerifyStatus = true
    var payMode = false
    var isPayStatus = false
    var payAmount = ""
    val mOrderPay: MutableLiveData<String> = MutableLiveData<String>()

    private var mMealId = 0
    private var mealId = -1
    val mealTime: MutableLiveData<String> = MutableLiveData<String>("未开餐")

    private var verifyName: String = ""
    val mOrderVerify: MutableLiveData<OrderVerify> = MutableLiveData<OrderVerify>()

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
    }

    init {
        checkTime()
    }

    fun getpayState(): Boolean {
        return payMode && !isPayStatus
    }

    fun getVerifyName(): String = verifyName

    fun setIsVerifyStatus(status: Boolean) {
        isVerifyStatus = status
    }

    fun setVerifyListener(listener: VerifyCallBack) {
        this.listener = listener
    }

    fun openIcCard() {
        mCardHandle.readDataListener = this
        mCardHandle.openSerialPort("/dev/ttyS4")
    }

    fun closeIcCard() {
        mCardHandle.readDataListener = null
        mCardHandle.closeSerialPort()
    }

    fun openScan(){
        mNTHandle.OpenScanCode(this,MyApplication.applicationContext)
        mScanHandle.setCallbackListener(this)
        mScanHandle.openScan()
    }

    fun closeScan(){
        mNTHandle.CloseScanCode()
        mScanHandle.closeScan()
    }

    //通过人脸查询人员信息
    fun faceVerification() {
        LogUtil.d(TAG, "查询人脸信息~")
        FaceScanVM.instance.bindService()
        FaceScanVM.instance.startFacePay(true)
        FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
            override fun onFacePay(payForUI: PayForUI) {

            }

            override fun onFaceQuery(bean: CcbFacePayResultBean) {
                if (!payMode) {
                    if (!mmkv.decodeBool(Constant.ORDER_QUERY, false) && !isVerifyStatus) {
                        viewModelScope.launch(Dispatchers.Main) { ToastShowUtil.show("无效操作") }
                        return
                    }
                    orderVerify(bean.CUST_ID)
                }else orderPayment(bean.CUST_ID,"1")
            }
        })
    }

    //刷卡回调
    override fun numberOfIcCard(number: String?) {
        if (number == null) return
        val icCard = number.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim().uppercase()
        if (!payMode) {
            if (!mmkv.decodeBool(Constant.ORDER_QUERY, false) && !isVerifyStatus) {
                viewModelScope.launch(Dispatchers.Main) { ToastShowUtil.show("无效操作") }
                return
            }
            orderVerify(icCard)
        } else orderPayment(icCard,"3")
    }

    //扫码回调
    override fun onData(data: String) {
        val icCard = data.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim()
        if(icCard.isEmpty()) return
        if (!payMode) {
            if (!mmkv.decodeBool(Constant.ORDER_QUERY, false) && !isVerifyStatus) {
                viewModelScope.launch(Dispatchers.Main) { ToastShowUtil.show("无效操作") }
                return
            }
            orderVerify(icCard)
        } else orderPayment(icCard,"2")
    }

    /**
     * 订餐核销
     */
    private fun orderVerify(cardId: String) {
        /*加锁防止触发多次请求*/
        if (requestStatus) return
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            mutex.withLock { requestStatus = true }
            listener?.onVerifyResult(-1, OrderVerifyBean())
            /*查询人员信息*/
            val persons = dbHelper.queryPersonToCardId(cardId)
            /*获取请求参数*/
            val request = VerificationRequest().apply {
                dcEncryptParam = getCavEncryptParam(if (persons != null) persons.personName else "", cardId)
                flag = if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) 1 else 0
            }
            LogUtil.d(TAG, Gson().toJson(request))
            val response = mRepository.orderVerify(request)
            LogUtil.d(TAG, Gson().toJson(response))
            if (response.code == "200") {
                val orderVerifyBean = Gson().fromJson(Gson().toJson(response.data ?: ""), OrderVerifyBean::class.java)
                verifyName = orderVerifyBean.personName
                if (!mmkv.decodeBool(Constant.ORDER_QUERY, false)) isVerifyStatus = false
                listener?.onVerifyResult(0, orderVerifyBean)
            } else {
                isVerifyStatus = true
                verifyName = ""
                val orderVerify = OrderVerify().apply {
                    verifyType = 0
                    verifyMsg = response.msg
                    verifyDishList = mutableListOf()
                }
                mOrderVerify.postValue(orderVerify)
                listener?.onVerifyResult(1, OrderVerifyBean(), response.msg)
            }
            mutex.withLock { requestStatus = false }
        }
    }

    /**
     * 收款模式
     */
    private fun orderPayment(icCard: String, type: String) {
        if (!isPayStatus) {
            viewModelScope.launch(Dispatchers.Main) { ToastShowUtil.show("无效操作") }
            return
        }
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            mutex.withLock { isPayStatus = false }
            listener?.onPayResult(-1, PayForUI())
            /*初始化数据*/
            val payForUI = initData(type, icCard, payAmount)
            /*请求支付扣费*/
            val requestCard = Gson().fromJson(Gson().toJson(payForUI), CardPayBean::class.java)
            val requestCode = Gson().fromJson(Gson().toJson(payForUI), CodePayBean::class.java)
            when(type){
                "1" -> {
                    requestCard.custId = payForUI.payContent
                }
                "2" -> {
                    requestCode.qrCode = payForUI.payContent
                }
                else -> {
                    requestCard.cardId = payForUI.payContent
                }
            }
            LogUtil.i(TAG, "支付请求: ${Gson().toJson(if(type == "2") requestCode else requestCard)}")
            val encryption = DES3CBCUtil.encryption(Gson().toJson(if(type == "2") requestCode else requestCard))
            val response = when(type){
                "1" -> {mRepository.payByFace(encryption)}
                "2" -> {mRepository.payByQrCode(encryption)}
                else -> {mRepository.payByIcCard(encryption)}
            }
            if (response.code == "200") {
                var totalBalance = BigDecimal(0.00)
                val decryptStr = DES3CBCUtil.decryptRSA(response.data ?: "")
                val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                LogUtil.d(TAG, "支付结果: ${Gson().toJson(result)}")
                payForUI.result = result.RESULT
                payForUI.accType = result.ACC_TYPE
                payForUI.accNo = result.ACC_NO
                payForUI.accBal = result.REMAIN_BAL.ifEmpty { result.ACC_BAL }
                result.ACC_LIST.forEach {
                    val acclist = ACCLIST().apply {
                        ACC_NO = it.ACC_NO
                        ACC_BAL = it.ACC_BAL
                        ACC_TYPE = it.ACC_TYPE
                        TRAN_ID = it.TRAN_ID
                        PAYMENT = it.PAYMENT
                    }
                    payForUI.accList.add(acclist)
                    totalBalance = totalBalance.add(BigDecimal(it.PAYMENT))
                }
                /*查询人员姓名*/
                if (result.CUST_ID.isNotEmpty() && payForUI.username.isEmpty()) {
                    val persons = dbHelper.queryPersonToCustId(result.CUST_ID)
                    if (persons != null) payForUI.username = persons.personName
                }
                payForUI.actualPayment = if(totalBalance.compareTo(BigDecimal(0.00)) == 1) totalBalance.toEngineeringString() else payForUI.actualPayment
                payForUI.orderId = result.ORDERID
                payForUI.traceId = result.TRACEID
                payForUI.errCode = result.ERRCODE
                payForUI.errMsg = result.ERRMSG
            } else {
                payForUI.errCode = response.code
                payForUI.errMsg = response.msg
            }
            if (payForUI.result == "Y") {
                saveOrderRecord(payForUI, 1)
                mOrderPay.postValue("${payForUI.username.ifEmpty { "" }}支付${payForUI.actualPayment}元")
            } else mOrderPay.postValue("${payForUI.username.ifEmpty { "" }}支付失败")
            LogUtil.d(TAG, Gson().toJson(payForUI))
            payAmount = ""
            listener?.onPayResult(0, payForUI)
        }
    }

    /**
     * 初始化消费数据
     */
    private fun initData(type: String, content: String, payAmount: String): PayForUI {
        val mPayCfg = mmkv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
        val currentTime = System.currentTimeMillis()
        val deviceSerial = Utils.getSN()
        val payForUI = PayForUI().apply {
            businessId = mPayCfg.businessId
            businessName = mPayCfg.businessName
            campusId = mPayCfg.campusId
            corpId = mPayCfg.corp_id
            vposId = mPayCfg.counterId
            deviceId = deviceSerial
            payType = type //刷卡支付
            payContent = content
            payment = payAmount
            actualPayment = payAmount
            payTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", currentTime)
            payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
            sessionId = "$deviceSerial$currentTime${Random().nextInt(10)}"
            signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
            offline = if (NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) "0" else "1"
        }
        return payForUI
    }

    private fun saveOrderRecord(payForUI: PayForUI, flag: Int) {
        val payOrder = Gson().fromJson(Gson().toJson(payForUI), PayOrderTable::class.java)
        payOrder.tranResult = "3" //1：待支付，2：支付失败，3：支付成功
        payOrder.flag = flag
        dbHelper.insertPayOrder(payOrder)
        val order = dbHelper.queryPayOrder(payOrder.orderId)
        payForUI.paymentDishes.forEach {
            val dish = Gson().fromJson(Gson().toJson(it), PayDishTable::class.java)
            dish.payOrderTable = order
            dbHelper.insertPayDish(dish)
        }
        payForUI.accList.forEach {
            val acc = Gson().fromJson(Gson().toJson(it), AccListTable::class.java)
            acc.payOrderTable = order
            dbHelper.insertAccList(acc)
        }
    }

    fun getOrderStatsCount() {
        if (statsStatus) return
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            mutex.withLock { statsStatus = true }
            val infoBeanList: MutableList<InfoBean> = mutableListOf()
            val mPayCfg = mmkv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
            val request = CountDishesOfWindowBean().apply {
                campusId = mPayCfg.campusId
                businessId = mPayCfg.businessId
                deviceId = Utils.getSN()
            }
            request.mealId = this@OrderVerifyVM.mealId
            LogUtil.d(TAG, "订餐核销统计：${Gson().toJson(request)}")
            val res = mRepository.getCountDishes(request)
            if (res.code == "200") {
                val dishesOfWindow = Gson().fromJson(Gson().toJson(res.data), CountDishesOfWindowResponse::class.java)
                LogUtil.d(TAG, "订餐核销统计: ${Gson().toJson(dishesOfWindow)}")
                infoBeanList.add(InfoBean("核销人数：", "${dishesOfWindow.dcVerifyPersonNum}/${dishesOfWindow.dcTotalPersonNum}"))
                dishesOfWindow.needVerifyTotal.forEach {
                    infoBeanList.add(InfoBean("${it.dishesName}：", "${getVerifyDishCount(it.dishesName, dishesOfWindow.verifyTotal)}/${it.dishesNum}"))
                }
                LogUtil.d(TAG, "订餐核销统计: ${Gson().toJson(infoBeanList)}")
                dishVerifyCount.postValue(infoBeanList)
            }
            mutex.withLock { statsStatus = false }
        }
    }

    private fun getVerifyDishCount(dishName: String, verifyDishList: List<DishesCounts>): Int {
        var count = 0
        verifyDishList.forEach { if (it.dishesName == dishName) count = it.dishesNum }
        return count
    }

    private fun checkTime() {
        viewModelScope.launch(Dispatchers.Default + mHandler) {
            while (isActive) {
                mMealId = TimeUtil.CurrentTimeSection()
                if (mMealId != mealId) {
                    mealId = mMealId
                    val timeStr = when (mealId) {
                        0 -> "未开餐"
                        else -> {
                            val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
                            "${meal.mealName} ${DateFormat.format("HH:mm", meal.startTime)}~${DateFormat.format("HH:mm", meal.endTime)}"
                        }
                    }
                    if (timeStr != mealTime.value) mealTime.postValue(timeStr)
                }
                delay(5000)
            }
        }
    }

    fun getOrderQuery(verifyMap: HashMap<String, JsonArray>): MutableList<Verify> {
        val verifyList: MutableList<Verify> = mutableListOf()
        val windowList = arrayListOf<String>()
        verifyMap.forEach { (key, value) ->
            val verify = Verify()
            verify.mealName = key
            val verifyReceive = Gson().fromJson<MutableList<VerifyReceive>>(value, object : TypeToken<MutableList<VerifyReceive>>() {}.type)
            verifyReceive.forEach { receive ->
                windowList.clear()
                receive.window.split("，").forEach { if (it.isNotEmpty() && !windowList.contains(it)) windowList.add(it) }
                verify.dishesList.add("${receive.dishes}  →  ${Gson().toJson(windowList).replace("(\\[|\\]|\")".toRegex(), "")}")
            }
            verifyList.add(verify)
        }
        return verifyList
    }

    fun getOrderVerify(bean: OrderVerifyBean): MutableList<OrderVerify> {
        val orderVerifyList: MutableList<OrderVerify> = mutableListOf()
        /*核销成功*/
        if (bean.verifySuccessDishes.size > 0) {
            val verifySuccess = OrderVerify().apply {
                verifyType = 0
                verifyMsg = ""
                verifyDishList = bean.verifySuccessDishes
            }
            mOrderVerify.postValue(verifySuccess)
            orderVerifyList.add(verifySuccess)
        } else {
            val orderVerify = OrderVerify().apply {
                verifyType = 0
                verifyMsg = "核销失败原因：${bean.verifyFail.ErrorMessage}"
                verifyDishList = mutableListOf()
            }
            mOrderVerify.postValue(orderVerify)
        }
        /*核销失败*/
        if (bean.verifyFail.verifyFailDishes.size > 0) {
            val verifyFailure = OrderVerify().apply {
                verifyType = 1
                verifyMsg = bean.verifyFail.ErrorMessage
                verifyDishList = bean.verifyFail.verifyFailDishes
            }
            orderVerifyList.add(verifyFailure)
        }
        /*待核销*/
        if (bean.unVerifyDishes.size > 0) {
            val unVerify = OrderVerify().apply {
                verifyType = 2
                verifyMsg = "${Gson().toJson(bean.unVerifyWindowName).replace("(\\[|\\]|\")".toRegex(), "")}"
                verifyDishList = bean.unVerifyDishes
            }
            orderVerifyList.add(unVerify)
        }
        return orderVerifyList
    }

    private fun getCavEncryptParam(custId: String, cardId: String): String {
        /*获取设备商户信息*/
        val mPayCfg = mmkv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
        val encryptStr = StringBuilder()
        /*拼接加密参数*/
        encryptStr.append("CAMPUS_ID=${mPayCfg.campusId}")
            .append("&BUSINESS_ID=${mPayCfg.businessId}")
            .append("&CUST_ID=${custId}")
            .append("&ORDER_ID=")
            .append("&DEVICE_ID=${Utils.getSN()}")
            .append("&CARD_ID=${cardId}")
        LogUtil.d(TAG, encryptStr.toString())
        /*参数加密返回*/
        return CanteenEncryptionUtil.encryption(encryptStr.toString())
    }

    interface VerifyCallBack {
        fun onVerifyResult(type: Int, orderVerifyBean: OrderVerifyBean, errMsg: String = "")

        fun onPayResult(type: Int, payForUI: PayForUI)
    }
}