package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.NTScanHelp
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.AccListTable
import com.yannuo.dgcanteen.greendao.entity.OfflineOrderTable
import com.yannuo.dgcanteen.greendao.entity.PayDishTable
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.interfaces.ReadCardListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class PayViewModel : ViewModel(), ScanDevice.DataCallBack, OnReadDataListener {
    private val TAG = javaClass.simpleName

    private val mRespository: PayRepositoryOfPay = PayRepositoryOfPay()
    private val kv: MMKV = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val mPayCfg: PayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()

    private var mCardHandle: SerialPortHelper? = null
    private var mScanDevice: ScanDevice? = null
    private var ntHelp: NTScanHelp? = null

    var mDishes: ProductsDetail? = null
    var listener: CallbackListener? = null
    var mReadCardListener: ReadCardListener? = null
    private var payState = PayStatus.INVALID

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        listener?.onOtherListener(2, "发生异常信息")
    }

    //扫码状态，主要用于区分选择商品 和支付码
    enum class PayStatus {
        INVALID,    //无效状态
        PAY         //支付状态
    }

    fun setPayState(state: PayStatus) {
        payState = state
    }

    fun openPayStatus() {
        payState = PayStatus.PAY
        //打卡读卡器
        mCardHandle = SerialPortHelper()
        mCardHandle?.readDataListener = this
        mCardHandle?.openSerialPort("/dev/ttyS4")
//        mCardHandle?.openSerialPort("/dev/ttyXRUSB0")
        //打开扫码头
        mScanDevice = ScanDevice()
        mScanDevice?.setCallbackListener(this)
        mScanDevice?.openScan()
        ntHelp = NTScanHelp()
        ntHelp?.OpenScanCode(this, MyApplication.applicationContext)
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
        paymentLogicHelper("2", data)
    }

    //IC卡数据
    override fun numberOfIcCard(number: String?) {
        if (payState == PayStatus.INVALID || number == null) return
        payState = PayStatus.INVALID
        val icCard = number.trim().uppercase()
        LogUtil.d(TAG, "卡号 :$icCard")
        paymentLogicHelper("3", icCard)
    }

    /**
     * 支付处理
     * @param type 支付类型 1-刷脸 2-扫码 3-刷卡
     * @param content 支付内容 payType: 1-刷脸 2-二维码 3-卡号
     */
    private fun paymentLogicHelper(type: String, content: String) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val currentTime = System.currentTimeMillis()
            val payForUI = initData(type, content, currentTime)
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
    private fun initData(type: String, content: String, currentTime: Long): PayForUI {
        val deviceSerial = CommonAndDpToPxUtil.getDeviceSerial() ?: ""
        val payForUI = PayForUI().apply {
            businessId = mPayCfg.businessId
            businessName = mPayCfg.businessName
            campusId = mPayCfg.campusId
            corpId = mPayCfg.corp_id
            vposId = mPayCfg.counterId
            deviceId = deviceSerial
            payType = type
            payContent = content.replace("\n", "").replace("\r", "")
            payment = String.format("%.02f", (mDishes?.totalMoney ?: "0.00").toFloat())
            actualPayment = String.format("%.02f", (mDishes?.totalMoney ?: "0.00").toFloat())
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
                    onLinePay(payForUI)
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
                    if (payForUI.offline == "0") onLinePay(payForUI) else {
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
            else -> if (payForUI.offline == "0") onLinePay(payForUI) else listener?.onOtherListener(5, 0)
        }
    }

    private fun icCardHandler(payForUI: PayForUI) {
        var state = true
        val person = dbHelper.queryPersonToCardId(payForUI.payContent)
        if (person != null && person.custId.isNotEmpty()) {
            state = false
            payForUI.custId = person.custId
            payForUI.username = person.personName
            if (payForUI.offline == "0") onLinePay(payForUI) else offLinePay(payForUI)
        } else if (payForUI.offline == "0") {
            runBlocking(mHandler) {
                val result = mRespository.queryPersonByCardId(RequestPerson(payForUI.campusId, payForUI.payContent))
                if (result.code == "200") {
                    payForUI.custId = result.data?.custId ?: ""
                    payForUI.username = result.data?.personName ?: ""
                    if (payForUI.custId.isNotEmpty()) {
                        state = false
                        onLinePay(payForUI)
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

    private fun onLinePay(payForUI: PayForUI) {
        runBlocking(mHandler) {
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
                    val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                    mRespository.payByIcCard(encryption)
                }
                else -> CanteenResponse<String>()
            }
            if (response.code == "200") {
                val decryptStr = DES3CBCUtil.decryptRSA(response.data ?: "")
                val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
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
}