package com.yannuo.dgcanteen.activitys.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.fragment.laundry.LaundryOrderResult
import com.yannuo.dgcanteen.activitys.repositorys.LaundryRepository
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.NTScanHelp
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class LaundryModeVM : ViewModel(), ScanDevice.DataCallBack, OnReadDataListener {
    private val tag = javaClass.simpleName
    private val kv = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val awaitStatus: MutableLiveData<String> = MutableLiveData("")
    private val userName: MutableLiveData<String> = MutableLiveData("")
    private val reorderStatus: MutableLiveData<Boolean> = MutableLiveData(false)
    private var currentCustId: String = ""
    private var currentCcbToken: String = ""
    private var loginOrPayStatus: Boolean = true
    private var orderForUI = OrderForUI()

    private val laundryRepository = LaundryRepository()
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()
    private var listener: OrderMealListener? = null

    private var mCardHandle: SerialPortHelper? = null
    private var mScanDevice: ScanDevice? = null
    private var ntHelp: NTScanHelp? = null

    private var orderStatus = OrderStatus.INVALID

    val clothingItems = MutableLiveData<List<DishesInfo>>()
    val cartItems = MutableLiveData<List<DishesInfo>>(emptyList())
    val totalPrice = MutableLiveData(0.0)
    val paymentStatus = MutableLiveData<String>()
    val laundryRecords = MutableLiveData<MutableList<Order>>()

    private val mHandler = CoroutineExceptionHandler { _, throwable ->
        LogUtil.e(tag, "Exception: $throwable")
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

    fun getClothingTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
            if (payCfg == null || payCfg.campusId.isNullOrEmpty() || payCfg.businessId.isNullOrEmpty()) {
                LogUtil.e(tag, "Failed to get clothing types: campusId or businessId is not configured.")
                clothingItems.postValue(emptyList()) // Post empty list to clear UI
                return@launch
            }

            if (currentCcbToken.isEmpty()) {
                LogUtil.e(tag, "Failed to get clothing types: Authentication token is missing.")
                clothingItems.postValue(emptyList())
                return@launch
            }

            val request = LaundryListRequest(
                campusId = payCfg.campusId,
                businessId = payCfg.businessId,
                custId = currentCustId,
                page = 1,
                pageSize = 20
            )
            val response = laundryRepository.getLaundryList(currentCcbToken, request)
            if (response.code == "200") {
                val listType = object : TypeToken<List<DishesInfo>>() {}.type
                val dishesList: List<DishesInfo> = Gson().fromJson(response.data?.get("list"), listType) ?: emptyList()
                clothingItems.postValue(dishesList)
            } else {
                LogUtil.e(tag, "Failed to get clothing types: API error code ${response.code} - ${response.msg}")
                clothingItems.postValue(emptyList()) // Post empty list on API error
            }
        }
    }

    fun addToCart(item: DishesInfo) {
        val currentCart = cartItems.value ?: emptyList()
        val existingItem = currentCart.find { it.dishesId == item.dishesId }
        val newCart = currentCart.toMutableList()
        if (existingItem != null) {
            val updatedItem = existingItem.copy(count = existingItem.count + 1)
            val index = newCart.indexOf(existingItem)
            newCart[index] = updatedItem
        } else {
            newCart.add(item.copy(count = 1))
        }
        cartItems.value = newCart
        calculateTotalPrice()
    }

    fun increaseCartItem(item: DishesInfo) {
        val currentCart = cartItems.value ?: return
        val newCart = currentCart.map {
            if (it.dishesId == item.dishesId) {
                it.copy(count = it.count + 1)
            } else {
                it
            }
        }
        cartItems.value = newCart
        calculateTotalPrice()
    }

    fun decreaseCartItem(item: DishesInfo) {
        val currentCart = cartItems.value ?: return
        val existingItem = currentCart.find { it.dishesId == item.dishesId } ?: return

        val newCart = if (existingItem.count > 1) {
            currentCart.map {
                if (it.dishesId == item.dishesId) {
                    it.copy(count = it.count - 1)
                } else {
                    it
                }
            }
        } else {
            currentCart.filterNot { it.dishesId == item.dishesId }
        }
        cartItems.value = newCart
        calculateTotalPrice()
    }

    fun removeCartItem(item: DishesInfo) {
        val currentCart = cartItems.value ?: return
        val newCart = currentCart.filterNot { it.dishesId == item.dishesId }
        cartItems.value = newCart
        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        val currentCart = cartItems.value ?: emptyList()
        var total = 0.0
        for (item in currentCart) {
            total += item.price * item.count
        }
        totalPrice.postValue(total)
    }

    fun placeLaundryOrder() {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            listener?.onOrderResult(2, "洗衣房下单中")

            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
            if (payCfg == null || payCfg.campusId.isNullOrEmpty() || payCfg.businessId.isNullOrEmpty()) {
                LogUtil.e(tag, "Failed to place order: campusId or businessId is not configured.")
                val errorOrder = OrderForUI().apply {
                    errCode = "ORDER_CONFIG_ERROR"
                    errMsg = "下单失败：未配置支付环境"
                    result = "N"
                }
                listener?.onOrderResult(4, LaundryOrderResult(errorOrder, 0))
                return@launch
            }

            if (currentCcbToken.isEmpty()) {
                LogUtil.e(tag, "Failed to place order: Authentication token is missing.")
                val errorOrder = OrderForUI().apply {
                    errCode = "ORDER_LOGIN_ERROR"
                    errMsg = "下单失败：用户未登录"
                    result = "N"
                }
                listener?.onOrderResult(4, LaundryOrderResult(errorOrder, 0))
                return@launch
            }

            val cart = cartItems.value ?: emptyList()
            if (cart.isEmpty()) {
                LogUtil.w(tag, "Attempted to place order with empty cart.")
                val errorOrder = OrderForUI().apply {
                    errCode = "ORDER_CART_EMPTY"
                    errMsg = "下单失败：购物车为空"
                    result = "N"
                }
                listener?.onOrderResult(4, LaundryOrderResult(errorOrder, 0))
                return@launch
            }

            val orderItems = cart.map {
                LaundryOrderItem(
                    dishesId = it.dishesId,
                    dishesName = it.dishesName,
                    dishesNum = it.count,
                    unit = it.unit,
                    imgUrl = it.imgUrl ?: ""
                )
            }

            val request = LaundryOrderRequest(
                campusId = payCfg.campusId,
                businessId = payCfg.businessId,
                custId = currentCustId,
                personName = userName.value ?: "",
                phone = orderForUI.phone,
                payment = "0.00",
                dcOrderDishesList = orderItems,
                orderStatus = "4",
                orderType = "2"
            )
            
            LogUtil.i(tag, "Placing laundry order with request: ${Gson().toJson(request)}")

            val response = laundryRepository.placeLaundryOrder(currentCcbToken, request)

            if (response.code == "200") {
                LogUtil.i(tag, "Laundry order placed successfully. Response: ${response.data}")
                val totalCount = cart.sumOf { it.count }
                val successOrderUI = orderForUI.apply {
                    orderId = response.data?.get("orderId")?.asString ?: ""
                    result = "Y"
                }
                val laundryResult = LaundryOrderResult(successOrderUI, totalCount)
                listener?.onOrderResult(4, laundryResult)
                cartItems.postValue(emptyList())
                calculateTotalPrice()
            } else {
                LogUtil.e(tag, "Failed to place laundry order. Code: ${response.code}, Msg: ${response.msg}")
                val failedOrder = orderForUI.apply {
                    errCode = response.code
                    errMsg = response.msg
                    result = "N"
                }
                listener?.onOrderResult(4, LaundryOrderResult(failedOrder, 0))
            }
        }
    }

    fun placeAnOrder(order: OrderForUI) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            orderForUI = order
            orderForUI.deviceId = CommonAndDpToPxUtil.getDeviceSerial()
            listener?.onOrderResult(2, "洗衣房下单中")

            val orderBean = getInsertOrderBean(orderForUI)
            LogUtil.d(tag, Gson().toJson(orderBean))
            val orderRes = laundryRepository.insertOrder(orderForUI.ccbToken, orderBean)

            if (orderRes.code == "200") {
                orderForUI.orderId = orderRes.data?.orderId ?: ""
                orderForUI.result = "Y"
                listener?.onOrderResult(4, orderForUI)
            } else {
                orderForUI.errCode = orderRes.code
                orderForUI.errMsg = orderRes.msg
                orderForUI.result = "N"
                listener?.onOrderResult(4, orderForUI)
            }
        }
    }

    private fun getInsertOrderBean(orderForUI: OrderForUI): InsertOrderBean {
        orderForUI.orderTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
        val bean = InsertOrderBean().apply {
            campusId = orderForUI.campusId
            businessId = orderForUI.businessId
            custId = orderForUI.custId
            personName = orderForUI.custName
            phone = orderForUI.phone
            address = orderForUI.address
            payment = "0.00"
            discountPayment = "0.00"
            actualPayment = "0.00"
            packagingFee = "0.00"
            deliveryFee = "0.00"
            isPackage = "2"
            orderType = "1"
            mealId = "1"
        }
        orderForUI.dishList.forEach { dish ->
            val insertDish = InsertDish().apply {
                dishesId = dish.dishId
                dishesName = dish.dishName
                dishesPrice = dish.dishPrice.toString()
                dishesNum = dish.dishCount.toString()
                imgUrl = dish.imgUrl
            }
            bean.dcOrderDishesList.add(insertDish)
        }

        return bean
    }

    fun queryLaundryRecords(callback: (Boolean, MutableList<Order>) -> Unit) {
        val emptyList = mutableListOf<Order>()
        laundryRecords.postValue(emptyList)
        callback(true, emptyList)
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
                mScanDevice = ScanDevice()
                mScanDevice?.setCallbackListener(this)
                mScanDevice?.openScan()
                ntHelp = NTScanHelp()
                ntHelp?.OpenScanCode(this, MyApplication.applicationContext)
            }
            "3" -> { //打卡读卡器
                mCardHandle = SerialPortHelper()
                mCardHandle?.readDataListener = this
                mCardHandle?.openSerialPort("/dev/ttyS4")
            }
        }
    }

    fun close(loginType: String) {
        when (loginType) {
            "2" -> {
                mScanDevice?.setCallbackListener(null)
                mScanDevice?.closeScan()
                mScanDevice = null
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
        override fun onFacePay(payForUI: PayForUI) {}
        override fun onFaceQuery(bean: CcbFacePayResultBean) {
            loginHandler("1", Gson().toJson(bean))
        }
    }

    override fun onData(data: String) {
        val icCard = data.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim()
        if (loginOrPayStatus) loginHandler("2", icCard)
    }

    override fun numberOfIcCard(number: String?) {
        if (number == null) return
        val icCard = number.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim().uppercase()
        if (loginOrPayStatus) loginHandler("3", icCard)
    }

    fun loginHandler(type: String, content: String) {
        if (orderStatus != OrderStatus.AWAIT) return
        orderStatus = OrderStatus.INVALID
        listener?.onOrderResult(-1, "验证用户信息")
        LogUtil.d(tag, "type: $type content: $content")
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
                offline = "0"
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
                        // TODO: Here we should query user's phone by custId if possible
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
                                // TODO: Here we should query user's phone by custId if possible
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
                                LogUtil.d(tag, plainText)
                                val cidNo = plainText.substring(0, plainText.indexOf("@"))
                                val person = dbHelper.queryPersonToCidNo(cidNo)
                                if (person != null) {
                                    orderForUI.custId = person.custId
                                    orderForUI.custName = person.personName
                                    orderForUI.phone = person.phone ?: ""
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
                        orderForUI.phone = person.phone ?: ""
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
            LogUtil.d(tag, encryptStr)
            val tokenRes = mRepository.getToken(orderForUI.campusId, orderForUI.corpId, encryptStr)
            LogUtil.d(tag, Gson().toJson(tokenRes))
            if (tokenRes.code == "200") {
                orderForUI.ccbToken = tokenRes.data?.dcccbToken ?: ""
                currentCustId = orderForUI.custId
                currentCcbToken = orderForUI.ccbToken
                userName.postValue(orderForUI.custName)
                this@LaundryModeVM.orderForUI = orderForUI
                getClothingTypes()
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
        LogUtil.d(tag, encryptStr.toString())
        return CanteenEncryptionUtil.encryption(encryptStr.toString())
    }

    interface OrderMealListener {
        /**
         * @param type 0-错误 1-成功 2-下单中 3-扫码支付 4-支付结果 5-刷脸支付
         */
        fun onOrderResult(type: Int, any: Any)
    }

    override fun onCleared() {
        super.onCleared()
        FaceScanVM.instance.setFaceListener(null)
    }
}
