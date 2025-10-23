package com.yannuo.dgcanteen.activitys.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
    private var currentCustId: String = ""
    private var currentCcbToken: String = ""
    private var loginOrPayStatus: Boolean = true
    private var orderForUI = OrderForUI()

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
    fun getUserId(): String = currentCustId
    fun getCcbToken(): String = currentCcbToken
    fun getReorderStatus(): MutableLiveData<Boolean> = reorderStatus

    fun setOrderStatus(status: OrderStatus) {
        orderStatus = status
    }

    fun setOrderForUI(order: OrderForUI) {
        orderForUI = order
    }

    fun setOrderListener(listener: OrderMealListener) {
        this.listener = listener
    }

    fun open(loginType: String, status: Boolean) {
        loginOrPayStatus = status
        when (loginType) {
            "1" -> {
                FaceScanVM.instance.bindService()
                FaceScanVM.instance.startFacePay(true)
                FaceScanVM.instance.setFaceListener(faceResultListener)
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

    private val faceResultListener = object : FaceScanVM.FaceResultListener {
        override fun onFacePay(payForUI: PayForUI) {
            payHandler("1", Gson().toJson(payForUI))
        }

        override fun onFaceQuery(bean: CcbFacePayResultBean) {
            loginHandler("1", Gson().toJson(bean))
        }
    }

    override fun onData(data: String) {
        val icCard = data.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim()
        if (loginOrPayStatus) loginHandler("2", icCard) else payHandler("2", icCard)
    }

    override fun numberOfIcCard(number: String?) {
        if (number == null) return
        val icCard = number.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim().uppercase()
        if (loginOrPayStatus) loginHandler("3", icCard) else payHandler("3", icCard)
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
                corpId = payCfg.corpId
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
                    when {
                        orderForUI.orderContent.contains("CT0001") -> {
                            val analysisRes = runBlocking {
                                val qrCodeMap = CanteenEncryptionUtil.getAnalysisQr(payCfg.campusId, "PAY002", payCfg.corpId, content)
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
                        orderForUI.orderContent.contains("CCB") -> {
                            val plainText = DES3CBCUtil.transDecryption(orderForUI.orderContent)
                            val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
                            val minutes = TimeUtil.timestamp(pastDueTime)
                            if (minutes > 1) {
                                LogUtil.d(TAG, plainText)
                                val cidNo = plainText.substring(0, plainText.indexOf("@"))
                                val person = dbHelper.queryPersonToCidNo(cidNo)
                                if (person != null) {
                                    orderForUI.custId = person.custId
                                    orderForUI.custName = person.personName
                                    getOrderToken(orderForUI)
                                } else {
                                    orderForUI.errCode = "ORDER0004"
                                    orderForUI.errMsg = "未查询到人员信息"
                                    listener?.onOrderResult(0, orderForUI)
                                }
                            } else {
                                orderForUI.errCode = "ORDER0005"
                                orderForUI.errMsg = "二维码已过期"
                                listener?.onOrderResult(0, orderForUI)
                            }
                        }
                        else -> {
                            orderForUI.errCode = "ORDER0006"
                            orderForUI.errMsg = "暂不支持该类型二维码"
                            listener?.onOrderResult(0, orderForUI)
                        }
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
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val encryptStr = getCavEncryptParam(orderForUI)
            LogUtil.d(TAG, encryptStr)
            val tokenRes = mRepository.getToken(orderForUI.campusId, orderForUI.corpId, encryptStr)
            LogUtil.d(TAG, Gson().toJson(tokenRes))
            if (tokenRes.code == "200") {
                orderForUI.ccbToken = tokenRes.data?.dcccbToken ?: ""
                currentCustId = orderForUI.custId
                currentCcbToken = orderForUI.ccbToken
                /*订餐查询*/
                if (kv.decodeBool(Constant.ORDER_QUERY, false)) {
                    listener?.onOrderResult(1, orderForUI)
                    return@launch
                }
                /*获取商家配置*/
                val businessConfig = mRepository.getBusinessConfig(currentCcbToken, orderForUI.campusId, orderForUI.businessId)
                LogUtil.d(TAG, Gson().toJson(businessConfig))
                if (businessConfig.code == "200") {
                    val busCigBean = Gson().fromJson(Gson().toJson(businessConfig.data), BusCigBean::class.java)
                    if (busCigBean.isOrder == "1") listener?.onOrderResult(1, orderForUI)
                    else {
                        orderForUI.errCode = "ORDER004"
                        orderForUI.errMsg = "当前商家不支持订餐"
                        listener?.onOrderResult(0, orderForUI)
                    }
                } else {
                    orderForUI.errCode = businessConfig.code
                    orderForUI.errMsg = businessConfig.msg
                    listener?.onOrderResult(0, orderForUI)
                }
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

    fun placeAnOrder(order: OrderForUI, verifyStatus: Boolean) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            orderForUI = order
            orderForUI.deviceId = CommonAndDpToPxUtil.getDeviceSerial()
            listener?.onOrderResult(2, "订餐下单中")
            //下单
//            val orderBean = getOrderMealData(orderForUI)
            val orderBean = getBatchOrder(orderForUI)
            LogUtil.d(TAG, Gson().toJson(orderBean))
//            val orderRes = mRepository.insertOrder(orderForUI.ccbToken, orderBean)
            val orderRes = mRepository.insertBatchOrder(orderForUI.ccbToken, orderBean)
            if (orderRes.code == "200") {
//                orderForUI.orderId = orderRes.data?.orderId ?: ""
                orderForUI.orderId = orderRes.data?.pOderId ?: ""
                orderForUI.verifyFlag = if (verifyStatus) "1" else "2"
                when (orderForUI.orderType) {
                    "1" -> {
                        listener?.onOrderResult(5, "")
                        FaceScanVM.instance.bindService()
                        FaceScanVM.instance.setOrderDishList(orderForUI.dishList)
                        FaceScanVM.instance.startFacePay(false, orderBean.actualTotalPayment, orderForUI.orderId, orderForUI.verifyFlag)
                        FaceScanVM.instance.setFaceListener(faceResultListener)
                    }
                    "2" -> {
                        listener?.onOrderResult(3, "")
                        open("2", false)
                    }
                    else -> payHandler(orderForUI.orderType, orderForUI.orderContent)
                }
            } else {
                orderForUI.errCode = orderRes.code
                orderForUI.errMsg = orderRes.msg
                listener?.onOrderResult(4, orderForUI)
            }
        }
    }

    private fun payHandler(type: String, content: String) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            listener?.onOrderResult(2, "订餐支付中")
            LogUtil.d(TAG, Gson().toJson(orderForUI))
            when (type) {
                "1" -> {
                    val payForUI = Gson().fromJson(content, PayForUI::class.java)
                    orderForUI.result = payForUI.result
                    orderForUI.payTime = payForUI.payTime
                    orderForUI.errCode = payForUI.errCode
                    orderForUI.errMsg = payForUI.errMsg
                    orderForUI.actualPayment = payForUI.actualPayment
                    orderForUI.accBal = payForUI.accBal
                    listener?.onOrderResult(4, orderForUI)
                }
                "2" -> {
                    close("2")
                    orderForUI.orderContent = content
                    val payRes = mRepository.payByQrCode(getOrderPayData())
                    if (payRes.code == "200") {
                        val decryptStr = DES3CBCUtil.decryptRSA(payRes.data ?: "")
                        val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                        LogUtil.d(TAG, Gson().toJson(result))
                        orderForUI.result = result.RESULT
                        orderForUI.errCode = result.ERRCODE
                        orderForUI.errMsg = result.ERRMSG
                        orderForUI.actualPayment = result.ACTUAL_PAYMENT
                        orderForUI.accBal = result.REMAIN_BAL.ifEmpty { result.ACC_BAL }
                        saveOrderRecord(orderForUI, result)
                        listener?.onOrderResult(4, orderForUI)
                    } else {
                        orderForUI.errCode = payRes.code
                        orderForUI.errMsg = payRes.msg
                        listener?.onOrderResult(4, orderForUI)
                    }
                }
                else -> {
                    val payRes = mRepository.payByIcCard(getOrderPayData())
                    if (payRes.code == "200") {
                        val decryptStr = DES3CBCUtil.decryptRSA(payRes.data ?: "")
                        val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                        LogUtil.d(TAG, Gson().toJson(result))
                        orderForUI.result = result.RESULT
                        orderForUI.errCode = result.ERRCODE
                        orderForUI.errMsg = result.ERRMSG
                        orderForUI.actualPayment = result.ACTUAL_PAYMENT
                        orderForUI.accBal = result.REMAIN_BAL.ifEmpty { result.ACC_BAL }
                        saveOrderRecord(orderForUI, result)
                        listener?.onOrderResult(4, orderForUI)
                    } else {
                        orderForUI.errCode = payRes.code
                        orderForUI.errMsg = payRes.msg
                        listener?.onOrderResult(4, orderForUI)
                    }
                }
            }
        }
    }

    private fun getOrderPayData(): String {
        val payBean = Gson().fromJson(Gson().toJson(orderForUI), RequestPayBase::class.java)
        val deviceSerial = CommonAndDpToPxUtil.getDeviceSerial() ?: ""
        val currentTime = System.currentTimeMillis()
        orderForUI.payTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", currentTime)
        payBean.apply {
            actualPayment = String.format("%.02f", orderForUI.payment.toDouble() - orderForUI.discountPayment.toDouble())
            deviceId = deviceSerial
            sessionId = "$deviceSerial$currentTime${Random().nextInt(10)}"
            signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
        }
        return if (orderForUI.orderType == "2") {
            val codePayBean = Gson().fromJson(Gson().toJson(payBean), CodePayBean::class.java)
            codePayBean.qrCode = orderForUI.orderContent
            LogUtil.d(TAG, Gson().toJson(codePayBean))
            DES3CBCUtil.encryption(Gson().toJson(codePayBean))
        } else {
            val cardPayBean = Gson().fromJson(Gson().toJson(payBean), CardPayBean::class.java)
            cardPayBean.cardId = orderForUI.orderContent
            LogUtil.d(TAG, Gson().toJson(cardPayBean))
            DES3CBCUtil.encryption(Gson().toJson(cardPayBean))
        }
    }

    fun placeAnOrder(orderForUI: OrderForUI, verifyStatus: Boolean, orderResult: (type: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            orderResult(1)
            //下单
//            val orderBean = getOrderMealData(orderForUI)
            val orderBean = getBatchOrder(orderForUI)
            LogUtil.d(TAG, Gson().toJson(orderBean))
//            val orderRes = mRepository.insertOrder(orderForUI.ccbToken, orderBean)
            val orderRes = mRepository.insertBatchOrder(orderForUI.ccbToken, orderBean)
            if (orderRes.code == "200") {
                orderResult(2)
//                orderForUI.orderId = orderRes.data?.orderId ?: ""
                orderForUI.orderId = orderRes.data?.pOderId ?: ""
                //支付
                val encryption = getOrderPayData(orderForUI, verifyStatus)
                val payRes = mRepository.payByIcCard(encryption)
                if (payRes.code == "200") {
                    val decryptStr = DES3CBCUtil.decryptRSA(payRes.data ?: "")
                    val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                    LogUtil.d(TAG, Gson().toJson(result))
                    orderForUI.result = result.RESULT
                    orderForUI.errCode = result.ERRCODE
                    orderForUI.errMsg = result.ERRMSG
                    orderForUI.actualPayment = result.ACTUAL_PAYMENT
                    orderForUI.accBal = result.REMAIN_BAL.ifEmpty { result.ACC_BAL }
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

    private fun getBatchOrder(orderForUI: OrderForUI): InsertBatchOrderBean {
        orderForUI.orderTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
        val batchOrderBean = InsertBatchOrderBean().apply {
            campusId = orderForUI.campusId
            businessId = orderForUI.businessId
            custId = orderForUI.custId
            totalPackagingFee = "0.00"
            totalPayment = orderForUI.payment
            actualTotalPayment = String.format("%.02f", orderForUI.payment.toDouble() - orderForUI.discountPayment.toDouble())
            totalDiscountPayment = orderForUI.discountPayment
        }
        orderForUI.menuList.forEach { dateMenu ->
            val discountMoney = orderForUI.discountPayment.toDouble() / orderForUI.menuList.size
            dateMenu.mealList.forEach { mealMenu ->
                val orderDetail = OrderDetail().apply {
                    personName = orderForUI.custName
                    phone = orderForUI.phone
                    mealId = mealMenu.mealId
                    orderType = orderForUI.distribute
                    isPackage = "2"
                    packagingFee = "0.00"
                    deliveryFee = "0.00"
                    mealDate = dateMenu.date
                    addressPersonName = orderForUI.custName
                    addressTel = orderForUI.phone
                    address = orderForUI.address
                }
                var totalMoney: Double = 0.0
                mealMenu.dishList.forEach { dish ->
                    val orderDishes = OrderDishes().apply {
                        dishesId = dish.dishId
                        dishesName = dish.dishName
                        unit = dish.dishUnit
                        dishesPrice = dish.dishPrice
                        dishesNum = dish.dishCount.toString()
                        imgUrl = dish.imgUrl
                        windowIdList = dish.windowIdList
                        description = dish.description
                    }
                    orderDetail.dcOrderDishesList.add(orderDishes)
                    totalMoney += dish.dishCount * dish.dishPrice.toDouble()
                }
                orderDetail.payment = String.format("%.02f", totalMoney)
                orderDetail.discountPayment = String.format("%.02f", discountMoney / dateMenu.mealList.size)
                orderDetail.actualPayment = String.format("%.02f", orderDetail.payment.toDouble() - orderDetail.discountPayment.toDouble())
                batchOrderBean.orderDetail.add(orderDetail)
            }
        }
        return batchOrderBean
    }

    private fun getOrderPayData(orderForUI: OrderForUI, verifyStatus: Boolean): String {
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
        if (orderForUI.distribute == "2") payBean.verifyFlag = if (verifyStatus) "1" else "2"
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

    override fun onCleared() {
        super.onCleared()
        FaceScanVM.instance.setFaceListener(null)
    }

    interface OrderMealListener {
        /**
         * @param type 0-错误 1-成功
         */
        fun onOrderResult(type: Int, any: Any)
    }
}