package com.yannuo.dgcanteen.activitys.presenters

import android.os.RemoteException
import android.text.format.DateFormat
import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.dao.CardPay
import com.yannuo.dgcanteen.dao.OffLineDishTable
import com.yannuo.dgcanteen.dao.OffLineTable
import com.yannuo.dgcanteen.dao.OwnOrder
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/8/3 16:36
 **/
class CodePresenter : ScanDevice.DataCallBack {
    private val TAG = javaClass.simpleName

    private lateinit var kv: MMKV
    private lateinit var mPayCfg: PayCfg
    private lateinit var mRespository: PayRepositoryOfPay
    private lateinit var mCodeDevice: ScanDevice
    var listener: CallbackListener? = null
    private var codeStatus = CodeStatus.INVALID
    private var payAmount = 0.0F
    private var validCode = 0       //支付状况
    private var plainText = ""       //离线码明文
    private var custIdOnLine: String? = null   //在线码

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception $throwable ${throwable.message}")
        listener?.onOtherListener(2, "${throwable.message}") //返回异常信息
    }

    //支付状态
    enum class CodeStatus {
        INVALID, PAY
    }

    //初始化什么
    fun initCode() {
        kv = MMKV.defaultMMKV()
        mCodeDevice = ScanDevice()
        mRespository = PayRepositoryOfPay()
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)!!
    }

    //打开扫码
    fun openQrCode(payment: Float) {
        payAmount = payment
        codeStatus = CodeStatus.PAY
        validCode = 3
        plainText = ""
        mCodeDevice.openScan()
        mCodeDevice.setCallbackListener(this)
    }

    //修改支付状态
    fun setPayStatus() {
        codeStatus = CodeStatus.PAY
    }

    //关闭扫码
    fun closeQrCode() {
        if (this::mCodeDevice.isInitialized) {
            mCodeDevice.setCallbackListener(null)
            mCodeDevice.closeScan()
        }
    }

    //扫码数据回调
    override fun onData(data: String) {
        if (codeStatus == CodeStatus.INVALID) return
        codeStatus = CodeStatus.INVALID
        val qrData = data.trim().replace("\r\n", "")
        Log.d(TAG, "QrCode: $qrData")
        qrCodeMsgHandler(data)
    }

    //
    private fun qrCodeMsgHandler(qrCode: String) {
        listener?.onOtherListener(1)
        runBlocking(Dispatchers.IO + mHandler) {
            if (qrCode.contains("CCB")) {
                plainText = DES3CBCUtil.transDecryption(qrCode)
                val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
                val hour = TimeUtil.timestamp(pastDueTime)
                if (hour < 1) {
                    listener?.onOtherListener(5, 1)
                    return@runBlocking
                }
            }
            val payTime = DateFormat.format("yyyyMMddHHmmss", System.currentTimeMillis()).toString()
            if (kv.decodeBool(Constant.SWITCH)) { //离线模式
                if (qrCode.contains("CCB")) validCode = 1
                else {
                    listener?.onOtherListener(5, 0)
                    return@runBlocking
                }
            } else { //在线模式
                val bean = SpendLimitBean().apply {
                    campusId = mPayCfg.campusId
                    businessId = mPayCfg.businessId
                    vposId = mPayCfg.counterId
                    payment = payAmount.toString()
                }
                if (qrCode.contains("CCB")) {
                    val cidNo = plainText.substring(0, plainText.indexOf("@"))
                    bean.cidNumber = cidNo
                    bean.custId = cidNo
                } else {
                    val map = mutableMapOf<String, String>()
                    map["CAMPUS_ID"] = mPayCfg.campusId.toString()
                    map["TXCODE"] = "PAY002"
                    map["CORP_ID"] = mPayCfg.corp_id.toString()
                    map["QR_CODE"] = qrCode
                    val result = CanteenEncryptionUtil.getAnalysisQr(map)
                    val res = mRespository.getCcbData(result).body()?.let { //解析二维码
                        Gson().fromJson(
                            it.string().replace("\r\n", ""),
                            ScanAnalysisBean::class.java
                        )
                    }
                    if (res?.RESULT.toString() == "Y") {
                        bean.cidNumber = res?.CID_NO ?: ""
                        bean.custId = res?.CUST_ID ?: ""
                        custIdOnLine = bean.custId
                    } else { //二维码解析失败
                        val payForUI = PayResultForUI().apply {
                            this.result = PayResultForUI.Result.FAIL
                            way = "被扫支付"
                            orderid = NumberGenerateUtil.getOrderNumber()
                            timestamp = payTime
                            cust_name = "***"
                            payment = payAmount.toString()
                            errormsg = "error ${res?.ERRCODE} ${res?.ERRMSG} "
                        }
                        listener?.onOtherListener(3, payForUI)
                        return@runBlocking
                    }
                }
                LogUtil.d(TAG, Gson().toJson(bean))
                val res = mRespository.getConsumeStatus(bean)
                LogUtil.d(TAG, Gson().toJson(res))
                if (res.data?.limit == true) { //是否受限
                    val payResult = PayResultForUI().apply {
                        way = "被扫支付"
                        cust_name = custIdOnLine
                        payment = payAmount.toString()
                        this.result = PayResultForUI.Result.FAIL
                        timestamp = payTime
                        errormsg = if (res.data?.type == 0) {
                            "error 支付次数受限 "
                        }else{
                            "error 支付金额受限 "
                        }
                    }
                    listener?.onOtherListener(3, payResult)
                    return@runBlocking
                }
            }
            startPayWithScan(qrCode, payTime)
        }
    }

    private fun startPayWithScan(qrcode: String, payTime: String) {
        var responseScanPay: ScanQrResultBean? = null
        val ccbBean = OffLineTable().apply {
            campuS_ID = mPayCfg.campusId
            corP_ID = mPayCfg.corp_id
            txcode = "PAY003"
            ccbSafeParam = ""
            businesS_ID = mPayCfg.businessId
            vpoS_ID = mPayCfg.counterId
            payment = payAmount.toString()
            actuaL_PAYMENT = payAmount.toString()
            coupoN_INFO = ""
            acC_NOS = ""
            qR_CODE = "" //二维码
            cusT_ID = "" //用户ID
            ordeR_ID = NumberGenerateUtil.getOrderNumber()
            offline = "0" //是否为离线码
            sigN_TIME = payTime
        }

        when (qrcode.contains("CCB")) { //离线码
            true -> {
                if (validCode == 3) {
                    ccbBean.offline = "1"
                    ccbBean.qR_CODE = qrcode
                    runBlocking(Dispatchers.IO + mHandler) {
                        var map = CanteenEncryptionUtil.getScanToPay(ccbBean)
                        responseScanPay = mRespository.getCcbData(map).body()?.let { //扫码支付
                            Gson().fromJson(
                                it.string().replace("\r\n", ""),
                                ScanQrResultBean::class.java
                            )
                        }
                        if (responseScanPay?.RESULT.toString() == "Y") {
                            ccbBean.txcode = "PAY006"
                            map = CanteenEncryptionUtil.getQueryRecord(ccbBean)
                            val payResult = mRespository.getCcbData(map).body()?.let { //查询记录
                                Gson().fromJson(
                                    it.string().replace("\r\n", ""),
                                    ScanQueryBean::class.java
                                )
                            }
                            ccbBean.cusT_ID = payResult?.CUST_ID.toString()
                        } else {
                            validCode = 2
                        }
                    }
                }
            }
            else -> {
                if (validCode == 3) {
                    ccbBean.offline = "0"
                    ccbBean.qR_CODE = qrcode
                    ccbBean.cusT_ID = custIdOnLine
                    val map = CanteenEncryptionUtil.getScanToPay(ccbBean)
                    runBlocking(Dispatchers.IO + mHandler) {
                        responseScanPay =
                            mRespository.getCcbData(map).body()?.let { //扫码支付
                                Gson().fromJson(
                                    it.string().replace("\r\n", ""),
                                    ScanQrResultBean::class.java
                                )
                            }
                        if (responseScanPay?.RESULT.toString() == "N") {
                            validCode = 2
                        }
                    }
                }
            }
        }

        when (validCode) {
            3 -> { //支付成功
                runBlocking(Dispatchers.IO + mHandler) {
                    responseScanPay?.let { consumeRecord(ccbBean, it) }
                }
                listener?.onOtherListener(3, resForUI("被扫支付", ccbBean, responseScanPay))
            }
            2 -> { //支付失败
                listener?.onOtherListener(3, resForUI("被扫支付", ccbBean, responseScanPay))
            }
            1 -> { //待支付

                ccbBean.decryptionCode = plainText
                ccbBean.postTag = false
                DishesDBHelper.getInstance().insertOffLineOrder(ccbBean)

                LogUtil.d(TAG, "离线订单已保存")
                listener?.onOtherListener(4, resForUI("被扫支付", ccbBean, responseScanPay))
            }
        }
    }

    private fun resForUI(type: String, data: OffLineTable, res: ScanQrResultBean?): PayResultForUI {
        val persons = DishesDBHelper.getInstance().queryPersonToCustId(data.cusT_ID)
        val payState = PayResultForUI()
        payState.way = type
        payState.orderid = data.ordeR_ID
        payState.custId = data.cusT_ID
        payState.timestamp = data.sigN_TIME
        payState.cust_name = persons?.personName ?: "***"
        payState.payment = data.payment
        if (res == null || res.RESULT.toString() == "Y") {
            payState.result = PayResultForUI.Result.SUCCESS
            payState.payment = res?.ACTUAL_PAYMENT ?: data.payment
            payState.acc_no = res?.ACC_NO ?: ""
            payState.acc_bal = res?.ACC_BAL ?: ""
        } else {
            payState.result = PayResultForUI.Result.FAIL
            payState.traceid = res.TRACEID
            payState.errormsg = "error ${res.ERRCODE} ${res.ERRMSG} "
        }
        return payState
    }

    //同步刷卡消费记录
    private suspend fun consumeRecord(data: OffLineTable, res: ScanQrResultBean) {
        val bean = SynConsumeRecordBean()
        bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
        bean.businessId = data.businesS_ID
        bean.counterId = data.vpoS_ID
        bean.consumptionType = 2
        bean.RESULT = res.RESULT.toString()
        bean.CUST_ID = data.cusT_ID
        bean.PAYMENT = res.PAYMENT?.toDouble()
        bean.ACTUAL_PAYMENT = res.ACTUAL_PAYMENT?.toDouble()
        bean.ACC_NO = res.ACC_NO
        bean.ACC_BAL = res.ACC_BAL?.toDouble()
        bean.ACC_TYPE = res.ACC_TYPE?.toInt()
        bean.TRACEID = ""
        bean.ORDER_ID = data.ordeR_ID
        bean.TRAN_RESULT = when (res.RESULT.toString()) {
            "Y" -> 3
            "N" -> 2
            else -> null
        }
        bean.OFFLINE = data.offline.toInt()
        bean.ERRCODE = res.ERRCODE
        bean.ERRMSG = res.ERRMSG
        bean.ACCALIAS = when (bean.ACC_TYPE) {
            1 -> "现金账号"
            2 -> "餐补账户"
            else -> ""
        }
        bean.PAYTIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        bean.BUSINESS_NAME = "彦诺智能测试园区"

        val responseScanPay = mRespository.synCsRecord(bean)
        LogUtil.i(TAG, Gson().toJson(responseScanPay))
        if (responseScanPay.code != HttpURLConnection.HTTP_OK) {
            saveFailureRecord(bean)
            LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${responseScanPay.data}")
        } else {
            LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
        }

    }

    //保存上传失败记录
    private fun saveFailureRecord(bean: SynConsumeRecordBean) {
        val saveOrder = OwnOrder()
        bean.apply {
            saveOrder.deviceSerialNumber = deviceSerialNumber
            saveOrder.businessId = businessId
            saveOrder.counterId = counterId
            saveOrder.consumptionType = consumptionType
            saveOrder.result = RESULT
            saveOrder.cusT_ID = CUST_ID
            saveOrder.payment = PAYMENT ?: 0.0

            saveOrder.actuaL_PAYMENT = ACTUAL_PAYMENT ?: 0.0
            saveOrder.acC_NO = ACC_NO
            saveOrder.acC_BAL = ACC_BAL ?: 0.0
            saveOrder.acC_TYPE = ACC_TYPE ?: 1
            saveOrder.traceid = TRACEID
            saveOrder.ordeR_ID = ORDER_ID
            saveOrder.traN_RESULT = TRAN_RESULT ?: 3
            saveOrder.offline = OFFLINE
            saveOrder.errcode = ERRCODE
            saveOrder.errmsg = ERRMSG
            saveOrder.accalias = ACCALIAS
            saveOrder.paytime = PAYTIME
            saveOrder.businesS_NAME = BUSINESS_NAME
        }
        DishesDBHelper.getInstance().insertConsumerOrder(saveOrder)
    }
}