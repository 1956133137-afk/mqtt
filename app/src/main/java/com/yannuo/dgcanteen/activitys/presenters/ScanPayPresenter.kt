package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import android.os.RemoteException
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice
import com.yannuo.libscan.ScanThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class ScanPayPresenter(mDishes : ProductsDetail, context :Context) {
     private val TAG = javaClass.simpleName
     private var mDishes : ProductsDetail ?= null
     private var mContext : Context ?= null

     init {
          this.mDishes = mDishes
          this.mContext = context
     }

     //扫码状态，主要用于区分选择商品 和支付码
     enum class ScanState{
          INVALID,  //扫码数据无效
          PAY        //支付状态
     }

     private var scanState = ScanState.INVALID  //状态码

     /**
      * 打开扫码头
      */
     fun scanListener(){
          ScanDevice.setCallbackListener(ScanCallback())
     }

     /**
      * 关闭扫码头释放资源
      */
     fun closeScan(){
          try {
               ScanThread.ScanThreadEnum.INSTNACE.instance.interrupt()
               ScanThread.ScanThreadEnum.INSTNACE.instance.close()
               ScanThread.ScanThreadEnum.INSTNACE.instance.stop()
          } catch (e: Exception) {
               LogUtil.w(TAG, "closeScan Exception!")
          }
     }

     /**
      * 更新扫码的状态，用于被扫支付，根据支付状态
      * @param state ScanState
      */
     fun setScanState(state : ScanState){
          scanState = state
     }

     inner class ScanCallback : ScanDevice.DataCallBack{
          override fun onData(it: String) {
               when (scanState) {
                    ScanState.PAY -> {
                         LogUtil.i(TAG, "扫码数据: $it")
                         mDishes?.let { it1 -> startPayWithScan(it1.totalMoney,it) }
//                    val message = handle.obtainMessage(Constant.EVENT_TWO, it)
//                    handle.sendMessage(message)
                         scanState = ScanState.INVALID //更新支付状态，以防止多次扫付款吗
                    }
                    ScanState.INVALID ->{
                         LogUtil.w(TAG,"扫码数据 INVALID !")
                    }
               }
          }
     }

     fun startPayWithScan(payment :String,qrcode :String){
          val ccbScanPayBean = CcbScanPayBean()
          ccbScanPayBean.CAMPUS_ID = "441999527"
          ccbScanPayBean.CORP_ID = "1041"
          ccbScanPayBean.TXCODE = "PAY003"
          ccbScanPayBean.ccbSafeParam = ""
          ccbScanPayBean.BUSINESS_ID = "SJ2023032511004"
          ccbScanPayBean.VPOS_ID = "V00443832"
          ccbScanPayBean.PAYMENT = payment
          ccbScanPayBean.ACTUAL_PAYMENT = payment
          ccbScanPayBean.COUPON_INFO = ""
          ccbScanPayBean.ACC_NOS = ""
          ccbScanPayBean.QR_CODE = qrcode
          ccbScanPayBean.CUST_ID = ""
          ccbScanPayBean.ORDER_ID = "YN" + System.currentTimeMillis()
          ccbScanPayBean.OFFLINE = "0"
          ccbScanPayBean.SIGN_TIME = ""
          try{
               runBlocking (Dispatchers.IO) {
                    val repository = PayRepositoryOfPay()
                    val responseScanPay = repository.getScanQrData(ccbScanPayBean)
                    LogUtil.e("test", Gson().toJson(responseScanPay))
                    consumeRecord(ccbScanPayBean, responseScanPay)
               }

          }catch (e: RemoteException) {
               e.printStackTrace()
          }
     }

     private suspend fun consumeRecord(scanPay :CcbScanPayBean, resScan : ScanQrResultBean){
          val bean = SynConsumeRecordBean()
          bean.deviceSerialNumber = ""
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

          val repository = PayRepositoryOfPay()
          repository.setConsumeRecord(bean)
     }
}