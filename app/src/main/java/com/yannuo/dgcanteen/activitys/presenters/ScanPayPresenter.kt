package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import android.os.RemoteException
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice
import com.yannuo.libscan.ScanThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class ScanPayPresenter(mDishes : ProductsDetail, context :Context) {
     private val TAG = javaClass.simpleName
     private var mDishes : ProductsDetail ?= null
     private var mContext : Context ?= null
     private var mRespository :PayRepositoryOfPay
     var listener : IProductsVM?= null

     init {
          this.mDishes = mDishes
          this.mContext = context
          mRespository = PayRepositoryOfPay()
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
                         mDishes?.let { it1 -> startPayWithScan(it1,it) }
                         scanState = ScanState.INVALID //更新支付状态，以防止多次扫付款吗
                    }
                    ScanState.INVALID ->{
                         LogUtil.w(TAG,"扫码数据 INVALID !")
                    }
               }
          }
     }

     fun startPayWithScan(data :ProductsDetail,qrcode :String){
          val ccbScanPayBean = CcbScanPayBean()
          ccbScanPayBean.CAMPUS_ID = "441999527"
          ccbScanPayBean.CORP_ID = "1041"
          ccbScanPayBean.TXCODE = "PAY003"
          ccbScanPayBean.ccbSafeParam = ""
          ccbScanPayBean.BUSINESS_ID = "SJ2023032511004"
          ccbScanPayBean.VPOS_ID = "V00443832"
          ccbScanPayBean.PAYMENT = data.totalMoney
          ccbScanPayBean.ACTUAL_PAYMENT = data.totalMoney
          ccbScanPayBean.COUPON_INFO = ""
          ccbScanPayBean.ACC_NOS = ""
          ccbScanPayBean.QR_CODE = qrcode
          ccbScanPayBean.CUST_ID = ""
          ccbScanPayBean.ORDER_ID = "YN" + System.currentTimeMillis()
          ccbScanPayBean.OFFLINE = "0"
          ccbScanPayBean.SIGN_TIME = ""
          try{
               runBlocking (Dispatchers.IO) {
                    val responseScanPay = mRespository.getScanQrData(ccbScanPayBean)
                    LogUtil.e("test", Gson().toJson(responseScanPay))
                    val payState = PayResultForUI()
                    payState.way = "被扫支付"
                    payState.orderid = responseScanPay.ORDER_ID
                    payState.timestamp = ccbScanPayBean.SIGN_TIME
                    payState.dishes = data.products
                    payState.piece = data.count.toInt()
                    when(responseScanPay.RESULT.toString()){
                         "Y" ->{
                              payState.cust_name = ccbScanPayBean.CUST_ID
                              payState.payment = responseScanPay.PAYMENT
                              payState.acc_no =  responseScanPay.ACC_NO
                              payState.acc_bal = responseScanPay.ACC_BAL
                              payState.result = PayResultForUI.Result.SUCCESS
                              payState.traceid  = ""
//                              consumeRecord(ccbScanPayBean, responseScanPay)
                         }
                         else -> payState.errormsg = "error ${responseScanPay.ERRCODE} ${responseScanPay.ERRMSG} "
                    }
                    listener?.onScanPayResult(payState)
               }

          }catch (e: RemoteException) {
               e.printStackTrace()
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
}