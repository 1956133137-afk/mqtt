package com.yannuo.dgcanteen.activitys.presenters

import android.os.RemoteException
import android.text.format.DateFormat
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.OffLineDishTable
import com.yannuo.dgcanteen.dao.OffLineTable
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class ScanPayPresenter(mDishes : ProductsDetail) : ScanDevice.DataCallBack {
     private val TAG = javaClass.simpleName
     private var mDishes : ProductsDetail ?= null
     private var mRespository :PayRepositoryOfPay
     var listener : CallbackListener?= null

     init {
          this.mDishes = mDishes
          mRespository = PayRepositoryOfPay()
          //开始监听扫码数据
          ScanDevice.setCallbackListener(this);
     }

     //扫码状态，主要用于区分选择商品 和支付码
     enum class ScanState{
          INVALID,  //扫码数据无效
          PAY        //支付状态
     }

     private var scanState = ScanState.INVALID  //状态码


     /**
      * 更新扫码的状态，用于被扫支付，根据支付状态
      * @param state ScanState
      */
     fun setScanState(state : ScanState){
          scanState = state
     }


     override fun onData(data: String) {
          when (scanState) {
               ScanState.PAY -> {
                    scanState = ScanState.INVALID //更新支付状态，以防止多次扫付款吗
                    mDishes?.also {
                         startPayWithScan(it,data)
                    }
               }
               ScanState.INVALID ->{
                    LogUtil.w(TAG,"扫码数据 INVALID !")
               }
          }
     }

     private fun initData(data :ProductsDetail):CcbScanPayBean{
          val ccbScanPayBean = CcbScanPayBean()
          ccbScanPayBean.CAMPUS_ID = "441999527"
          ccbScanPayBean.CORP_ID = "1041"
          ccbScanPayBean.TXCODE = "PAY003"
          ccbScanPayBean.ccbSafeParam = ""
          ccbScanPayBean.BUSINESS_ID = "SJ2023032511004"
          ccbScanPayBean.VPOS_ID = "V00463775"
          ccbScanPayBean.PAYMENT = data.totalMoney
          ccbScanPayBean.ACTUAL_PAYMENT = data.totalMoney
          ccbScanPayBean.COUPON_INFO = ""
          ccbScanPayBean.ACC_NOS = ""
          ccbScanPayBean.QR_CODE = "" //二维码
          ccbScanPayBean.CUST_ID = "" //用户ID
          ccbScanPayBean.ORDER_ID = TimeUtil.DateToTimestamp()
          ccbScanPayBean.OFFLINE = "0" //是否为离线码
          ccbScanPayBean.SIGN_TIME = DateFormat.format("yyyyMMddHHmmss",System.currentTimeMillis()).toString()
          return ccbScanPayBean
     }

     private fun startPayWithScan(data :ProductsDetail,qrcode :String){
          listener?.onOtherListener(1)
          var validCode = 3   //支付状况
          var offLineCode = 1
          var responseScanPay: ScanQrResultBean ?= null
          var res: ScanAnalysisBean ?= null
          val kv = MMKV.defaultMMKV()
          var plainText = ""
          val ccbBean = initData(data)

          if (kv.decodeBool(Constant.SWITCH)){ //离线模式
               if (qrcode.contains("CCB")) validCode = 1
               else {
                    validCode = 0
                    offLineCode = 0
               }
          }

          when(qrcode.contains("CCB")){ //离线码
               true -> {
                    ccbBean.QR_CODE = qrcode
                    ccbBean.OFFLINE = "1"
                    plainText = DES3CBCUtil.transDecryption(qrcode)
                    val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
                    val hour = TimeUtil.timestamp(pastDueTime)
                    if (hour < 1) validCode = 0

                    if (validCode == 3){
                         try {
                              runBlocking (Dispatchers.IO) {
                                   responseScanPay = mRespository.getScanQrData(ccbBean)
                                   if (responseScanPay!!.RESULT.toString() == "N"){
                                        validCode = 2
                                   }

                              }
                         }catch (e: RemoteException) {
                              e.printStackTrace()
                              listener?.onOtherListener(2,"网络请求失败")
                         }
                    }
               }
               else ->{
                    ccbBean.OFFLINE = "0"
                    try { //解析二维码
                         ccbBean.TXCODE = "PAY002"
                         ccbBean.ccbSafeParam = CanteenEncryptionUtil.encryption("QR_CODE=$qrcode")
                         runBlocking (Dispatchers.IO) {
                              res = mRespository.getQrData(ccbBean)
                         }
                         if (res?.RESULT.toString() == "Y"){
                              ccbBean.TXCODE = "PAY003"
                              ccbBean.QR_CODE = qrcode
                              ccbBean.CUST_ID = res?.CUST_ID.toString()

                              if (validCode == 3){
                                   try {
                                        runBlocking (Dispatchers.IO) {
                                             responseScanPay = mRespository.getScanQrData(ccbBean)
                                             if (responseScanPay!!.RESULT.toString() == "N"){
                                                  validCode = 2
                                             }
                                        }
                                   }catch (e: RemoteException) {
                                        e.printStackTrace()
                                        listener?.onOtherListener(2,"网络请求失败")
                                   }
                              }
                         }else{
                              responseScanPay = ScanQrResultBean(res?.ERRCODE,res?.ERRMSG)
                              validCode = 2
                         }
                    }catch (e: RemoteException) {
                         e.printStackTrace()
                    }
               }
          }

          LogUtil.e("test", Gson().toJson(responseScanPay))

          val payState = PayResultForUI()
          payState.way = "被扫支付"
          payState.orderid = ccbBean.ORDER_ID
          payState.timestamp = ccbBean.SIGN_TIME
          payState.dishes = data.products
          payState.piece = data.count.toInt()
          payState.cust_name = ccbBean.CUST_ID
          payState.payment = ccbBean.PAYMENT
          payState.acc_no = ""
          payState.acc_bal = ""
          payState.result = PayResultForUI.Result.SUCCESS
          payState.traceid = ""

          when(validCode) {
               3 -> { //支付成功
                    payState.payment = responseScanPay?.ACTUAL_PAYMENT
                    payState.acc_no = responseScanPay?.ACC_NO
                    payState.acc_bal = responseScanPay?.ACC_BAL
                    payState.result = PayResultForUI.Result.SUCCESS
                    try {
                         runBlocking (Dispatchers.IO) {
//                              responseScanPay?.let { consumeRecord(ccbScanPayBean, it) }
                         }
                    }catch (e :Exception){
                         e.printStackTrace()
                    }
                    listener?.onOtherListener(3,payState)
               }
               2 -> { //支付失败
                    payState.result = PayResultForUI.Result.FAIL
                    payState.errormsg =
                         "error ${responseScanPay?.ERRCODE} ${responseScanPay?.ERRMSG} "
                    listener?.onOtherListener(3,payState)
               }
               1 -> { //待支付

                    var offLineData = OffLineTable()
                    offLineData.campuS_ID = ccbBean.CAMPUS_ID
                    offLineData.corP_ID = ccbBean.CORP_ID
                    offLineData.txcode = ccbBean.TXCODE
                    offLineData.ccbSafeParam = ccbBean.ccbSafeParam
                    offLineData.businesS_ID = ccbBean.BUSINESS_ID
                    offLineData.vpoS_ID = ccbBean.VPOS_ID
                    offLineData.payment = ccbBean.PAYMENT
                    offLineData.actuaL_PAYMENT = ccbBean.ACTUAL_PAYMENT
                    offLineData.coupoN_INFO = ccbBean.COUPON_INFO
                    offLineData.qR_CODE = ccbBean.QR_CODE
                    offLineData.cusT_ID = ccbBean.CUST_ID
                    offLineData.ordeR_ID = ccbBean.ORDER_ID
                    offLineData.offline = ccbBean.OFFLINE
                    offLineData.sigN_TIME = ccbBean.SIGN_TIME
                    offLineData.decryptionCode = plainText
                    DishesDBHelper.getInstance().insertOffLineOrder(offLineData)
                    LogUtil.e("OffLineOrder", Gson().toJson(offLineData))

                    val dishList = mutableListOf<OffLineDishTable>()
                    data.products.forEach {
                         var dish = OffLineDishTable()
                         dish.corDishId = offLineData.ordeR_ID.toLong()
                         dish.dishesId = it.dishesId
                         dish.dishesName = it.dishesName
                         dish.dishesNumber = it.count
                         dish.dishesPrice = it.price
                         dishList.add(dish)
                    }
                    DishesDBHelper.getInstance().insertOffLineDishes(dishList)
                    LogUtil.e("OffLineDishes", Gson().toJson(dishList))

                    listener?.onOtherListener(4,payState)
               }
               0 -> { //离线码过期或者无效
                    listener?.onOtherListener(5,offLineCode)
               }
          }

     }

     private suspend fun consumeRecord(scanPay :CcbScanPayBean, resScan : ScanQrResultBean){
          val bean = SynConsumeRecordBean()
          bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
          bean.businessId = scanPay.BUSINESS_ID
          bean.counterId = scanPay.VPOS_ID
          bean.RESULT  = resScan.RESULT.toString()
          bean.CUST_ID = scanPay.CUST_ID
          bean.PAYMENT = resScan.PAYMENT?.toDouble()
          bean.ACTUAL_PAYMENT = resScan.ACTUAL_PAYMENT?.toDouble()
          bean.ACC_NO = resScan.ACC_NO
          bean.ACC_BAL = resScan.ACC_BAL?.toDouble()
          bean.ACC_TYPE = resScan.ACC_TYPE?.toInt()
          bean.TRACEID = ""
          bean.ORDER_ID = scanPay.ORDER_ID
          bean.TRAN_RESULT =  when(resScan.RESULT.toString()){
               "Y" -> 3
               "N" -> 2
               else -> null
          }
          bean.OFFLINE = scanPay.OFFLINE.toInt()
          bean.ERRCODE = resScan.ERRCODE
          bean.ERRMSG = resScan.ERRMSG
          bean.ACCALIAS = when(bean.ACC_TYPE){
               1 -> "现金账号"
               2 -> "餐补账户"
               3 -> "餐补账户1"
               else -> ""
          }
          bean.PAYTIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
          bean.BUSINESS_NAME = "彦诺智能测试园区"
          bean.paymentDishesList = mutableListOf()
          mDishes?.products?.forEach {
               bean.paymentDishesList.add(PaymentDishesList(
                    it.dishesId,
                    it.dishesName,
                    it.count,
                    it.price
               ))
          }

          val responseScanPay = mRespository.synCsRecord(bean)
          LogUtil.i(TAG,Gson().toJson(responseScanPay))
          if (responseScanPay.code != HttpURLConnection.HTTP_OK){
               LogUtil.e(TAG,"上传消费${bean.ORDER_ID} 订单失败==\n${responseScanPay.data}")
          }
          LogUtil.i(TAG,"订单${bean.ORDER_ID} 上传成功!")
     }


     fun release() {
          //取消扫码监听
          ScanDevice.setCallbackListener(null);
     }
}