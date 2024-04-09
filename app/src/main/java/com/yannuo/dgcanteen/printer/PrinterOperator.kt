package com.yannuo.dgcanteen.printer

import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

object PrinterOperator {
    private var helper:SCNPrinterHelper
    private var lastPort  :String ?= null
    private var lastBaudrate :String ?= null

    private var printering = AtomicBoolean(false)
    private val TAG = javaClass.simpleName
    private var mScope: CoroutineScope


    init {
        helper = SCNPrinterHelper()
        val exceptionHandler =  CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG,"打印异常： $throwable ${throwable.stackTraceToString()}")
            error("协程异常： ${throwable} ")

        }
        mScope = CoroutineScope(Dispatchers.IO + exceptionHandler)
    }


    fun open(){
        val mv = MMKV.defaultMMKV()
        val port = mv.decodeString(Constant.PRINTER_PATH_SET)
        val tr = mv.decodeString(Constant.PRINTER_BAUD_SET)
        if (port.equals(lastPort).not() || tr.equals(lastBaudrate).not()) {
            helper.closePrinter()
            helper.release()
            helper.openPrinter()
        }else{
            if (helper.isOpenPrinter.not()) {
                helper.release()
                helper.openPrinter()
            }
        }
    }

    fun close(){
        if (helper.isOpenPrinter) {
            helper.closePrinter()
            lastBaudrate = null
            lastPort = null
        }
    }

    //添加到预打印列表
    fun addElement(obj: Any?) {
        helper.addElement(obj)
    }

    //添加到打印列表
    fun addElements() {
        helper.addElements()
    }

    fun clearPreData() {
        helper.clearPreData()
    }

    //清空打印列表
    fun clearPrintCache() {
        helper.clearPrintCache()
    }

    fun printTicket() {
        if (printering.get()) return
        printering.set(true)
        mScope.launch {
            val code = helper.queryStatus()
            if (code != 0){
                val err = helper.resultCodeToString(code)
                CommonAndDpToPxUtil.speakWork("打印出错${err}")
                withContext(Dispatchers.Main){
                    ToastShowUtil.show("打印出错：${err}")
                }
                helper.clearPreData()
                printering.set(false)
                return@launch
            }
            helper.printTicket()
            printering.set(false)
        }
    }


     fun printerFoodsList(data: PayResultForUI){
        if (MMKV.defaultMMKV().decodeBool(Constant.EN_PRINTER).not()){
            close()
            helper.release()
            return
        }
        mScope.launch {
            PrinterOperator.apply {
                open()
                var dat = TextPrint().apply {
                    text="消费明细"
                    font = 0
                    style = 8
                    scaleW =1
                    scaleH = 1
                    align = 1
                }
                addElement(dat)
                dat = TextPrint().apply {
                    text=" \n"
                }
                addElement(dat)

                dat = TextPrint().apply {
                    text="用户:${data.cust_name ?:"***"}"
                }
                addElement(dat)

                dat = TextPrint().apply {
                    text="下单时间:${data.timestamp}"
                }
                addElement(dat)
                var way = data.way
                if (data.way == "20" || data.way == "21") {
                    way = "支付宝"
                    if (data.way == "20") way = "微信"
                }
                dat = TextPrint().apply {
                    text="支付方式:${way}"
                }
                addElement(dat)

                dat = TextPrint().apply {
                    text="${String.format("%-7s","商品名称：")}${String.format("%5s","数量*单价")}" +
                            "${String.format("%4s","金额")}"
                    style = 8
                }
                addElement(dat)
                dat = TextPrint().apply {
                    text="-----"
                }
                addElement(dat)

                data.dishes?.forEach {
                    dat = TextPrint().apply {
                        val width = EscapeUtil.calculateSize(it.dishesName,7)
                        text="${String.format("%-${width}s",it.dishesName)}${String.format("%-9s","${it.count}*${it.price}")}" +
                                "${String.format("%-8.2f",it.count * it.price)}"
                    }
                    addElement(dat)
                }
                dat = TextPrint().apply {
                    text="-----"
                }
                addElement(dat)
                addElement(Integer.valueOf(1))
                dat = TextPrint().apply {
                    text="订单号:"
                }
                addElement(dat)
                dat = TextPrint().apply {
                    var odi = ""
                    if (data.traceid.isNullOrEmpty())odi = data.orderid!!
                    text="${odi}"
                }
                addElement(dat)

                dat = TextPrint().apply {
                    text="付款:  ￥${data.payment}"
                    align = 2
                    style = 8
                }
                addElement(dat)
                addElement(Integer.valueOf(1))
                dat = TextPrint().apply {
                    text="谢谢惠顾！欢迎下次光临"
                    align = 1
                }
                addElement(dat)

                addElements()
                printTicket()

            }
        }
    }


}