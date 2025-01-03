package com.yannuo.dgcanteen.activitys.viewModel

import androidx.core.text.isDigitsOnly
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
import com.yannuo.dgcanteen.exception.ResponseException
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.AccListTable
import com.yannuo.dgcanteen.greendao.entity.MealTable
import com.yannuo.dgcanteen.greendao.entity.OfflineOrderTable
import com.yannuo.dgcanteen.greendao.entity.PayDishTable
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.greendao.entity.SwPayOrderTable
import com.yannuo.dgcanteen.interfaces.AllowanceStateListener
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class PayViewModel : ViewModel(), ScanDevice.DataCallBack, OnReadDataListener {
    private val TAG = javaClass.simpleName

    private val mRespository: PayRepositoryOfPay = PayRepositoryOfPay()
    private val kv: MMKV = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val mPayCfg: PayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    private val mealTimeVM by lazy { MealTimeVM() }

    val bulkPayAmount: MutableLiveData<String> = MutableLiveData("")

    private var mCardHandle: SerialPortHelper? = null
    private var mScanDevice: ScanDevice? = null
    private var ntHelp: NTScanHelp? = null

    var mDishes: ProductsDetail? = null
    var listener: CallbackListener? = null
    var allowanceStateListener: AllowanceStateListener? = null
    var mReadCardListener: OnReadDataListener? = null
    var mScanCodeListener: ScanDevice.DataCallBack? = null
    private var payState = PayStatus.INVALID

    // sw
    private var isSw = 0 // 0：不是   1：是
    private var isAllowance = 2
    private var mealId: Int? = null
    private var actualMealId: Int? = null
    private var useRuleId: Int? = null

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        listener?.onOtherListener(2, "发生异常信息：${throwable.message}")
    }

    init {
        mReadCardListener = this
        mScanCodeListener = this
    }

    //扫码状态，主要用于区分选择商品 和支付码
    enum class PayStatus {
        INVALID,    //无效状态
        PAY         //支付状态
    }

    fun setPayState(state: PayStatus) {
        payState = state
    }

    fun getPayState() = payState

    fun setIsSw(isSw: Int) {
        this.isSw = isSw
    }

    fun getIsSw() = isSw

    fun setIsAllowance(isAllowance: Int) {
        this.isAllowance = isAllowance
    }

    fun getIsAllowance() = isAllowance

    fun openPayStatus(payType: Int) {
        payState = PayStatus.PAY
        //打卡读卡器
        if (payType == Constant.PAY_IC_TYPE || payType == Constant.PAY_CODE_IC_TYPE) {
            mCardHandle = SerialPortHelper()
            mCardHandle?.readDataListener = mReadCardListener
            mCardHandle?.openSerialPort("/dev/ttyS4")
//            mCardHandle?.openSerialPort("/dev/ttyXRUSB0")
        }
        //打开扫码头
        if (payType == Constant.PAY_CODE_TYPE || payType == Constant.PAY_CODE_IC_TYPE) {
            mScanDevice = ScanDevice()
            mScanDevice?.setCallbackListener(mScanCodeListener)
            mScanDevice?.openScan()
            ntHelp = NTScanHelp()
            ntHelp?.OpenScanCode(mScanCodeListener, MyApplication.applicationContext)
        }
    }

    fun closePayStatus() {
        payState = PayStatus.INVALID
        //取消读取监听
        mCardHandle?.readDataListener = null
        mCardHandle?.closeSerialPort()
        mCardHandle = null
        //取消扫码监听
        mScanDevice?.setCallbackListener(null)
        mScanDevice?.closeScan()
        mScanDevice = null
        ntHelp?.CloseScanCode()
        ntHelp = null
    }

    override fun onData(data: String) {
        if (payState == PayStatus.INVALID || data.isEmpty()) return
        payState = PayStatus.INVALID
        LogUtil.d(TAG, "二维码 :$data")
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            // 获取custId
            var custId: String? = null
            if (isSw == 1) {
                LogUtil.i(TAG, "申万支付...")
                custId = getCustId("2", data)
                if (custId.isNullOrBlank()) {
                    LogUtil.e(TAG, "未查询到该人员信息：content -> $data, type -> 2")
                    listener?.onOtherListener(4, "未查询到该人员信息")
                    return@launch
                }
            } else LogUtil.i(TAG, "寻常支付...")

            paymentLogicHelper(custId, "2", data)
        }
    }

    //IC卡数据
    override fun numberOfIcCard(number: String?) {
        if (payState == PayStatus.INVALID || number == null) return
        payState = PayStatus.INVALID
        val icCard = number.trim().uppercase()
        LogUtil.d(TAG, "卡号 :$icCard")
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            // 获取custId
            var custId: String? = null
            if (isSw == 1) {
                LogUtil.i(TAG, "申万支付...")
                custId = getCustId("3", icCard)
                if (custId.isNullOrBlank()) {
                    LogUtil.e(TAG, "未查询到该人员信息：content -> $icCard, type -> 3")
                    listener?.onOtherListener(4, "未查询到该人员信息")
                    return@launch
                }
            } else LogUtil.i(TAG, "寻常支付...")

            paymentLogicHelper(custId, "3", icCard)
        }
    }

    /**
     * 支付处理
     * @param type 支付类型 1-刷脸 2-扫码 3-刷卡
     * @param content 支付内容 payType: 1-刷脸 2-二维码 3-卡号
     */
    suspend fun paymentLogicHelper(
        custIdArg: String?,
        type: String,
        content: String,
        sw: Boolean = false,
        withoutAllowance: Boolean = false,
        aPayForUI: PayForUI? = null,
        paymentMap: HashMap<String, String>? = null
    ) {
        try {
            val currentTime = System.currentTimeMillis()
            var payForUI: PayForUI? = null
            payForUI = if (!sw) initData(custIdArg, type, content, currentTime) ?: return
            else {
                if (!withoutAllowance) initData(custIdArg, type, content, currentTime) ?: return
                else initDataByPaymentMap(paymentMap!!, aPayForUI!!, currentTime) ?: return
            }

            if (kv.decodeBool(Constant.REPEAT_PAY_JUDGE, true)) {
                var custId = ""
                when (payForUI.payType) {
                    "2" -> {
                        when {
                            payForUI.payContent.contains("CCB") -> {
                                val plainText = DES3CBCUtil.transDecryption(payForUI.payContent)
                                val cidNo = plainText.substring(0, plainText.indexOf("@"))
                                val person = dbHelper.queryPersonToCidNo(cidNo)
                                if (person != null) custId = person.custId
                            }
                            payForUI.payContent.contains("CT0001") -> {
                                val res = runBlocking(Dispatchers.IO + mHandler) {
                                    val map = CanteenEncryptionUtil.getAnalysisQr(payForUI.campusId, "PAY002", payForUI.corpId, payForUI.payContent)
                                    val analysisResult = mRespository.getCcbData(map).body()?.string() ?: ""
                                    Gson().fromJson(analysisResult.replace("\r\n", ""), ScanAnalysisBean::class.java)
                                }
                                if (res != null) {
                                    val person = dbHelper.queryPersonToCustId(res.CUST_ID)
                                    if (person != null) custId = person.custId
                                }
                            }
                        }
                    }
                    "3" -> {
                        val person = dbHelper.queryPersonToCardId(payForUI.payContent)
                        if (person != null) custId = person.custId
                    }
                }
                val record = dbHelper.queryPayOrderRecord(custId, payForUI.payContent)
                if (record != null) {
//                    LogUtil.d(TAG, Gson().toJson(record))
                    val timeStr = if (record.payTime == null || record.payTime.isEmpty()) "2000/01/01 00:00:00" else record.payTime.replace("-", "/")
                    if (System.currentTimeMillis() - Date(timeStr).time < 30000 && record.payment == payForUI.payment) {
                        LogUtil.d(TAG, "重复支付")
                        listener?.onOtherListener(8, payForUI)
                        return
                    }
                }
            }
            confirmPay(payForUI)
        } catch (e: Exception) {
            LogUtil.e(TAG, "Exception: $e")
            e.printStackTrace()
            listener?.onOtherListener(2, "发生异常信息：${e.message}")
        }
    }

    fun confirmPay(payForUI: PayForUI) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            listener?.onOtherListener(1)
            when (payForUI.payType) {
                "2" -> qrCodeHandler(payForUI)
                "3" -> icCardHandler(payForUI)
            }
        }
    }

    /**
     * 初始化消费数据
     */
    private suspend fun initData(custId: String?, type: String, content: String, currentTime: Long): PayForUI? {
        val payForUI = PayForUI()
        payForUI.payType = type
        payForUI.custId = custId ?: ""
        payForUI.payContent = content
        payForUI.isSw = isSw
        // 可以在这里计算实际金额
        val paymentMap = getPayment(custId ?: "", payForUI)
        LogUtil.i(TAG, "paymentMap: $paymentMap")
        if (paymentMap == null) return null

        return initDataByPaymentMap(paymentMap, payForUI, currentTime)
    }

    private fun initDataByPaymentMap(paymentMap: HashMap<String, String>, payForUI: PayForUI, currentTime: Long): PayForUI? {
        val deviceSerial = CommonAndDpToPxUtil.getDeviceSerial() ?: ""
        payForUI.apply {
            businessId = mPayCfg.businessId
            businessName = mPayCfg.businessName
            campusId = mPayCfg.campusId
            corpId = mPayCfg.corp_id
            vposId = mPayCfg.counterId
            deviceId = deviceSerial
            payContent = payContent.replace("\n", "").replace("\r", "")
            this.payment = paymentMap.get("payment") ?: "0.00"
            actualPayment = paymentMap.get("actualPayment") ?: "0.00"
            payTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", currentTime)
            payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
            sessionId = "$deviceSerial$currentTime${Random().nextInt(10)}"
            signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
            offline = if (NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) "0" else "1"
        }
        mDishes?.products?.forEach {
            val dish = Dish().apply {
                dishesId = it.dishesId
                dishesName = it.dishesName
                dishesNumber = it.count.toString()
                dishesPrice = it.price.toString()
            }
            payForUI.paymentDishes.add(dish)
        }
        return payForUI
    }

    suspend fun getCustId(type: String, content: String): String? {
        // 解析出 custId
        when(type) {
            "1" -> {
                // todo 刷脸
                return ""
            }
            "2" -> {
                // 扫码
                if (!(content.startsWith("CCB") || content.startsWith("CT0001"))) {
                    LogUtil.e(TAG, "无效二维码！")
                    if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) listener?.onOtherListener(9, "无效二维码！")
                    else listener?.onOtherListener(4, "无效二维码！")
                    return null
                }
                if (content.startsWith("CCB")) {
                    val plainText = DES3CBCUtil.transDecryption(content)
                    val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
                    val minutes = TimeUtil.timestamp(pastDueTime)
                    if (minutes <= 1) {
                        LogUtil.e(TAG, "二维码已过期！")
                        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) listener?.onOtherListener(9, "二维码已过期！")
                        else listener?.onOtherListener(4, "二维码已过期！")
                        return null
                    }
                    // 学号/工号
                    val personNumber = plainText.substring(0, plainText.indexOf("@"))
                    val person = dbHelper.queryPersonToNumber(personNumber)
                    return person.custId
                }
                if (kv.decodeBool(Constant.SWITCH)) {
                    withContext(Dispatchers.Main) {
                        ToastShowUtil.show("当前设备无网络，无法完成支付")
                    }
                    return null
                }
                if (content.startsWith("CT0001")) {
                    val analysisRes = runBlocking {
                        val qrCodeMap = CanteenEncryptionUtil.getAnalysisQr(mPayCfg.campusId ?: "", "PAY002", mPayCfg.corp_id ?: "", content)
                        val analysisResult = mRespository.getCcbData(qrCodeMap).body()?.string() ?: ""
                        Gson().fromJson(analysisResult.replace("\r\n", ""), ScanAnalysisBean::class.java)
                    }
                    if (analysisRes.RESULT != "Y") {
                        LogUtil.e(TAG, "在线二维码解析失败！")
                        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) listener?.onOtherListener(9, analysisRes.ERRMSG)
                        else listener?.onOtherListener(4, analysisRes.ERRMSG)
                        return null
                    }
                    return analysisRes.CUST_ID
                }
            }
            "3" -> {
                // 刷卡
                var person = dbHelper.queryPersonToCardId(content)
                LogUtil.i(TAG, "db person: $person")
                if (person == null) {
                    if (kv.decodeBool(Constant.SWITCH) ) {
                        withContext(Dispatchers.Main) {
                            ToastShowUtil.show("当前设备无网络，无法完成支付")
                        }
                        return null
                    }
                    val response = runBlocking {
                        val request = RequestPerson().apply {
                            campusId = mPayCfg.campusId
                            cardId = content
                        }
                        mRespository.queryPersonByCardId(request)
                    }
                    if (response.code == "200") {
                        person = response.data
                    } else {
                        if (kv.decodeInt(Constant.QUERY_TIME_SWITCH, 0) == 1) listener?.onOtherListener(9, response.msg)
                        else listener?.onOtherListener(4, response.msg)
                        return null
                    }
                    LogUtil.i(TAG, "request person: $response")
                }
                if (person != null) {
                    dbHelper.insertPersons(mutableListOf(person))
                    return person.custId
                }
                return ""
            }
            else -> return ""
        }
        return ""
    }

    private suspend fun getPayment(custId: String, payForUI: PayForUI): HashMap<String, String>? {
       val res = hashMapOf<String, String>()
        //原价
        res["payment"] = String.format("%.02f", (mDishes?.totalMoney ?: "0.00").toFloat())
        res["actualPayment"] = String.format("%.02f", (mDishes?.totalMoney ?: "0.00").toFloat())

        val queryAllMeals = dbHelper.queryAllMeals()
        val currentMeal = dbHelper.queryToMeals(TimeUtil.CurrentTimeSection())
        val person = dbHelper.queryPersonToCustId(custId)
        // 支付金额 to 实际支付金额
        when (isSw) {
            1 -> {
                // 网络查询餐次规则
                val mealTimeRuleMap = mealTimeVM.requestMealTimeRule(custId)
                LogUtil.i(TAG, "获取餐次规则: $mealTimeRuleMap")
                if (mealTimeRuleMap.size > 0 && mealTimeRuleMap[-1] == null) {
                    // 网络查询个人剩余次数
                    val restTimeMap = mealTimeVM.requestPersonRestMealTime(custId)
                    if (restTimeMap.size > 0 && restTimeMap[-1] != null) {
                        listener?.onOtherListener(4, restTimeMap[-1])
                        return null
                    }
                    LogUtil.i(TAG, "剩余餐次：$restTimeMap")
                    var no = -1  // 计算当前餐别的序号
                    for ((i,v) in queryAllMeals.withIndex()) {
                        if (v.mealId == TimeUtil.CurrentTimeSection()) {
                            no = i + 1
                            break
                        }
                    }
                    LogUtil.i(TAG, "currentMeal: $currentMeal")
                    LogUtil.i(TAG, "mealTimeRuleMap: ${mealTimeRuleMap}")
                    try {
                        if (mealTimeRuleMap[no] == null) {
                            // todo 是否使用零点支付
                            if (bulkPayAmount.value.isNullOrBlank()) {
                                listener?.onOtherListener(4, "无可用餐标，价格未知")
                                return null
                            }
                            return substractAllowance(false, payForUI, person, currentMeal, restTimeMap, mealTimeRuleMap, no)
                        } else {
                            // 有当前餐别餐标
                            return substractAllowance(true, payForUI, person, currentMeal, restTimeMap, mealTimeRuleMap, no)
                        }
//                        LogUtil.i(TAG, "paymentMap: $res")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            ToastShowUtil.show(e.message)
                        }
                        LogUtil.e(TAG, "error: ${e.message}")
                        listener?.onOtherListener(4, e.message)
                        return null
                    }
                }
                listener?.onOtherListener(4, mealTimeRuleMap[-1]?.mealName ?: "无任何餐次规则")
                return null
            }
            else -> return res
        }
    }

    private suspend fun substractAllowance(
        hasExclusiveRule: Boolean,
        payForUI: PayForUI,
        person: Persons,
        currentMeal: MealTable,
        restTimeMap: HashMap<Int, PersonRestMealTime>,
        mealTimeRuleMap: HashMap<Int, MealTimeRuleInfo>,
        no: Int
    ): HashMap<String, String>? {
        val res = hashMapOf<String, String>()
        val queryAllMeals = dbHelper.queryAllMeals()

        val actualMealTimeRule = getActualMealTimeRule(person.custId, currentMeal.mealId)
        LogUtil.i(TAG, "actualMealTimeRule: $actualMealTimeRule")
        if (actualMealTimeRule == null) {
            // 无补贴，用当前餐别餐标单价，以原价
            isAllowance = 2
            if (!bulkPayAmount.value.isNullOrBlank()) {
                res["payment"] = String.format("%.02f", (bulkPayAmount.value ?: "0.00").toFloat())
                res["actualPayment"] = String.format("%.02f", (bulkPayAmount.value?: "0.00").toFloat())
            } else {
                if (hasExclusiveRule) {
                    res["payment"] = String.format("%.02f", (mealTimeRuleMap[no]?.price ?: 0.00f).toFloat())
                    res["actualPayment"] = String.format("%.02f", (mealTimeRuleMap[no]?.price ?: 0.00f).toFloat())
//                payForUI.swForUI.rulePrice = (mealTimeRuleMap[no]?.price ?: 0.00f).toFloat()
                    payForUI.swForUI.actualPrice = String.format("%.02f", (mealTimeRuleMap[no]?.price ?: 0.00f).toFloat())
                } else return null
            }

            payForUI.payment = res["payment"] ?: "0.00"
            payForUI.actualPayment = res["actualPayment"] ?: "0.00"

            actualMealId = currentMeal.mealId
            mealId = currentMeal.mealId

            payForUI.swForUI.apply {
                username = person.personName
                meal01Time = restTimeMap[1]?.updateCount ?: 0
                meal02Time = restTimeMap[2]?.updateCount ?: 0
                meal03Time = restTimeMap[3]?.updateCount ?: 0
                meal04Time = restTimeMap[4]?.updateCount ?: 0

                this.actualMealId = currentMeal.mealId.toString()
                actualMealName = currentMeal.mealName
                useMealRuleName = "无补贴"
                if (hasExclusiveRule) {
                    useRuleId = mealTimeRuleMap[no]?.id
                    useMealRuleName = "无补贴(${mealTimeRuleMap[no]?.standardName})"
                    rulePrice = mealTimeRuleMap[no]?.price ?: 0.00f
//                    everyUseTime = mealTimeRuleMap[no]?.standardNum.toString()
                }
                restTime = 0
            }
            if (hasExclusiveRule) {
                allowanceStateListener?.withoutAllowance(res, payForUI)
                return null
            }
            return res
        } else {
            // 有补贴，用当前餐别餐标单价 - 补贴
            isAllowance = 1
            if (!bulkPayAmount.value.isNullOrBlank()) {
                val payment = (bulkPayAmount.value ?: "0.00").toFloat()
                val actualPayment = (bulkPayAmount.value?: "0.00").toFloat() - actualMealTimeRule.subsidyMoney
                res["payment"] = String.format("%.02f", payment)
                res["actualPayment"] = String.format("%.02f", if (actualPayment < 0) 0.00f else actualPayment)
            } else {
                if (hasExclusiveRule) {
                    res["payment"] = String.format("%.02f", (mealTimeRuleMap[no]?.price ?: 0.00f).toFloat())
                    res["actualPayment"] = String.format("%.02f", (mealTimeRuleMap[no]?.price ?: 0.00f).toFloat() - actualMealTimeRule.subsidyMoney)
                } else return null
            }
            payForUI.payment = res["payment"] ?: "0.00"
            payForUI.actualPayment = res["actualPayment"] ?: "0.00"

            actualMealId = actualMealTimeRule.actualMealId
            mealId = actualMealTimeRule.mealId
            useRuleId = actualMealTimeRule.id

            payForUI.swForUI.apply {
                username = person.personName
                meal01Time = restTimeMap[1]?.updateCount ?: 0
                meal02Time = restTimeMap[2]?.updateCount ?: 0
                meal03Time = restTimeMap[3]?.updateCount ?: 0
                meal04Time = restTimeMap[4]?.updateCount ?: 0

                this.actualMealId = currentMeal.mealId.toString()
                actualMealName = currentMeal.mealName
                if (hasExclusiveRule) actualPrice = mealTimeRuleMap[no]?.price.toString()
                for (v in queryAllMeals) {
                    if (v.mealId == actualMealTimeRule.mealId) {
                        this.standardMealId = v.mealId.toString()
                        standardMealName = v.mealName ?: ""
                        break
                    }
                }
                for (v in mealTimeRuleMap.values) {
                    if (v.id == actualMealTimeRule.id) {
                        this.useMealRuleId = v.id.toString()
                        useMealRuleName = v.standardName
                        break
                    }
                }
                everyUseTime = actualMealTimeRule.standardNum.toString()
                restTime = actualMealTimeRule.leftOverTimes
                rulePrice = actualMealTimeRule.price
                subsidy = actualMealTimeRule.subsidyMoney.toString()
            }

            if (!bulkPayAmount.value.isNullOrBlank() && !hasExclusiveRule) {
                if (payForUI.actualPayment.trim().toFloat() >= 1e-6f ) {
                    // 大于 0
                    allowanceStateListener?.hasAllowance(res, payForUI)
                    return null
                }
            }
            return res
        }
    }

    fun showFacePayResult(payForUI: PayForUI) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            // 查询餐次
            val restMealTime = mealTimeVM.requestPersonRestMealTime(payForUI.custId)
            payForUI.swForUI.apply {
                username = payForUI.username
                balance = payForUI.accBal
                if (restMealTime.size > 0 && restMealTime[-1] != null) {
                    withContext(Dispatchers.Main + mHandler) {
                        ToastShowUtil.show("查询餐次失败：${restMealTime[-1]}")
                        LogUtil.e(TAG, "showFacePayResult 查询餐次失败：${restMealTime[-1]}")
                    }
                    meal01Time = -1
                    meal02Time = -1
                    meal03Time = -1
                    meal04Time = -1
                } else{
                    restMealTime.forEach {
                        when (it.key) {
                            1 -> meal01Time = it.value.updateCount
                            2 -> meal02Time = it.value.updateCount
                            3 -> meal03Time = it.value.updateCount
                            4 -> meal04Time = it.value.updateCount
                            else -> {}
                        }
                    }
                }
                // 查询餐标
                val queryAllowance = mealTimeVM.queryAllowance(payForUI.orderId)
                LogUtil.i(TAG, "queryAllowance: $queryAllowance")
                if (!queryAllowance.first.isNullOrBlank()) {
                    // 有错误
                    withContext(Dispatchers.Main + mHandler) {
                        ToastShowUtil.show("查询餐标失败：${queryAllowance.first}")
                        LogUtil.e(TAG, "showFacePayResult 查询餐标失败：${queryAllowance.first}")
                    }
                } else {
                    val queryAllowanceData = queryAllowance.second
                    if (queryAllowanceData != null && queryAllowanceData.isAllowance == "1") {
                        payForUI.swForUI.apply {
                            standardMealName = queryAllowanceData.mealName
                            useMealRuleName = queryAllowanceData.standardName
                            subsidy = queryAllowanceData.subsidyMoney
                            rulePrice = BigDecimal(queryAllowanceData.price).setScale(2, BigDecimal.ROUND_HALF_UP).toString().toFloat()
                            restTime = queryAllowanceData.leftOfTimes
                            everyUseTime = queryAllowanceData.leftOfTimes.toString()
                        }
                    }
                }
            }
            delay(1500)
            listener?.onOtherListener(3, payForUI)
        }
    }

    fun queryRestTimeAndBalance(custId: String, type: String, content: Any) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            var realCustId = custId
            var name = ""
            var bal = ""
            if (type == "1") {
                delay(1500)
                val bean = content as CcbFacePayResultBean
                if (bean.RESULT == "N") {
                    listener?.onOtherListener(9, bean.ERRMSG)
                    return@launch
                }
                realCustId = bean.CUST_ID
                name = bean.CUST_NAME
            }

            val restMealTime = mealTimeVM.requestPersonRestMealTime(realCustId)
            if (restMealTime.size > 0 && restMealTime[-1] != null) {
                listener?.onOtherListener(9, restMealTime[-1])
                return@launch
            }
            when (type) {
                "1" -> {
                    bal = queryBalanceByCustId(realCustId) ?: return@launch
                }
                "2","3" -> {
                    val queryBalance = queryBalance(type, content as String)
                    if (queryBalance == null) {
                        listener?.onOtherListener(9, "异常错误")
                        return@launch
                    }
                    LogUtil.i(TAG, "queryRestTimeAndBalance: $queryBalance")
                    if (queryBalance.RESULT == "N") {
                        listener?.onOtherListener(9, queryBalance.ERRMSG)
                        return@launch
                    }
                    name = queryBalance.personName
                    bal = queryBalance.REMAIN_BAL
                }
            }
            LogUtil.i(TAG, "queryRestTimeAndBalance: $restMealTime")
            val payForUI = PayForUI().apply {
                this.result = "Y"
                username = name
                accBal = bal
                swForUI = SwForUI().apply {
                    this.result = "N"
                    this.username = name
                    this.balance = bal
                    restMealTime.forEach {
                        when (it.key) {
                            1 -> meal01Time = it.value.updateCount
                            2 -> meal02Time = it.value.updateCount
                            3 -> meal03Time = it.value.updateCount
                            4 -> meal04Time = it.value.updateCount
                            else -> {}
                        }
                    }
                }
            }
            listener?.onOtherListener(8, payForUI)
            return@launch
        }
    }

    private suspend fun queryBalanceByCustId(custId: String): String? {
        val prikey = CanteenEncryptionUtil.encryption("CUST_ID=${custId}&ACC_NO=")
        val hashMap = HashMap<String,String>()
        hashMap.put("CCB_IBSVersion","V6")
        hashMap.put("PT_STYLE","8")
        hashMap.put("PT_LANGUAGE","CN")
        hashMap.put("CAMPUS_ID", mPayCfg.campusId)
        hashMap.put("TXCODE","VIAC05")
        hashMap.put("CORP_ID", mPayCfg.corp_id)
        hashMap.put("ccbSafeParam",prikey)

        val res = mRespository.ccbPersonBanlance(hashMap)
        if (res.code != "200"){
            LogUtil.e(TAG, "queryBalanceByCustId error: ${res.msg}")
            ToastShowUtil.show("queryBalanceByCustId error: ${res.msg}")
            listener?.onOtherListener(9, res.msg)
            return null
        }
        val data = res.data
        if (data == null) {
            LogUtil.e(TAG, "queryBalanceByCustId error data is null" )
            ToastShowUtil.show("queryBalanceByCustId error data is null")
            listener?.onOtherListener(9, "data is null")
            return null
        }
        if (data.RESULT == "Y") {
            var allMoney = 0.00f
            data.ACC_DATA?.forEach {
                allMoney += it.MONEY.toFloat()
            }
            return allMoney.toString()
        } else {
            LogUtil.e(TAG, "queryBalanceByCustId error: ${data.ERRMSG}" )
            ToastShowUtil.show("queryBalanceByCustId error: ${data.ERRMSG}")
            listener?.onOtherListener(9, data.ERRMSG)
            return null
        }
    }
    private suspend fun queryBalance(type: String, content: String): QueryBalanceResponse? {
        try {
            val gson = Gson()
            val request = QueryBalanceRequest().apply {
                campusId = mPayCfg.campusId
                when (type) {
                    "2" -> qrCode = content
                    "3" -> cardId = content
                }
            }
            LogUtil.i(TAG, "queryBalance 加密前：$request")
            val encryption1 = DES3CBCUtil.encryption(gson.toJson(request))
            val queryBalance = mRespository.queryBalance(EncryptedDataRequest(encryption1))
            LogUtil.i(TAG, "查询余额：${queryBalance}")
            if (queryBalance.code != "200") {
                LogUtil.e(TAG, "queryBalance error: $queryBalance")
                throw ResponseException(queryBalance.code, queryBalance.msg)
            }
            if (queryBalance.data == null) throw ResponseException("", "queryBalance response data == null")
            val decryptRSA = DES3CBCUtil.decryptRSA(queryBalance.data)
            LogUtil.i(TAG, "queryBalance response 解密：$decryptRSA")
            val fromJson = Gson().fromJson(decryptRSA, QueryBalanceResponse::class.java)
            return fromJson
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, e.message)
            withContext(Dispatchers.Main) {
                if (e is ResponseException) {
                    ToastShowUtil.show("错误： $e")
                } else ToastShowUtil.show("错误： ${e.message}")
            }
            return null
        }
    }

    /**
     * TODO
     *
     * @param mealTimeMap
     * @param restTimeMap
     * @param no 餐别编号，用于排序、获取
     * @return first: mealId  second: (restTime, rule)
     */
    private suspend fun getActualMealTimeRule(custId: String, currentMealId: Int): MealRestTimeReceive? {
        val mealRestTime = runBlocking(Dispatchers.IO + mHandler) {
            mealTimeVM.requestMealRestTime(custId, currentMealId)
        }
        val error = mealRestTime.first
        val data = mealRestTime.second
        LogUtil.i(TAG, "实际使用餐标规则：$mealRestTime")
        if (!error.isNullOrBlank() || data == null) throw Exception(error)
        return if (!(data.leftOverTimes == 0 && data.subsidyMoney == 0.0f && data.mealId == 0 && data.flag == 0 && data.standardNum == 0 && data.price == 0.0f && data.id == 0 && data.useTimes == 0)
            && data.useTimes != 0 && (data.useTimes - data.standardNum >= 0)
        ) data
        else null
    }

    private fun qrCodeHandler(payForUI: PayForUI) {
        when {
            payForUI.payContent.contains("CT0001") && payForUI.offline == "0" -> {
                //解析在线码获取用户信息
//                val analysisBean = runBlocking(Dispatchers.IO + mHandler) {
//                    val map = CanteenEncryptionUtil.getAnalysisQr(payForUI, "PAY002")
//                    val analysisResult = mRespository.getCcbData(map).body()?.string() ?: ""
//                    Gson().fromJson(analysisResult.replace("\r\n", ""), ScanAnalysisBean::class.java)
//                }
//                if (analysisBean.RESULT == "Y") {
//                    payForUI.custId = analysisBean.CUST_ID
//                    val person = dbHelper.queryPersonToCustId(payForUI.custId)
//                    payForUI.username = person.personName
                if (isSw != 1) {
                    onLinePay(payForUI)
                } else {
                    swOnLinePay(payForUI)
                }
//                } else {
//                    payForUI.errCode = analysisBean.ERRCODE
//                    payForUI.errMsg = analysisBean.ERRMSG
//                    listener?.onOtherListener(3, payForUI)
//                }
            }
            payForUI.payContent.contains("CCB") -> {
                val plainText = DES3CBCUtil.transDecryption(payForUI.payContent)
                val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
                val minutes = TimeUtil.timestamp(pastDueTime)
                if (minutes > 1) {
                    //将离线码的密文串解密可以得到cidNo(学号)
//                    val cidNo = plainText.substring(0, plainText.indexOf("@"))
//                    //将离线码的密文串解密可以得到campusId(园区编号)
//                    val campusId = plainText.substring(plainText.indexOf("@") + 1, plainText.lastIndexOf("@"))
//                    if (payForUI.campusId == campusId) {
//                        //查询人员信息
//                        val person = dbHelper.queryPersonToCidNo(cidNo)
//                        if (person != null && person.custId.isNotEmpty()) {
//                            payForUI.custId = person.custId
//                            payForUI.username = person.personName
                    if (payForUI.offline == "0") {
                        if (isSw != 1) {
                            onLinePay(payForUI)
                        } else {
                            swOnLinePay(payForUI)
                        }
                    } else {
                        val person: Persons
                        //将离线码的密文串解密可以得到cidNo(学号)
                        val plainText = DES3CBCUtil.transDecryption(payForUI.payContent)
                        val cidNo = plainText.substring(0, plainText.indexOf("@"))
                        person = dbHelper.queryPersonToCidNo(cidNo)
                        LogUtil.d(TAG, Gson().toJson(person))
                        payForUI.custId = person.custId ?: ""
                        payForUI.username = person.personName ?: ""
                        offLinePay(payForUI)
                    }
//                        } else {
//                            payForUI.errCode = "PAY0001"
//                            payForUI.errMsg = "未查询到人员信息"
//                            listener?.onOtherListener(3, payForUI)
//                        }
//                    } else listener?.onOtherListener(5, 0)
                } else listener?.onOtherListener(5, 1)
            }
            else -> if (payForUI.offline == "0") {
                if (isSw != 1) {
                    onLinePay(payForUI)
                } else {
                    swOnLinePay(payForUI)
                }
            } else listener?.onOtherListener(5, 0)
        }
    }

    private fun icCardHandler(payForUI: PayForUI) {
        LogUtil.i(TAG, "icCardHandler payForUI: $payForUI")
        var state = true
        val person = dbHelper.queryPersonToCardId(payForUI.payContent)
        if (person != null && person.custId.isNotEmpty()) {
            state = false
            payForUI.custId = person.custId
            payForUI.username = person.personName
            if (isSw != 1) {
                if (payForUI.offline == "0") onLinePay(payForUI) else offLinePay(payForUI)
            } else {
                // 申万 餐次
                swOnLinePay(payForUI)
            }
        } else if (payForUI.offline == "0") {
            runBlocking(mHandler) {
                val result = mRespository.queryPersonByCardId(RequestPerson(payForUI.campusId, payForUI.payContent))
                if (result.code == "200") {
                    payForUI.custId = result.data?.custId ?: ""
                    payForUI.username = result.data?.personName ?: ""
                    if (payForUI.custId.isNotEmpty()) {
                        state = false
                        if (isSw != 1) {
                            onLinePay(payForUI)
                        } else {
                            // 申万 餐次
                            swOnLinePay(payForUI)
                        }
                    }
                }
            }
        }
        if (state) {
            payForUI.errCode = "PAY0001"
            payForUI.errMsg = "未查询到人员信息"
            listener?.onOtherListener(3, payForUI)
        }
    }

    private fun swOnLinePay(payForUI: PayForUI) {
        LogUtil.i(TAG, "swOnLinePay payForUI: $payForUI")

        runBlocking(mHandler) {
            val mealTable = DishesDBHelper.getInstance().queryToMeals(TimeUtil.CurrentTimeSection())
            val response = when (payForUI.payType) {
                "2" -> {
                    val payDetail = Gson().fromJson(Gson().toJson(payForUI), PayForUI::class.java)
                    payDetail.payment = payForUI.actualPayment
                    val request = Gson().fromJson(Gson().toJson(payDetail), CodePayBean::class.java)
                    request.qrCode = payForUI.payContent
                    val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                    mRespository.swPayByQrCode(encryption, "sw", isAllowance.toString(), actualMealId ?: mealTable.mealId,(mealId ?: actualMealId) ?: mealTable.mealId, useRuleId)
                }
                "3" -> {
                    val payDetail = Gson().fromJson(Gson().toJson(payForUI), PayForUI::class.java)
                    payDetail.payment = payForUI.actualPayment
                    val request = Gson().fromJson(Gson().toJson(payDetail), CardPayBean::class.java)
                    request.cardId = payForUI.payContent
                    LogUtil.i(TAG, "request: $request")
                    val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                    LogUtil.i(TAG, "刷卡餐次：mealId --> ${mealId ?: mealTable.mealId}, useRuleId --> $useRuleId")
                    mRespository.swPayByIcCard(encryption, "sw", isAllowance.toString(), actualMealId ?: mealTable.mealId,(mealId ?: actualMealId) ?: mealTable.mealId, useRuleId)
                }
                else -> CanteenResponse<String>()
            }
            if (response.code == "200") {
                val decryptStr = DES3CBCUtil.decryptRSA(response.data ?: "")
                val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                LogUtil.i(TAG, "swOnLinePay result: $result")
                payForUI.result = result.RESULT
                payForUI.accType = result.ACC_TYPE
                payForUI.accNo = result.ACC_NO
                payForUI.accBal = result.REMAIN_BAL
                result.ACC_LIST.forEach {
                    val acclist = ACCLIST().apply {
                        ACC_NO = it.ACC_NO
                        ACC_BAL = it.ACC_BAL
                        ACC_TYPE = it.ACC_TYPE
                        TRAN_ID = it.TRAN_ID
                        PAYMENT = it.PAYMENT
                    }
                    payForUI.accList.add(acclist)
                }
                payForUI.actualPayment = result.ACTUAL_PAYMENT
                payForUI.orderId = result.ORDERID
                payForUI.traceId = result.TRACEID
                payForUI.errCode = result.ERRCODE
                payForUI.errMsg = result.ERRMSG

                //sw
                payForUI.swForUI.result = result.RESULT
                payForUI.swForUI.balance = result.REMAIN_BAL

            } else {
                payForUI.errCode = response.code
                payForUI.errMsg = response.msg
            }
//            if (payForUI.result == "Y") saveOrderRecord(payForUI, 1)
            // 存储订单
            LogUtil.d(TAG, Gson().toJson(payForUI))
            listener?.onOtherListener(3, payForUI)
        }
    }

    private fun onLinePay(payForUI: PayForUI) {
        runBlocking(mHandler) {
            LogUtil.d(TAG, Gson().toJson(payForUI))
            val response = when (payForUI.payType) {
                "2" -> {
                    val request = Gson().fromJson(Gson().toJson(payForUI), CodePayBean::class.java)
                    request.qrCode = payForUI.payContent
                    val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                    mRespository.payByQrCode(encryption)
                }
                "3" -> {
                    val request = Gson().fromJson(Gson().toJson(payForUI), CardPayBean::class.java)
                    request.cardId = payForUI.payContent
                    LogUtil.i(TAG, "request: $request")
                    val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                    mRespository.payByIcCard(encryption)
                }
                else -> CanteenResponse<String>()
            }
            if (response.code == "200") {
                val decryptStr = DES3CBCUtil.decryptRSA(response.data ?: "")
                val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                LogUtil.d(TAG, "支付结果:${Gson().toJson(result)}")
                payForUI.result = result.RESULT
                payForUI.accType = result.ACC_TYPE
                payForUI.accNo = result.ACC_NO
                payForUI.accBal = result.ACC_BAL
                result.ACC_LIST.forEach {
                    val acclist = ACCLIST().apply {
                        ACC_NO = it.ACC_NO
                        ACC_BAL = it.ACC_BAL
                        ACC_TYPE = it.ACC_TYPE
                        TRAN_ID = it.TRAN_ID
                        PAYMENT = it.PAYMENT
                    }
                    payForUI.accList.add(acclist)
                }
                payForUI.actualPayment = result.ACTUAL_PAYMENT
                payForUI.orderId = result.ORDERID
                payForUI.traceId = result.TRACEID
                payForUI.errCode = result.ERRCODE
                payForUI.errMsg = result.ERRMSG
            } else {
                payForUI.errCode = response.code
                payForUI.errMsg = response.msg
            }
            if (payForUI.result == "Y") saveOrderRecord(payForUI, 1)
            LogUtil.d(TAG, Gson().toJson(payForUI))
            listener?.onOtherListener(3, payForUI)
        }
    }

    private fun offLinePay(payForUI: PayForUI) {
        val offlineOrder = Gson().fromJson(Gson().toJson(payForUI), OfflineOrderTable::class.java)
        dbHelper.insertOfflineOrder(offlineOrder)
        val order = dbHelper.queryOfflineOrder(offlineOrder.sessionId)
        offlineOrder.paymentDishes.forEach {
            it.offlineOrderTable = order
            dbHelper.insertOfflineDish(it)
        }
        offlineOrder.accList.forEach {
            it.offlineOrderTable = order
            dbHelper.insertOfflineAccList(it)
        }
        payForUI.result = "Y"
        payForUI.orderId = payForUI.sessionId
        saveOrderRecord(payForUI, 0)
        listener?.onOtherListener(3, payForUI)
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

    public fun saveSwOrderRecord(payForUI: PayForUI, flag: Int) {
        LogUtil.i(TAG, "saveSwOrderRecord payOrder: $payForUI")
        val payOrder = Gson().fromJson(Gson().toJson(payForUI), SwPayOrderTable::class.java)
        val swForUI = payForUI.swForUI
        payOrder.apply {
            tranResult = "3" //1：待支付，2：支付失败，3：支付成功
            this.flag = flag
            //sw
            actualMealId = swForUI.actualMealId
            actualMealName = swForUI.actualMealName

            meal01Time = swForUI.meal01Time
            meal02Time = swForUI.meal02Time
            meal03Time = swForUI.meal03Time
            meal04Time = swForUI.meal04Time

            standardMealId = swForUI.standardMealId
            standardMealName = swForUI.standardMealName
            standardName = swForUI.useMealRuleName
            standardNum = swForUI.everyUseTime
            ruleRestTime = swForUI.restTime - swForUI.everyUseTime.trim().toInt()
            rulePrice = swForUI.rulePrice
            subsidyMoney = swForUI.subsidy
        }

        dbHelper.insertSwPayOrder(payOrder)
        val order = dbHelper.querySwPayOrder(payOrder.orderId)
        payForUI.accList.forEach {
            val acc = Gson().fromJson(Gson().toJson(it), AccListTable::class.java)
            acc.swPayOrderTable = order
            dbHelper.insertAccList(acc)
        }
//        saveOrderRecord(payForUI, 1)
    }
}