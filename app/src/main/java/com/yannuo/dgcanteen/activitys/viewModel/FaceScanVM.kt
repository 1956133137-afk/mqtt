package com.yannuo.dgcanteen.activitys.viewModel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.PayDishTable
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/11 14:26
 **/
class FaceScanVM {
    private val TAG = javaClass.simpleName
    private val mContext = MyApplication.applicationContext
    private val mmkv: MMKV = MMKV.defaultMMKV()
    private var mPayCfg: PayCfg = PayCfg()
    private val dbHelper = DishesDBHelper.getInstance()
    private val mRespository by lazy { PayRepositoryOfPay() }
    private var mFacePayService: ZHSTFacePayService? = null
    private var listener: FaceResultListener? = null
    private var swListener: SwFaceResultListener? = null
    private val serviceConnection by lazy { MyServiceConnection() }
    private val resultListener by lazy { OnPayResultListener() }
    private val ccbFacePayBean = CcbFacePayBean()
    private var modeStatus: Boolean = false
    private var currentOffline: String = ""
    private val dishList: MutableList<Dish> = mutableListOf()

    // 放在重复调用
    private val mutex = Mutex()
    private var isFaceStatus = false

    private var mHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, e ->
        e.printStackTrace()
        LogUtil.e(TAG, "Exception: ${e.message}")
    }

    private val mScope = CoroutineScope(Dispatchers.IO + mHandler)

    companion object {
        val instance: FaceScanVM by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(FaceScanVM::class.java) { FaceScanVM() }
        }
    }

    fun setFaceListener(listener: FaceResultListener?) {
        this.listener = listener
    }

    fun setSwFaceListener(swListener: SwFaceResultListener?) {
        this.swListener = swListener
    }

    fun setTimeOut(timeout: Int) {
        mFacePayService?.setTimeOut(timeout)
    }

    fun setOrderDishList(beanList: MutableList<DishBean>) {
        dishList.clear()
        beanList.forEach {
            val dish = Dish().apply {
                dishesId = it.dishId
                dishesName = it.dishName
                dishesPrice = it.dishPrice
                dishesNumber = it.dishCount.toString()
            }
            dishList.add(dish)
        }
    }

    fun startFacePay(status: Boolean, payment: String = "", orderId: String = "", verifyFlag: String = "") {
        if (isFaceStatus) return
        runBlocking { mutex.withLock { isFaceStatus = true } }
        mPayCfg = mmkv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
        modeStatus = status
        currentOffline = if (mmkv.decodeBool(Constant.SWITCH)) "1" else "0"
        ccbFacePayBean.apply {
            CAMPUS_ID = mPayCfg.campusId
            CORP_ID = mPayCfg.corpId        // "1046"
            PAYMENT = payment
            ORDER_ID = orderId.ifEmpty { "${mPayCfg.counterId}${System.currentTimeMillis()}" }
            BUSINESS_ID = mPayCfg.businessId // "SJ2022022500004"
            VPOS_ID = mPayCfg.counterId      // "V00023523"
            REMARK = verifyFlag
            OFFLINE = currentOffline
        }
        // 常亮模式
        when {
            mmkv.decodeString(Constant.APP_MODE) == Constant.PROCEEDS_MODE && mmkv.decodeBool(Constant.AUTO_VERIFY, false) -> {
                mFacePayService?.setTimeOut(0)  // 常亮核销
            }
            mmkv.decodeString(Constant.APP_MODE) == Constant.PROCEEDS_MODE && mmkv.decodeBool(Constant.AUTO_PAY, false) -> {
                mFacePayService?.setTimeOut(0)  // 常亮收款
            }
            else -> mFacePayService?.setTimeOut(30000)
        }
        mFacePayService?.startFacePay(if (modeStatus) "" else Gson().toJson(ccbFacePayBean), currentOffline, resultListener)
    }

    fun bindService() {
        if (mFacePayService != null) return
        val serviceIntent = Intent()
        serviceIntent.action = "com.ccb.smartcanteen.FacePayService"
        serviceIntent.setPackage("com.ccb.smartcanteen")
        mContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun stopScanFace() {
        mFacePayService?.stopFacePay()
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
            mScope.launch {
                delay(100)
                mutex.withLock { isFaceStatus = false }
                LogUtil.d(TAG, result)
                val responseStr = result.replace("\"[", "[").replace("]\"", "]")
                val bean = Gson().fromJson(responseStr, CcbFacePayResultBean::class.java)
                if (modeStatus) listener?.onFaceQuery(bean) else facePay(bean)
            }
//            runBlocking { mutex.withLock { isFaceStatus = false } }
//            LogUtil.d(TAG, result)
//            val responseStr = result.replace("\"[", "[").replace("]\"", "]")
//            val bean = Gson().fromJson(responseStr, CcbFacePayResultBean::class.java)
//            if (modeStatus) listener?.onFaceQuery(bean) else facePay(bean)
        }
    }

    private fun facePay(bean: CcbFacePayResultBean) {
        val currentTime = System.currentTimeMillis()
        val payForUI = PayForUI().apply {
            businessId = mPayCfg.businessId
            businessName = mPayCfg.businessName
            campusId = mPayCfg.campusId
            corpId = mPayCfg.corpId
            vposId = mPayCfg.counterId
            deviceId = CommonAndDpToPxUtil.getDeviceSerial()
            payType = "1"
            payment = bean.PAYMENT.ifEmpty { ccbFacePayBean.PAYMENT }
            discountAmt = bean.DISCOUNTAMT
            discountMsg = bean.DISCOUNTMSG
            orderId = bean.ORDER_ID.ifEmpty { ccbFacePayBean.ORDER_ID }
            payTime = bean.PAYTIME.ifEmpty { TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", currentTime) }
            payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
            sessionId = "${CommonAndDpToPxUtil.getDeviceSerial()}$currentTime${Random().nextInt(10)}"
            signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
            offline = bean.OFFLINE.ifEmpty { currentOffline }
            paymentDishes.addAll(dishList)
        }
        dishList.clear()
        payForUI.apply {
            result = bean.RESULT
            username = bean.CUST_NAME
            custId = bean.CUST_ID
            actualPayment = bean.ACTUAL_PAYMENT  //非离线用实际支付值
            accType = bean.ACC_TYPE
            accNo = bean.ACC_NO
            accBal = bean.REMAIN_BAL.ifEmpty { bean.ACC_BAL }
            traceId = bean.TRACEID
            errCode = bean.ERRCODE
            errMsg = bean.ERRMSG
        }
        var remainBal = 0F
        bean.ACC_LIST.forEach {
            remainBal += it.ACC_BAL.ifEmpty { "0" }.toFloat()
            val acclist = ACCLIST().apply {
                ACC_NO = it.ACC_NO
                ACC_BAL = it.ACC_BAL
                ACC_TYPE = it.ACC_TYPE
                TRAN_ID = it.TRAN_ID
                PAYMENT = it.PAYMENT
            }
            payForUI.accList.add(acclist)
        }
        if (payForUI.accBal.isEmpty()) payForUI.accBal = String.format("%.02f", remainBal)
        LogUtil.d(TAG, Gson().toJson(payForUI))
        /*保存记录*/
        if (bean.RESULT == "Y") saveOrSynOrder(payForUI, bean.TRAN_RESULT)
        else when (bean.ERRMSG) {
            "活体检测超时", "活体检测取消", "支付取消", "识别失败，请重试或更新人脸信息。", "1:N人脸库识别失败！提取人脸特征值失败，人脸检测不合格2" -> {}
            else -> saveOrSynOrder(payForUI, bean.TRAN_RESULT)
        }
        listener?.onFacePay(payForUI)
    }

    private fun saveOrSynOrder(payForUI: PayForUI, tranResult: String) {
        mScope.launch {
            //保存记录
            val payOrder = Gson().fromJson(Gson().toJson(payForUI), PayOrderTable::class.java)
            payOrder.tranResult = tranResult.ifEmpty { "2" } //1：待支付，2：支付失败，3：支付成功
            dbHelper.insertPayOrder(payOrder)
            val order = dbHelper.queryPayOrder(payOrder.orderId)
            payForUI.paymentDishes.forEach {
                val dish = Gson().fromJson(Gson().toJson(it), PayDishTable::class.java)
                dish.payOrderTable = order
                dbHelper.insertPayDish(dish)
            }
            //上传记录
            val bean = SynConsumeRecordBean().apply {
                deviceSerialNumber = order.deviceId
                businessId = order.businessId
                campusId = order.campusId
                counterId = order.vposId
                consumptionType = order.payType
                RESULT = order.result
                CUST_ID = order.custId
                PAYMENT = order.payment
                ACTUAL_PAYMENT = order.actualPayment ?: "0.0"
                ACC_NO = order.accNo
                ACC_BAL = order.accBal
                ACC_TYPE = order.accType
                TRACEID = order.traceId
                ORDER_ID = order.orderId
                TRAN_RESULT = order.tranResult
                OFFLINE = order.offline
                ERRCODE = order.errCode
                ERRMSG = order.errMsg
                order.accList.forEach {
                    val acclist = ACCLIST().apply {
                        ACC_NO = it.acC_NO
                        ACC_BAL = it.acC_BAL
                        ACC_TYPE = it.acC_TYPE
                        TRAN_ID = it.traN_ID
                        PAYMENT = it.payment
                    }
                    ACC_LIST.add(acclist)
                }
                PAYTIME = order.payTime
                BUSINESS_NAME = order.businessName
            }
            payForUI.paymentDishes.forEach { bean.paymentDishesList.add(it) }
            LogUtil.d(TAG, Gson().toJson(bean))
            if (payForUI.offline == "0") {
                val res = mRespository.synCsRecord(bean)
                if (res.code == "200") {
                    order.flag = 1
                    if (payForUI.isSw == 1) {
                        // 回调sw餐次界面
                        swListener?.swOnFacePay(payForUI, "订单${bean.ORDER_ID} 上传成功!")
                    } else dbHelper.updatePayOrder(order)
                    LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
                } else {
                    if (payForUI.isSw == 1) {
                        // 回调sw餐次界面
                        swListener?.swOnFacePay(payForUI, "上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                    }
                    LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                }
            }
        }
    }

    fun clear() {
        mFacePayService = null
        mScope.cancel()
    }

    interface FaceResultListener {
        fun onFacePay(payForUI: PayForUI)
        fun onFaceQuery(bean: CcbFacePayResultBean)
    }

    interface SwFaceResultListener {
        fun swOnFacePay(payForUI: PayForUI, msg: String)
//        fun swOnFaceQuery(bean: CcbFacePayResultBean)
    }
}