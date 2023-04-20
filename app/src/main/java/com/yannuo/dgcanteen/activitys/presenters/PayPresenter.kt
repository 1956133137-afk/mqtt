package com.yannuo.dgcanteen.activitys.presenters

import COM.CCB.EnDecryptAlgorithm.MCipherDecryptor
import android.os.RemoteException
import android.text.format.DateFormat
import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.dao.*
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class PayPresenter() : ScanDevice.DataCallBack, OnReadDataListener {
     private val TAG = javaClass.simpleName
     var mDishes : ProductsDetail ?= null
     private var mRespository :PayRepositoryOfPay
     private var mDataPresenter :DataPresenter
     var listener : CallbackListener?= null
     private lateinit var mCardHandle :SerialPortHelper

     init {

          mRespository = PayRepositoryOfPay()
          mDataPresenter = DataPresenter()
          //开始监听扫码数据
          ScanDevice.setCallbackListener(this)
          mCardHandle = SerialPortHelper()

     }

     //扫码状态，主要用于区分选择商品 和支付码
     enum class ScanState{
          CLOSE,  // 未初始化
          INVALID,  //扫码数据无效
          PAY        //支付状态
     }

     private var scanState = ScanState.INVALID  //状态码
     private var cardState = ScanState.INVALID  //

     /**
      * 更新扫码的状态，用于被扫支付，根据支付状态
      * @param state ScanState
      */
     fun setScanState(state : ScanState,data: ProductsDetail){
          scanState = state
          mDishes = data
     }

     /**
      * 更新刷卡的状态
      */
     fun setCardState(state :ScanState){
          cardState = state
     }

     /**
      * 打开IC卡串口
      */
     fun openIcCard(){
               mCardHandle.openSerialPort("/dev/ttyXRUSB0")
               cardState = ScanState.INVALID
               mCardHandle.readDataListener = this

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
               else -> {}
          }
     }

     private fun initData(data :ProductsDetail) : OffLineTable{
          val offBean = OffLineTable()
          offBean.campuS_ID = "441999527"
          offBean.corP_ID = "1041"
          offBean.txcode = "PAY003"
          offBean.ccbSafeParam = ""
          offBean.businesS_ID = "SJ2023032511004"
          offBean.vpoS_ID = "V00463775"
          offBean.payment = data.totalMoney
          offBean.actuaL_PAYMENT = data.totalMoney
          offBean.coupoN_INFO = ""
          offBean.acC_NOS = ""
          offBean.qR_CODE = "" //二维码
          offBean.cusT_ID = "" //用户ID
          offBean.ordeR_ID = NumberGenerateUtil.getOrderNumber()
          offBean.offline = "0" //是否为离线码
          offBean.sigN_TIME = DateFormat.format("yyyyMMddHHmmss",System.currentTimeMillis()).toString()
          return offBean
     }

     private fun startPayWithScan(data :ProductsDetail,qrcode :String){
          listener?.onOtherListener(1)
          var validCode = 3   //支付状况
          var offLineCode = 1
          var responseScanPay: ScanQrResultBean ?= null
          var res: ScanAnalysisBean ?= null
          var plainText = ""
          val ccbBean = initData(data)

          if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)){ //离线模式
               if (qrcode.contains("CCB")) validCode = 1
               else {
                    validCode = 0
                    offLineCode = 0
               }
          }

          when(qrcode.contains("CCB")){ //离线码
               true -> {
                    ccbBean.qR_CODE = qrcode
                    ccbBean.offline = "1"
                    plainText = DES3CBCUtil.transDecryption(qrcode)
                    val pastDueTime = DES3CBCUtil.getTimestamp(plainText)
                    val hour = TimeUtil.timestamp(pastDueTime)
                    if (hour < 1) validCode = 0

                    if (validCode == 3){
                         try {
                              runBlocking (Dispatchers.IO) {
                                   var map = CanteenEncryptionUtil.getScanToPay(ccbBean)
                                   responseScanPay = mRespository.getCcbData(map).body()?.let { //扫码支付
                                        Gson().fromJson(it.string().replace("\r\n",""), ScanQrResultBean::class.java)
                                   }
                                   if (responseScanPay?.RESULT.toString() == "Y"){
                                        ccbBean.txcode = "PAY006"
                                        map = CanteenEncryptionUtil.getQueryRecord(ccbBean)
                                        val payResult = mRespository.getCcbData(map).body()?.let { //扫码支付
                                             Gson().fromJson(it.string().replace("\r\n",""), ScanQueryBean::class.java)
                                        }
                                        ccbBean.cusT_ID = payResult?.CUST_ID.toString()
                                   }else{
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
                    ccbBean.offline = "0"
                    try { //解析二维码
                         ccbBean.txcode = "PAY002"
                         ccbBean.qR_CODE = qrcode
                         var map = CanteenEncryptionUtil.getAnalysisQr(ccbBean)
                         runBlocking (Dispatchers.IO) {
                              res = mRespository.getCcbData(map).body()?.let { //解析二维码
                                   Gson().fromJson(it.string().replace("\r\n",""), ScanAnalysisBean::class.java)
                              }
                         }
                         if (res?.RESULT.toString() == "Y"){
                              ccbBean.txcode = "PAY003"
                              ccbBean.cusT_ID = res?.CUST_ID.toString()
                              if (validCode == 3){
                                   map = CanteenEncryptionUtil.getScanToPay(ccbBean)
                                   runBlocking (Dispatchers.IO) {
                                        responseScanPay = mRespository.getCcbData(map).body()?.let { //扫码支付
                                             Gson().fromJson(it.string().replace("\r\n",""), ScanQrResultBean::class.java)
                                        }
                                        if (responseScanPay?.RESULT.toString() == "N"){
                                             validCode = 2
                                        }
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
          payState.orderid = ccbBean.ordeR_ID
          payState.timestamp = ccbBean.sigN_TIME
          payState.dishes = data.products
          payState.piece = data.count.toInt()
          payState.cust_name = ccbBean.cusT_ID
          payState.payment = ccbBean.payment
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
                              responseScanPay?.let { mDataPresenter.consumeRecord(ccbBean, it, data.products) }
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

                    ccbBean.decryptionCode = plainText
                    ccbBean.postTag = false
                    DishesDBHelper.getInstance().insertOffLineOrder(ccbBean)

                    val dishList = mutableListOf<OffLineDishTable>()
                    data.products.forEach {
                         var dish = OffLineDishTable()
                         dish.dishesId = it.dishesId
                         dish.dishesName = it.dishesName
                         dish.dishesNumber = it.count
                         dish.dishesPrice = it.price
                         dish.order = ccbBean
                         dishList.add(dish)
                    }
                    DishesDBHelper.getInstance().insertOffLineDishes(dishList)

                    LogUtil.d(TAG,"离线订单已保存")
                    listener?.onOtherListener(4,payState)
               }
               0 -> { //离线码过期或者无效
                    listener?.onOtherListener(5,offLineCode)
               }
          }

     }



     fun release() {
          //取消扫码监听
          ScanDevice.setCallbackListener(null)
          mCardHandle.readDataListener = this
          mCardHandle.closeSerialPort()
     }

     //IC卡数据
     override fun numberOfIcCard(number: String?) {
          number?.trim()?.also {
//               cardState = ScanState.INVALID
               LogUtil.d(TAG,"number :${number}")
               payByCard(it.toUpperCase())

          }

     }

     private fun payByCard(cardId :String) {
          val payBean = CardPay()
          payBean.campus_id = "441999527"
          payBean.corp_id = "1041"
          payBean.txcode = "PAY005"
          payBean.business_id = "SJ2023032511004"
          payBean.vpos_id = "V00463775"
          payBean.payment = mDishes?.totalMoney
          payBean.actual_payment = mDishes?.totalMoney
          payBean.offline = "0"
          if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)){
               payBean.offline = "1"
          }
          payBean.sign_time = DateFormat.format("yyyyMMddHHmmss",System.currentTimeMillis()).toString()
          payBean.card_id = cardId
          runBlocking {
               val map = mutableMapOf<String,String>()
               map["campusId"] = payBean.campus_id
               map["cardId"] = payBean.card_id
               val res = mRespository.getUserInfo(map)
               LogUtil.i(TAG,Gson().toJson(res))
               if (res.code == HttpURLConnection.HTTP_OK){
                    payBean.cust_id = res.data?.custId ?: ""
               }
          }
          payBean.order_id= NumberGenerateUtil.getOrderNumber()

          val payState = PayResultForUI()
          payState.way = "刷卡支付"
          payState.orderid = payBean.order_id
          payState.timestamp = payBean.sign_time
          payState.dishes = mDishes?.products
          payState.piece = mDishes?.count?.toInt() ?: 0
          payState.cust_name = payBean.cust_id
          payState.payment = payBean.payment
          payState.acc_no = ""
          payState.acc_bal = ""
          payState.result = PayResultForUI.Result.SUCCESS
          payState.traceid = ""

          when(payBean.offline){
               "0" -> {
                    runBlocking {

                         val map = CanteenEncryptionUtil.getCardToPay(payBean)
                         val res = mRespository.getCcbData(map).body()?.let { //扫码支付
                              Gson().fromJson(it.string().replace("\r\n",""), ScanQrResultBean::class.java)
                         }

                         LogUtil.e("ning", Gson().toJson(res))
                         if (res?.RESULT.toString() == "Y"){
                              payState.payment = res?.ACTUAL_PAYMENT
                              payState.acc_no = res?.ACC_NO
                              payState.acc_bal = res?.ACC_BAL
                              payState.result = PayResultForUI.Result.SUCCESS
                              if (mDishes != null && res != null){
                                   mDataPresenter.cardConsumeRecord(payBean, res, mDishes!!.products)
                              }
                         }else {
                              payState.result = PayResultForUI.Result.FAIL
                              payState.errormsg = "error ${res?.ERRCODE} ${res?.ERRMSG} "
                         }
                         listener?.onOtherListener(3,payState)
                    }
               }
               "1" -> {
                    payBean.up = false
                    DishesDBHelper.getInstance().insertCardOrder(payBean)

                    val dishList = mutableListOf<CardDishTable>()
                    mDishes?.products?.forEach {
                         var dish = CardDishTable()
                         dish.dishesId = it.dishesId
                         dish.dishesName = it.dishesName
                         dish.dishesNumber = it.count
                         dish.dishesPrice = it.price
                         dish.order = payBean
                         dishList.add(dish)
                    }
                    DishesDBHelper.getInstance().insertCardDishes(dishList)

                    LogUtil.d(TAG,"离线订单已保存")
                    listener?.onOtherListener(4,payState)
               }
          }
     }

}