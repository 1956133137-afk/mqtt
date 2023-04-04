package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import android.os.RemoteException
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.CcbScanPayBean
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice
import com.yannuo.libscan.ScanThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

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
          val bean = CcbScanPayBean()
          bean.CAMPUS_ID = "441999527"
          bean.CORP_ID = "1041"
          bean.TXCODE = "PAY003"
          bean.ccbSafeParam = ""
          bean.BUSINESS_ID = "SJ2023032511004"
          bean.VPOS_ID = "V00443832"
          bean.PAYMENT = payment
          bean.ACTUAL_PAYMENT = payment
          bean.COUPON_INFO = ""
          bean.ACC_NOS = ""
          bean.QR_CODE = qrcode
          bean.CUST_ID = ""
          bean.ORDER_ID = "YN" + System.currentTimeMillis()
          bean.OFFLINE = "0"
          bean.SIGN_TIME = ""
          try{
               runBlocking (Dispatchers.IO) {
                    val repository = PayRepositoryOfPay()
                    val response = repository.getScanQrData(bean)
                    LogUtil.e("test", Gson().toJson(response))
               }
          }catch (e: RemoteException) {
               e.printStackTrace()
          }
     }

}