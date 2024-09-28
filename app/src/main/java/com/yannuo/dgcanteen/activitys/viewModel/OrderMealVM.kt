package com.yannuo.dgcanteen.activitys.viewModel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/21 15:08
 **/
class OrderMealVM : ViewModel(), ScanDevice.DataCallBack, OnReadDataListener {
    private val TAG = javaClass.simpleName
    private val kv = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val awaitStatus: MutableLiveData<String> = MutableLiveData<String>("")
    private val userName: MutableLiveData<String> = MutableLiveData<String>("")
    private val reorderStatus: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    private var mFacePayService: ZHSTFacePayService? = null
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()
    private var listener: OrderMealListener? = null

    private var mCardHandle: SerialPortHelper? = null
    private var mScanDevice: ScanDevice? = null
    private var ntHelp: NTScanHelp? = null

    private var orderStatus = OrderStatus.INVALID

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        val orderForUI = OrderForUI().apply {
            errCode = "ORDER0003"
            errMsg = "发生异常信息: ${throwable.message}"
        }
        listener?.onOrderResult(0, orderForUI)
    }

    enum class OrderStatus {
        INVALID,    //无效状态
        AWAIT       //等待状态
    }

    fun getAwaitStatus(): MutableLiveData<String> = awaitStatus
    fun getUserName(): MutableLiveData<String> = userName
    fun getReorderStatus(): MutableLiveData<Boolean> = reorderStatus

    fun setOrderStatus(status: OrderStatus) {
        orderStatus = status
    }

    fun bindService() {
        val serviceIntent = Intent()
        serviceIntent.action = "com.ccb.smartcanteen.FacePayService"
        serviceIntent.setPackage("com.ccb.smartcanteen")
        MyApplication.applicationContext.bindService(serviceIntent, MyServiceConnection(), Context.BIND_AUTO_CREATE)
    }

    fun setOrderListener(listener: OrderMealListener) {
        this.listener = listener
    }

    fun open(loginType: String) {
        when (loginType) {
            "1" -> {
                val offline = if (kv.decodeBool(Constant.SWITCH)) "1" else "0"
                mFacePayService?.startFacePay("", offline, OnPayResultListener())
            }
            "2" -> { //打开扫码头
                // 元捷
                mScanDevice = ScanDevice()
                mScanDevice?.setCallbackListener(this)
                mScanDevice?.openScan()
                // 牛图
                ntHelp = NTScanHelp()
                ntHelp?.OpenScanCode(this, MyApplication.applicationContext)
            }
            "3" -> { //打卡读卡器
                mCardHandle = SerialPortHelper()
                mCardHandle?.readDataListener = this
                mCardHandle?.openSerialPort("/dev/ttyS4")
//                mCardHandle?.openSerialPort("/dev/ttyXRUSB0")
            }
        }
    }

    fun close(loginType: String) {
        when (loginType) {
            "2" -> {
                // 元捷
                mScanDevice?.setCallbackListener(null)
                mScanDevice?.closeScan()
                mScanDevice = null
                // 牛图
                ntHelp?.CloseScanCode()
                ntHelp = null
            }
            "3" -> {
                mCardHandle?.readDataListener = null
                mCardHandle?.closeSerialPort()
                mCardHandle = null
            }
        }
    }

    private inner class MyServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, "Service Connected Success!")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.e(TAG, "Service Connected Failure!")
            mFacePayService = null
        }
    }

    private inner class OnPayResultListener : PayResultListener.Stub() {
        override fun onResult(result: String) {
            loginHandler("1", result)
        }
    }

    override fun onData(data: String) {
        loginHandler("2", data)
    }

    override fun numberOfIcCard(number: String?) {
        if (number == null) return
        val icCard = number.trim().uppercase()
        loginHandler("3", icCard)
    }

    private fun loginHandler(type: String, content: String) {
        if (orderStatus != OrderStatus.AWAIT) return
        orderStatus = OrderStatus.INVALID
        listener?.onOrderResult(-1, "验证用户信息")
        LogUtil.d(TAG, "type: $type content: $content")
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
            val orderForUI = OrderForUI().apply {
                campusId = payCfg.campusId
                businessId = payCfg.businessId
                businessName = payCfg.businessName
                vposId = payCfg.counterId
                corpId = payCfg.corp_id
                orderType = type
                orderContent = content
                offline = if (NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) "0" else "1"
            }
            if (orderForUI.campusId.isEmpty() || orderForUI.businessId.isEmpty() || orderForUI.vposId.isEmpty()) {
                orderForUI.errCode = "ORDER0001"
                orderForUI.errMsg = "未配置支付环境"
                listener?.onOrderResult(0, orderForUI)
                return@launch
            }
            if (orderForUI.offline == "1") {
                orderForUI.errCode = "ORDER0002"
                orderForUI.errMsg = "设备无网络请检查网络"
                listener?.onOrderResult(0, orderForUI)
                return@launch
            }
            when (orderForUI.orderType) {
                "1" -> {
                    val faceRes = Gson().fromJson(orderForUI.orderContent, CcbFacePayResultBean::class.java)
                    if (faceRes.RESULT == "Y") {
                        orderForUI.custId = faceRes.CUST_ID
                        orderForUI.custName = faceRes.CUST_NAME
                        getOrderToken(orderForUI)
                    } else {
                        orderForUI.errCode = faceRes.ERRCODE
                        orderForUI.errMsg = faceRes.ERRMSG
                        listener?.onOrderResult(0, orderForUI)
                    }
                }
                "2" -> {
                    if (!orderForUI.orderContent.contains("CT0001")) {
                        orderForUI.errCode = "ORDER0002"
                        orderForUI.errMsg = ""
                        return@launch
                    }
                    val analysisRes = runBlocking {
                        val qrCodeMap = CanteenEncryptionUtil.getAnalysisQr(payCfg.campusId, "PAY002", payCfg.corp_id, content)
                        val analysisResult = mRepository.getCcbData(qrCodeMap).body()?.string() ?: ""
                        Gson().fromJson(analysisResult.replace("\r\n", ""), ScanAnalysisBean::class.java)
                    }
                    if (analysisRes.RESULT == "Y") {
                        orderForUI.custId = analysisRes.CUST_ID
                        orderForUI.custName = analysisRes.CUST_NAME
                        getOrderToken(orderForUI)
                    } else {
                        orderForUI.errCode = analysisRes.ERRCODE
                        orderForUI.errMsg = analysisRes.ERRMSG
                        listener?.onOrderResult(0, orderForUI)
                    }
                }
                "3" -> {
                    val person = dbHelper.queryPersonToCardId(orderForUI.orderContent)
                    if (person != null) {
                        orderForUI.custId = person.custId
                        orderForUI.custName = person.personName
                        getOrderToken(orderForUI)
                    } else if (orderForUI.offline == "0") {
                        val result = mRepository.queryPersonByCardId(RequestPerson(orderForUI.campusId, orderForUI.orderContent))
                        if (result.code == "200") {
                            orderForUI.custId = result.data?.custId ?: ""
                            orderForUI.custName = result.data?.personName ?: ""
                            orderForUI.phone = result.data?.phone ?: ""
                            getOrderToken(orderForUI)
                        } else {
                            orderForUI.errCode = "ORDER0003"
                            orderForUI.errMsg = "未查询到用户信息"
                            listener?.onOrderResult(0, orderForUI)
                        }
                    } else {
                        orderForUI.errCode = "ORDER0003"
                        orderForUI.errMsg = "未查询到用户信息"
                        listener?.onOrderResult(0, orderForUI)
                    }
                }
            }
        }
    }

    private fun getOrderToken(orderForUI: OrderForUI) {
        runBlocking {
            val encryptStr = getCavEncryptParam(orderForUI)
            val tokenRes = mRepository.getToken(orderForUI.campusId, encryptStr)
            if (tokenRes.code == "200") {
                orderForUI.ccbToken = tokenRes.data?.dcccbToken ?: ""
                listener?.onOrderResult(1, orderForUI)
            } else {
                orderForUI.errCode = tokenRes.code
                orderForUI.errMsg = tokenRes.msg
                listener?.onOrderResult(0, orderForUI)
            }
        }
    }

    private fun getCavEncryptParam(orderForUI: OrderForUI): String {
        val encryptStr = StringBuilder()
        encryptStr.append("CUST_ID=${orderForUI.custId}")
            .append("&CUST_NAME=${orderForUI.custName.ifEmpty { "null" }}")
            .append("&CID_NO=null")
            .append("&CAMPUS_ID=${orderForUI.campusId}")
            .append("&PHONE_NO=null")
            .append("&TIMESTAMP=${System.currentTimeMillis()}")
        LogUtil.d(TAG, encryptStr.toString())
        return CanteenEncryptionUtil.encryption(encryptStr.toString())
    }

    fun placeAnOrder(orderForUI: OrderForUI, orderResult: (type: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            orderResult(1)
            //下单
            val orderBean = getOrderMealData(orderForUI)
            LogUtil.d(TAG, Gson().toJson(orderBean))
            val orderRes = mRepository.insertOrder(orderForUI.ccbToken, orderBean)
            if (orderRes.code == "200") {
                orderResult(2)
                orderForUI.orderId = orderRes.data?.orderId ?: ""
                //支付
                val encryption = getOrderPayData(orderForUI)
                val payRes = mRepository.payByIcCard(encryption)
                if (payRes.code == "200") {
                    val decryptStr = DES3CBCUtil.decryptRSA(payRes.data ?: "")
                    val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                    LogUtil.d(TAG, Gson().toJson(result))
                    orderForUI.result = result.RESULT
                    orderForUI.errCode = result.ERRCODE
                    orderForUI.errMsg = result.ERRMSG
                    orderForUI.actualPayment = result.ACTUAL_PAYMENT
                    orderForUI.accBal = result.REMAIN_BAL
                    saveOrderRecord(orderForUI, result)
                    orderResult(3)
                } else {
                    orderForUI.errCode = payRes.code
                    orderForUI.errMsg = payRes.msg
                    orderResult(3)
                }
            } else {
                orderForUI.errCode = orderRes.code
                orderForUI.errMsg = orderRes.msg
                orderResult(3)
            }
        }
    }

    private fun getOrderMealData(orderForUI: OrderForUI): InsertOrderBean {
        orderForUI.orderTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
        val orderBean = Gson().fromJson(Gson().toJson(orderForUI), InsertOrderBean::class.java)
        if (orderForUI.distribute == "2") {
            orderBean.deliveryTime = ""
            orderBean.pickupTime = orderForUI.orderTime
        }
        orderBean.apply {
            personName = orderForUI.custName
            discountPayment = "0.00"
            packagingFee = "0.00"
            deliveryFee = "0.00"
            orderType = orderForUI.distribute
            delFlag = "1"
        }
        orderForUI.dishList.forEach {
            val insertDish = InsertDish().apply {
                dishesId = it.dishId
                dishesName = it.dishName
                dishesNum = it.dishCount.toString()
                dishesPrice = it.dishPrice
                imgUrl = it.imgUrl
                delFlag = "1"
            }
            orderBean.dcOrderDishesList.add(insertDish)
        }
        return orderBean
    }

    private fun getOrderPayData(orderForUI: OrderForUI): String {
        val payBean = Gson().fromJson(Gson().toJson(orderForUI), CardPayBean::class.java)
        val deviceSerial = CommonAndDpToPxUtil.getDeviceSerial() ?: ""
        val currentTime = System.currentTimeMillis()
        orderForUI.payTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", currentTime)
        payBean.apply {
            cardId = orderForUI.orderContent
            deviceId = deviceSerial
            sessionId = "$deviceSerial$currentTime${Random().nextInt(10)}"
            signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
        }
        LogUtil.d(TAG, Gson().toJson(payBean))
        return DES3CBCUtil.encryption(Gson().toJson(payBean))
    }

    private fun saveOrderRecord(orderForUI: OrderForUI, payRes: ResponsePay) {
        val payOrder = Gson().fromJson(Gson().toJson(orderForUI), PayOrderTable::class.java)
        payOrder.apply {
            tranResult = "3" //1：待支付，2：支付失败，3：支付成功
            deviceId = CommonAndDpToPxUtil.getDeviceSerial() ?: ""
            username = orderForUI.custName
            accNo = payRes.ACC_NO
            accBal = payRes.ACC_BAL
            accType = payRes.ACC_TYPE
            traceId = payRes.TRACEID
            payType = orderForUI.orderType
            payContent = orderForUI.orderContent
            payDate = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
            flag = 1
        }
        dbHelper.insertPayOrder(payOrder)
        val order = dbHelper.queryPayOrder(payOrder.orderId)
        orderForUI.dishList.forEach {
            val payDish = PayDishTable().apply {
                dishesId = it.dishId
                dishesName = it.dishName
                dishesNumber = it.dishCount.toString()
                dishesPrice = it.dishPrice
                payOrderTable = order
            }
            dbHelper.insertPayDish(payDish)
        }
        payRes.ACC_LIST.forEach {
            val acc = Gson().fromJson(Gson().toJson(it), AccListTable::class.java)
            acc.payOrderTable = order
            dbHelper.insertAccList(acc)
        }
    }

    interface OrderMealListener {
        /**
         * @param type 0-错误 1-成功
         */
        fun onOrderResult(type: Int, any: Any)
    }
}