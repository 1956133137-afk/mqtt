package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.yannuo.dgcanteen.activitys.PayForFragment
import com.yannuo.dgcanteen.dao.ProductsTable
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.model.*


import com.yannuo.dgcanteen.printer.SCNPrinterHelper

import com.yannuo.dgcanteen.util.*

import com.yannuo.dgcanteen.views.NumberGenerateUtil
import com.yannuo.libscan.ScanThread
import com.yannuo.paylib.model.MerchantInfo
import com.yannuo.paylib.model.PayInfoCcb
import com.yannuo.paylib.model.PayResultForUI
import com.yannuo.paylib.pay.PayWithApi
import com.yannuo.paylib.repositorys.PayRepositoryOfPay


import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

class PayForPresenter(handler: PayForFragment.MyHandler,context : Context?) {
    private val TAG = javaClass.simpleName

    private var scope :CoroutineScope
    private var repository: PayRepositoryOfPay
    var showToastEvent : MutableLiveData<String>
    var loadingEvent : MutableLiveData<Boolean>
    private var merchantInfo : MerchantInfo?= null
    private var handle = handler
    private var queryPayStateTask :Job ?= null //订单支付结果轮询任务
    private var printerTicker  = AtomicReference<PrinterTicker>()  //打印机打印信息

    private var temporary = AtomicReference<PayResultForUI>() //查询临时支付结果
    private lateinit var mSCNPrinter : SCNPrinterHelper //打印机类
    private lateinit var mPayWithApi : PayWithApi //支付接口
    private var cnt = context



    //扫码状态，主要用于区分选择商品 和支付码
    enum class ScanState{
        INVALID,  //扫码数据无效
        ENTERING, //商品选择状态
        PAY        //支付状态
    }

    private var scanState = ScanState.ENTERING  //状态码



    init {
        val handle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler ${e.message}")
        }
        scope = CoroutineScope(Dispatchers.Default+SupervisorJob()+handle)
        repository = PayRepositoryOfPay()
        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()
        mSCNPrinter = SCNPrinterHelper()

        mPayWithApi = PayWithApi()
        LogUtil.d(TAG,"ASDA")

    }

    fun calculate(list : MutableList<ProductInfo>):FloatArray{
        val result = FloatArray(2)
        list.forEach {
            if (it.count > 0){
                val mid = it.pMoney.toBigDecimal().multiply(it.count.toBigDecimal()).add(result.get(0).toBigDecimal())
                result.set(0,mid.toFloat())
                result.set(1,it.count + result.get(1))
            }
        }
        return result
    }

    fun openPrinter() {
        mSCNPrinter.openPrinter()
    }


    fun printerSomething(data: PrinterTicker?):Int {
       return mSCNPrinter.printerSomething(data)
    }

    /**
     * 获取数据库中该条码对应的商品
     */
    fun getBarcodeProduct(barcode :String): ProductsTable? {
        return DbHelper.getInstance().queryProduct(barcode)
    }

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
     * 更新扫码的状态，用于被扫支付，根据支付状态去
     * 确定是商品条码还是支付二维码
     * @param state ScanState
     */
    fun setScanState(state :ScanState){
        scanState = state
    }


    inner class ScanCallback : ScanDevice.DataCallBack{
        override fun onData(it: String) {
            when (scanState) {
                ScanState.PAY -> {
                    LogUtil.i(TAG, "扫码数据: $it")
                    val message = handle.obtainMessage(Constant.EVENT_TWO, it)
                    handle.sendMessage(message)
//                    scanState = ScanState.INVALID //更新支付状态，以防止多次扫付款吗
                }
                ScanState.ENTERING -> {
                    val message = handle.obtainMessage(Constant.EVENT_FIVE,it)
                    handle.sendMessage(message)
                }
                ScanState.INVALID ->{
                    LogUtil.w(TAG,"扫码数据 INVALID !")
                }
            }
        }
    }


    fun payLogic(dat: MutableList<ProductInfo>, payWay :Int, listener : PayWithApi.PayStateCallback?,timeout :Int = 15, qr: String? = null){

        val payInfo = PayInfoCcb()
        //1.准备数据
        val money = calculate(dat)
        val productName = StringBuffer()
        dat.forEach {
            productName.append("${it.pName} ${it.count}件;")
        }
        payInfo.qrcode = qr
        payInfo.onln_py_txn_ordr_id = NumberGenerateUtil.getOrderNumber()
        payInfo.ahn_txnamt = money[0].toString()
        payInfo.piece = money[1].toInt()
        payInfo.cmdty_nm = productName.toString()


        mPayWithApi.payMoney(payWay,payInfo,listener,timeout)
    }



    fun release(){
        scope.cancel()
        mSCNPrinter.closePrinter()
    }

    /**
     * 生成打印机信息模板，用于打印小票
     * @param data MutableList<ProductInfo>
     */
    fun generatePrinterData(list: MutableList<ProductInfo>) {
        val title = "电子小票"
        val stitle = arrayOf("商品编码", "单价",  "数量" ,   "小计")
        val cashier = "蓝盼盼"
        val storeName = "建银大厦24F"
        val welcomeSpeech = "着力体验 用心感受"



        val products = mutableListOf<Array<String?>>()

        val result = FloatArray(2)
        list.forEach {
            if (it.count > 0){
                //+1表示最后跟商品名
                val sub = arrayOfNulls<String>(stitle.size+1)
                sub[0] = it.barCode
                sub[1] = it.pMoney
                sub[2] = it.count.toString()
                sub[4] = it.pName
                val mid = it.pMoney.toBigDecimal().multiply(it.count.toBigDecimal()) //小计
                sub[3] = mid.toFloat().toString()

                val value = mid.add(result.get(0).toBigDecimal())
                result.set(0,value.toFloat())
                result.set(1,it.count + result.get(1))
                products.add(sub)
            }
        }
        val amount =  result[0].toString()  //商品总价
        val quantity = result[1].toString() //商品总件数

        printerTicker?.set( PrinterTicker(title,stitle,products,
            quantity,amount,cashier,welcomeSpeech,storeName))
    }


    fun changePrinterData(time:String?,nember :String?): PrinterTicker? {

        val dat = printerTicker?.get()
        dat?.timeBuying = time
        dat?.journalNumber = nember
        return dat
    }

}