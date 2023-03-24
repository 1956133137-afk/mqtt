package com.yannuo.dgcanteen.activitys.presenters


import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.yannuo.dgcanteen.activitys.PayForFragment
import com.yannuo.dgcanteen.dao.ProductsTable
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.PrinterTicker
import com.yannuo.dgcanteen.model.ProductInfo

import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice
import com.yannuo.libscan.ScanThread
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

class PayForPresenter(handler: PayForFragment.MyHandler,context : Context?) {
    private val TAG = javaClass.simpleName

    private var scope :CoroutineScope

    var showToastEvent : MutableLiveData<String>
    var loadingEvent : MutableLiveData<Boolean>
    private var handle = handler
    private var queryPayStateTask :Job ?= null //订单支付结果轮询任务
    private var printerTicker  = AtomicReference<PrinterTicker>()  //打印机打印信息
    private var temporary = AtomicReference<PayResultForUI>() //查询临时支付结果

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

        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()


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
//                    val message = handle.obtainMessage(Constant.EVENT_TWO, it)
//                    handle.sendMessage(message)
//                    scanState = ScanState.INVALID //更新支付状态，以防止多次扫付款吗
                }
                ScanState.ENTERING -> {
//                    val message = handle.obtainMessage(Constant.EVENT_FIVE,it)
//                    handle.sendMessage(message)
                }
                ScanState.INVALID ->{
                    LogUtil.w(TAG,"扫码数据 INVALID !")
                }
            }
        }
    }


    fun release(){
        scope.cancel()

    }


}