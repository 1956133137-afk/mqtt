package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import android.os.RemoteException
import android.text.format.DateFormat
import android.util.Log
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.IProductsVM
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

     private fun startPayWithScan(data :ProductsDetail,qrcode :String){
//          listener?.onOtherListener(1)
//          var validCode = 3
//          var responseScanPay: ScanQrResultBean ?= null
//          val ccbScanPayBean = CcbScanPayBean()
//          ccbScanPayBean.CAMPUS_ID = "441999527"
//          ccbScanPayBean.CORP_ID = "1041"
//          ccbScanPayBean.TXCODE = "PAY003"
//          ccbScanPayBean.ccbSafeParam = ""
//          ccbScanPayBean.BUSINESS_ID = "SJ2023032511004"
//          ccbScanPayBean.VPOS_ID = "V00443832"
//          ccbScanPayBean.PAYMENT = data.totalMoney
//          ccbScanPayBean.ACTUAL_PAYMENT = data.totalMoney
//          ccbScanPayBean.COUPON_INFO = ""
//          ccbScanPayBean.ACC_NOS = ""
//          ccbScanPayBean.QR_CODE = qrcode
//          ccbScanPayBean.CUST_ID = ""
//          ccbScanPayBean.ORDER_ID = TimeUtil.DateToTimestamp()
//          if (Constant.SWITCH == "true"){ //是否为离线模式
//               validCode = if (qrcode.contains("CCB")) 1
//               else 0
//          }
//          when(qrcode.contains("CCB")){ //是否为离线码
//               true -> {
//                    ccbScanPayBean.OFFLINE = "1"
//                    val plainText = DES3CBCUtil.transDecryption(qrcode)
//                    val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
//                    val hour = TimeUtil.timestamp(pastDueTime)
//                    if (hour < 1) validCode = 0
//               }
//               else -> ccbScanPayBean.OFFLINE = "0"
//          }
//          ccbScanPayBean.SIGN_TIME = DateFormat.format("yyyyMMddHHmmss",System.currentTimeMillis()).toString()
//          if (validCode == 3){
//               runBlocking (Dispatchers.IO) {
//                    responseScanPay = mRespository.getScanQrData(ccbScanPayBean)
//                    LogUtil.e("test", Gson().toJson(responseScanPay))
//                    if (responseScanPay!!.RESULT.toString() == "N"){
//                         validCode = 2
//                    }
//               }
//          }
//
//          val payState = PayResultForUI()
//          payState.way = "被扫支付"
//          payState.orderid = responseScanPay?.ORDER_ID
//          payState.timestamp = ccbScanPayBean.SIGN_TIME
//          payState.dishes = data.products
//          payState.piece = data.count.toInt()
//
//          when(validCode) {
//               3 -> { //支付成功
//                    payState.cust_name = ccbScanPayBean.CUST_ID
//                    payState.payment = responseScanPay?.PAYMENT
//                    payState.acc_no = responseScanPay?.ACC_NO
//                    payState.acc_bal = responseScanPay?.ACC_BAL
//                    payState.result = PayResultForUI.Result.SUCCESS
//                    payState.traceid = ""
////                    runBlocking (Dispatchers.IO) {
////                         responseScanPay?.let { consumeRecord(ccbScanPayBean, it) }
////                    }
//                    listener?.onScanPayResult(payState)
//               }
//               2 -> { //支付失败
//                    payState.errormsg =
//                         "error ${responseScanPay?.ERRCODE} ${responseScanPay?.ERRMSG} "
//                    listener?.onScanPayResult(payState)
//               }
//               1 -> { //待支付
//
//               }
//               0 -> { //离线码过期或者无效
//                    LogUtil.d(TAG, "离线码过期")
//               }
//          }
//          }catch (e: RemoteException) {
//               e.printStackTrace()
//          }

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