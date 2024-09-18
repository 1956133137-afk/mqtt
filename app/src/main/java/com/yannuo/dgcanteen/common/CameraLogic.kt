package com.yannuo.dgcanteen.common

import android.os.RemoteException
import android.text.TextUtils
import android.text.format.DateFormat
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.PayDishTable
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import kotlinx.coroutines.*
import java.util.*

class CameraLogic {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val mPayCfg: PayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    private val mRespository: PayRepositoryOfPay = PayRepositoryOfPay()
    private lateinit var mScope: CoroutineScope

    init {
        val job = SupervisorJob()
        mScope = CoroutineScope(Dispatchers.IO + job)
    }

    /**
     * 发起人脸支付
     * @param service ZHSTFacePayService?
     * @param productsDetail ProductsDetail
     * @throws RemoteException
     */
    fun startPayWithFace(service: ZHSTFacePayService?, amount: Float, listener: CallbackListener?) {
        mScope.launch() {
            if (TextUtils.isEmpty(mPayCfg.campusId) || TextUtils.isEmpty(mPayCfg.businessId) || TextUtils.isEmpty(mPayCfg.counterId)) {
                LogUtil.e(TAG, "未配置支付环境")
                val err = PayForUI()
                err.payType = "1"
                err.errMsg = "未配置支付环境"
                err.payTime = DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString()
                listener?.onOtherListener(3 ,err)
                return@launch
            }

            val offline = if (kv.decodeBool(Constant.SWITCH)) 1 else 0  //在线

            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = mPayCfg.campusId
            bean.CORP_ID = mPayCfg.corp_id
            bean.PAYMENT = String.format("%.2f", amount)
            bean.BUSINESS_ID = mPayCfg.businessId
            bean.VPOS_ID = mPayCfg.counterId
            bean.OFFLINE = offline.toString()

            service!!.startFacePay(Gson().toJson(bean), bean.OFFLINE, object : PayResultListener.Stub() {
                override fun onResult(result: String) {
                    LogUtil.d(TAG, result)
                    val currentTime = System.currentTimeMillis()
                    val payResult = Gson().fromJson(result, CcbFacePayResultBean::class.java)
                    val payForUI = PayForUI().apply {
                        businessId = mPayCfg.businessId
                        businessName = mPayCfg.businessName
                        campusId = mPayCfg.campusId
                        corpId = mPayCfg.corp_id
                        vposId = mPayCfg.counterId
                        deviceId = CommonAndDpToPxUtil.getDeviceSerial()
                        payType = "1"
                        payment = payResult.PAYMENT
                        orderId = payResult.ORDER_ID
                        payTime = payResult.PAYTIME
                        payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
                        sessionId = "${CommonAndDpToPxUtil.getDeviceSerial()}$currentTime${Random().nextInt(10)}"
                        signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
                        this.offline = offline.toString()
                    }
                    when (payResult.RESULT) {
                        "Y" -> { //订单状态,成功
                            payForUI.username = payResult.CUST_NAME
                            payForUI.custId = payResult.CUST_ID
                            if (offline == 0) payForUI.actualPayment = payResult.ACTUAL_PAYMENT  //非离线用实际支付值
                            payForUI.accType = payResult.ACC_TYPE
                            payResult.ACC_LIST.forEach {
                                val acclist = ACCLIST().apply {
                                    ACC_NO = it.ACC_NO
                                    ACC_BAL = it.ACC_BAL
                                    ACC_TYPE = it.ACC_TYPE
                                    TRAN_ID = it.TRAN_ID
                                    PAYMENT = it.PAYMENT
                                }
                                payForUI.accList.add(acclist)
                            }
                            //检查支付结果，
                            when (payResult.TRAN_RESULT) {
                                "3" -> {  //3支付成功
                                    payForUI.result = "Y"
                                    payForUI.traceId = payResult.TRACEID
                                    saveOrSynOrder(payForUI)
                                }
                                else -> { //1 -待支付、2-支付失败
                                    payForUI.errCode = payResult.ERRCODE
                                    payForUI.errMsg = payResult.ERRMSG
                                }
                            }
                        }
                        else -> { //订单状态,失败
                            payForUI.errCode = payResult.ERRCODE
                            payForUI.errMsg = payResult.ERRMSG
                        }
                    }
                    listener?.onOtherListener(3 ,payForUI)
                }
            })
        }

    }

    /**
     * 保存或同步消费记录,离线模式将直接保存，在线模式上传失败也会保存
     */
    private fun saveOrSynOrder(payForUI: PayForUI) {
        mScope.launch(Dispatchers.IO) {
            //保存记录
            val payOrder = Gson().fromJson(Gson().toJson(payForUI), PayOrderTable::class.java)
            payOrder.tranResult = "3" //1：待支付，2：支付失败，3：支付成功
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
                ERRCODE = ""
                ERRMSG = ""
                order.accList.forEach {
                    val acclist = ACCLIST().apply {
                        ACC_NO = it.acC_NO
                        ACC_BAL = it.acC_BAL
                        ACC_TYPE = it.acC_TYPE
                        TRAN_ID = it.traN_ID
                        PAYMENT = it.payment
                    }
                    ACCALIAS.add(acclist)
                }
                PAYTIME = order.payTime
                BUSINESS_NAME = order.businessName
            }
            payForUI.paymentDishes.forEach { bean.paymentDishesList.add(it) }
            if (payForUI.offline == "0") {
                val res = mRespository.synCsRecord(bean)
                if (res.code == "200") {
                    order.flag = 1
                    dbHelper.updatePayOrder(order)
                    LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
                } else LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
            }
        }
    }
}