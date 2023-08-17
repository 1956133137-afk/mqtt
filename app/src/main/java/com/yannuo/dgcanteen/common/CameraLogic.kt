package com.yannuo.dgcanteen.common

import android.os.RemoteException
import android.text.TextUtils
import android.text.format.DateFormat
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.OwnOrder
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.*
import java.net.HttpURLConnection

class CameraLogic {
    private lateinit var mScope: CoroutineScope
    private val TAG = javaClass.simpleName
    private lateinit var kv : MMKV
    private var mPayCfg : PayCfg ?= null
    private lateinit var mRespository : PayRepositoryOfPay

    init {
        kv = MMKV.defaultMMKV()
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)

        val job = SupervisorJob()
        mScope = CoroutineScope(Dispatchers.IO + job)
        mRespository =  PayRepositoryOfPay()
    }


    /**
     * 发起人脸支付
     * @param service ZHSTFacePayService?
     * @param productsDetail ProductsDetail
     * @throws RemoteException
     */
    fun startPayWithFace(service: ZHSTFacePayService?, amount: Float, listener : IProductsVM?) {
        mScope.launch() {
            if (mPayCfg == null || TextUtils.isEmpty(mPayCfg!!.campusId) ||
                TextUtils.isEmpty(mPayCfg?.businessId) || TextUtils.isEmpty(mPayCfg?.counterId)) {
                LogUtil.e(TAG, "未配置支付环境")
                val err = PayResultForUI()
                err.way = "人脸支付"
                err.errormsg = "未配置支付环境"
                err.timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss",System.currentTimeMillis()).toString()
                listener?.onFacePayResult(err)
                return@launch
            }

            var offline = 0  //在线
            if (kv.decodeBool(Constant.SWITCH)) offline = 1  //离线

            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = mPayCfg!!.campusId.toString()
            bean.CORP_ID = mPayCfg!!.corp_id.toString()
            bean.PAYMENT = String.format("%.2f",amount)
            bean.BUSINESS_ID = mPayCfg!!.businessId.toString()
            bean.VPOS_ID = mPayCfg!!.counterId.toString()
            bean.OFFLINE = offline.toString()

            service!!.startFacePay(Gson().toJson(bean), bean.OFFLINE, object : PayResultListener.Stub() {
                override fun onResult(result: String) {
                        LogUtil.d(TAG, result)
                        val payResult = Gson().fromJson(result, CcbFacePayResultBean::class.java)
                        val payState = PayResultForUI()
                        payState.way = "人脸支付"
                        payState.orderid = payResult.ORDER_ID
                        payState.timestamp = payResult.PAYTIME
                        when (payResult.RESULT) {
                            "Y" -> { //订单状态,成功
                                payState.cust_name = payResult.CUST_NAME
                                payState.custId = payResult.CUST_ID
                                payState.payment = payResult.PAYMENT
                                if (offline == 0) payState.payment =
                                    payResult.ACTUAL_PAYMENT  //非离线用实际支付值
                                payState.acc_no = payResult.ACC_NO
                                payState.acc_bal = payResult.ACC_BAL
                                //检查支付结果，
                                when (payResult.TRAN_RESULT) {
                                    "3" -> {  //3支付成功
                                        payState.result = PayResultForUI.Result.SUCCESS
                                        payState.traceid = payResult.TRACEID
                                        saveOrSynConsumeRecord(payResult, payState, listener)
                                    }
                                    else -> { //1 -待支付、2-支付失败
                                        payState.errormsg = "error ${payResult.ERRCODE} ${payResult.ERRMSG} "
                                        listener?.onFacePayResult(payState)
                                    }
                                }
                            }
                            else -> { //订单状态,失败
                                payState.errormsg = "error ${payResult.ERRCODE} ${payResult.ERRMSG} "
                                payState.timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss",System.currentTimeMillis()).toString()
                                listener?.onFacePayResult(payState)
                            }
                        }
                }
            })
        }

    }

    /**
     * 保存或同步消费记录,离线模式将直接保存，在线模式上传失败也会保存
     */
    private fun saveOrSynConsumeRecord(
        payResult: CcbFacePayResultBean,
        payState: PayResultForUI,
        listener: IProductsVM?
    ) {
         mScope.launch( Dispatchers.IO) {
            val bean = SynConsumeRecordBean()
            bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
            bean.businessId = mPayCfg?.businessId
            bean.counterId = mPayCfg?.counterId
            bean.consumptionType = 1
            bean.RESULT  = "Y"
            bean.CUST_ID = payResult.CUST_ID
            bean.PAYMENT = payResult.PAYMENT!!.toDouble()

            bean.ACTUAL_PAYMENT = if (payResult.ACTUAL_PAYMENT.isNullOrEmpty().not()) payResult.ACTUAL_PAYMENT!!.toDouble()
            else 0.0
            bean.ACC_NO = payResult.ACC_NO

            bean.ACC_BAL =  if (payResult.ACC_BAL.isNullOrEmpty().not()) payResult.ACC_BAL!!.toDouble()
            else 0.0
            bean.ACC_TYPE =   if (payResult.ACC_TYPE.isNullOrEmpty().not()) payResult.ACC_TYPE!!.toInt()
            else 1
            bean.TRACEID = payResult.TRACEID
            bean.ORDER_ID = payResult.ORDER_ID
            bean.TRAN_RESULT =  3
            bean.OFFLINE = payResult.OFFLINE.toInt()
            bean.ERRCODE = ""
            bean.ERRMSG = ""
            bean.ACCALIAS =payResult.ACCALIAS

            bean.PAYTIME = payResult.PAYTIME
            bean.BUSINESS_NAME = "智慧食堂园区"

            var needSave = true
            if(bean.OFFLINE == 0){
                val res = mRespository.synCsRecord(bean)
                if (res.code == HttpURLConnection.HTTP_OK){
                    needSave = false
                    LogUtil.i(TAG,"订单${bean.ORDER_ID} 上传成功!")
                }else{
                    LogUtil.e(TAG,"上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                }
            }
            //上传成功直接返回
            if (needSave.not()) {
                listener?.onFacePayResult(payState)
                return@launch
            }
            val saveOrder =  OwnOrder()
            bean.apply {
                saveOrder.deviceSerialNumber = deviceSerialNumber
                saveOrder.businessId = businessId
                saveOrder.counterId = counterId
                saveOrder.consumptionType = consumptionType
                saveOrder.result = RESULT
                saveOrder.cusT_ID = CUST_ID
                saveOrder.payment = PAYMENT ?:0.0

//                saveOrder.actuaL_PAYMENT = ACTUAL_PAYMENT ?:0.0
                saveOrder.actuaL_PAYMENT = PAYMENT ?:0.0
                saveOrder.acC_NO = ACC_NO
                saveOrder.acC_BAL = ACC_BAL ?:0.0
                saveOrder.acC_TYPE = ACC_TYPE ?:1
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
            listener?.onFacePayResult(payState)
        }

    }
}