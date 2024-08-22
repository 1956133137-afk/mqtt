package com.yannuo.dgcanteen.printer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import com.csnprintersdk.csnio.CSNPOS
import com.csnprintersdk.csnio.CSNUSBPrinting
import com.csnprintersdk.csnio.csnbase.CSNIOCallBack
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.*
import java.util.concurrent.ArrayBlockingQueue

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/7/30 11:04
 **/
class USBPrinterHelper {
    private val TAG = javaClass.simpleName
    private val mContext = MyApplication.applicationContext
    private val handler = Handler(mContext.mainLooper)
    private lateinit var usbManager: UsbManager
    private var mUsb: CSNUSBPrinting? = null
    private var mPos: CSNPOS? = null
    private val kv = MMKV.defaultMMKV()
    private val printerThread: PrinterThread = PrinterThread()
    private val printQueue: ArrayBlockingQueue<PayForUI> = ArrayBlockingQueue(5)
    private var connectStatus = false
    private var connectTimes = 0

    private lateinit var mScope: CoroutineScope
    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: ${throwable.message}")
    }

    companion object {
        val instance: USBPrinterHelper by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(USBPrinterHelper::class.java) { USBPrinterHelper() }
        }
    }

    // 查找打印机
    fun queryPrinter() {
        usbManager = mContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceIterator: Iterator<UsbDevice> = usbManager.deviceList.values.iterator()
        while (deviceIterator.hasNext()) {  // 这里是if不是while，说明我只想支持一种device
            val device = deviceIterator.next()
            val equipmentStr = String.format("VID:%04X PID:%04X NAME:%s", device.vendorId, device.productId, device.productName)
            when (equipmentStr) {
                "VID:0FE6 PID:811E NAME:GP-58" -> if (!connectStatus) connectPrinter(device)
            }
        }
    }

    // 连接打印机
    private fun connectPrinter(device: UsbDevice) {
        connectTimes = 0
        mScope = CoroutineScope(Dispatchers.Default + mHandler)
        mScope.launch {
            while (!connectStatus && connectTimes < 10) {
                // 判断循环次数
                connectTimes++
                mUsb = CSNUSBPrinting()
                mPos = CSNPOS()
                mPos?.Set(mUsb)
                mUsb?.SetCallBack(MYCSNIOCallBack())
                if (!usbManager.hasPermission(device)) {
                    LogUtil.d(TAG, "设备没有权限")
                    // 获取权限
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                    val usbPermissionIntent = PendingIntent.getBroadcast(mContext, 0, Intent(mContext.applicationInfo.packageName), flags)
                    usbManager.requestPermission(device, usbPermissionIntent)
                }
                if (usbManager.hasPermission(device)) {
                    showText("打印机连接中...")
                    mUsb?.Open(usbManager, device, mContext)
                }
                // 每次循环间隔
                delay(2000)
                if (connectTimes >= 10 && !connectStatus) showText("打印机连接超时")
            }
        }
    }

    // 关闭打印机
    fun closePrinter() {
        if (mUsb != null) {
            mUsb?.Close()
            mUsb = null
        }
        if (mPos != null) mPos = null
    }

    private inner class MYCSNIOCallBack : CSNIOCallBack {
        override fun OnOpen() {
            connectStatus = true
            showText("打印机连接成功")
        }

        override fun OnOpenFailed() {
            connectStatus = false
            showText("打印机连接失败")
        }

        override fun OnClose() {
            connectStatus = false
            stopPrint()
            showText("打印机连接丢失")
            if (this@USBPrinterHelper::mScope.isInitialized) mScope.cancel()
        }
    }

    private fun showText(str: String) {
        handler.post { ToastShowUtil.show(str) }
    }

    fun printTicket(payForUI: PayForUI) {
        if (mPos?.GetIO()?.IsOpened() == false) return
        val state = queryPrintState()
        if (state != 0) LogUtil.e(TAG, codeToResult(state))
        else {
            val dateFormat = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
            if (kv.decodeString(Constant.PRINTER_UPDATE_TIME) != dateFormat) {
                kv.encode(Constant.PRINTER_UPDATE_TIME, dateFormat)
                kv.encode(Constant.PRINTER_AMOUNT, 1)
            }
            // 入队
            printQueue.offer(payForUI)
            startPrint()
        }
    }

    private fun startPrint() {
        if (printerThread.isInterrupt) {
            printerThread.isInterrupt = false
            printerThread.start()
        }
    }

    private fun stopPrint() {
        if (!printerThread.isInterrupt) printerThread.interrupt()
    }

    private inner class PrinterThread : Thread() {
        var isInterrupt = true

        override fun run() {
            super.run()
            while (!isInterrupt) {
                while (printQueue.size > 0) {
                    if (queryPrintState() == 0) {
                        printQueue.peek()?.let { printContent(it) }
                        var times = 3
                        while (times > 0) {
                            times--
                            try {
                                sleep(1000)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (times == 0 && queryPrintState() == 0) {
                                printQueue.poll()
                                kv.encode(Constant.PRINTER_AMOUNT, kv.decodeInt(Constant.PRINTER_AMOUNT, 1) + 1)
                            }
                        }
                    }
                }
            }
        }

        override fun interrupt() {
            isInterrupt = true
            super.interrupt()
        }
    }

    private fun printContent(payForUI: PayForUI) {
        mPos?.POS_Reset() //复位打印机
        mPos?.POS_S_Align(1) //居中对齐
        //按照一定的格式打印字符串
        mPos?.POS_TextOut("${kv.decodeString(Constant.PRINTER_TICKET_NAME, "电子发票联")}\r\n", 0, 0, 1, 1, 0, 0)
        mPos?.POS_FeedLine()
        mPos?.POS_TextOut("${String.format("%04d", kv.decodeInt(Constant.PRINTER_AMOUNT, 1))}\r\n", 0, 0, 1, 1, 0, 0)
        mPos?.POS_S_Align(0) //左对齐
        mPos?.POS_TextOut("================================\r\n", 0, 0, 0, 0, 0, 0)
        if (payForUI.username.isNotEmpty()) {
            mPos?.POS_TextOut("${printFormat("用户姓名", payForUI.username)}\r\n", 0, 0, 0, 0, 0, 0)
        }
        if (payForUI.accBal.isNotEmpty()) {
            mPos?.POS_TextOut("${printFormat("用户余额", "${payForUI.accBal}元")}\r\n", 0, 0, 0, 0, 0, 0)
        }
        if (payForUI.orderId.isNotEmpty()) {
            mPos?.POS_TextOut("${printFormat("订单号", payForUI.orderId)}\r\n", 0, 0, 0, 0, 0, 0)
        }
        if (payForUI.payment.isNotEmpty()) {
            mPos?.POS_TextOut("${printFormat("订单金额", String.format("%.02f元", payForUI.payment.toFloat()))}\r\n", 0, 0, 0, 0, 0, 0)
        }
        if (payForUI.payTime.isNotEmpty()) {
            mPos?.POS_TextOut("${printFormat("订单时间", payForUI.payTime)}\r\n", 0, 0, 0, 0, 0, 0)
        }
        mPos?.POS_TextOut("${printFormat("收银员", "${kv.decodeString(Constant.PRINTER_CASHIER_NAME, "10000001")}")}\r\n", 0, 0, 0, 0, 0, 0)
        val dishes = payForUI.paymentDishes
        if (dishes.size > 0) {
            var sum = 0.0
            mPos?.POS_FeedLine()
            mPos?.POS_TextOut("${printFormatMenu("名称", "数量", "小计")}\r\n", 0, 0, 0, 0, 0, 0)
            mPos?.POS_TextOut("--------------------------------\r\n", 0, 0, 0, 0, 0, 0)
            dishes.forEach {
                sum += it.dishesNumber.toFloat() * it.dishesPrice.toFloat()
                mPos?.POS_TextOut("${printFormatMenu(it.dishesName, it.dishesNumber, "${it.dishesNumber.toFloat() * it.dishesPrice.toFloat()}")}\r\n", 0, 0, 0, 0, 0, 0)
            }
            mPos?.POS_TextOut("--------------------------------\r\n", 0, 0, 0, 0, 0, 0)
            mPos?.POS_TextOut("${printFormat("合计", String.format("%.02f元", sum))}\r\n", 0, 0, 0, 0, 0, 0)
        }
        mPos?.POS_TextOut("================================\r\n", 0, 0, 0, 0, 0, 0)
        mPos?.POS_FeedLine()
        mPos?.POS_FeedLine()
        mPos?.POS_FeedLine()
        mPos?.POS_FullCutPaper()
    }

    private fun printFormat(msg: String, value: String): String {
        val frontCount = countBytesASCII(msg)
        val afterCount = countBytesASCII(value)
        // 总宽32个字节数
        val totalCount = 32 - frontCount - afterCount
        return if (totalCount < 0) msg
        else if (totalCount == 0) "$msg$value"
        else "$msg${addSpace(totalCount)}$value"
    }

    private fun printFormatMenu(name: String, count: String, price: String): String {
        val nameCount = countBytesASCII(name)
        val countCount = countBytesASCII(count)
        val priceCount = countBytesASCII(price)
        val count1 = if (18 - nameCount < 1) 0 else 18 - nameCount
        val count2 = if (4 - countCount < 1) 0 else 4 - countCount
        val count3 = if (10 - priceCount < 1) 0 else 10 - priceCount
        return "$name${addSpace(count1 + count2 / 2)}$count${addSpace(count2 / 2 + count3)}$price"
    }

    // 添加空格
    private fun addSpace(count: Int): String {
        val str = StringBuilder()
        for (i in 1..count) str.append(" ")
        return String(str)
    }

    // 计算字节数
    private fun countBytesASCII(str: String): Int {
        var byteCount = 0
        str.forEach { char ->
            // 通过ASCII的取值范围计算
            byteCount += if (char.code > 0x007F) 2 else 1
        }
        return byteCount
    }

    // 判断打印机状态
    private fun queryPrintState(): Int {
        if (!connectStatus || mUsb == null || mPos == null) return 5
        val byteArray = ByteArray(1)

        if (mPos?.POS_RTQueryStatus(byteArray, 1, 1000, 2) == true) {
            if (byteArray[0].toInt() and 0x08 == 0x08) return -4
//            if (byteArray[0].toInt() and 0x80 == 0x80) return 3
        } else return 5

        if (mPos?.POS_RTQueryStatus(byteArray, 2, 1000, 2) == true) {
            if (byteArray[0].toInt() and 0x04 == 0x04) return -6
            if (byteArray[0].toInt() and 0x20 == 0x20) return -5
//            if (byteArray[0].toInt() and 0x40 == 0x40) return 6
        } else return 5

        if (mPos?.POS_RTQueryStatus(byteArray, 3, 1000, 2) == true) {
            if (byteArray[0].toInt() and 0x08 == 0x08) return -2
            if (byteArray[0].toInt() and 0x20 == 0x20) return 4
            if (byteArray[0].toInt() and 0x40 == 0x40) return -3
        } else return 5

        if (mPos?.POS_RTQueryStatus(byteArray, 4, 1000, 2) == true) {
            if (byteArray[0].toInt() and 0x0C == 0x0C) return 1
            if (byteArray[0].toInt() and 0x60 == 0x60) return -5
        } else return 5

        return 0
    }

    // 状态详情
    private fun codeToResult(code: Int): String {
        return when (code) {
            6 -> "有错误情况"
            5 -> "打印机通讯异常"
            4 -> "有不可恢复错误票"
            3 -> "出纸口有未取小票，请注意及时取走小票"
            2 -> "紙将尽且出纸口有未取小票，请注意更换纸卷 和 及时取走小票"
            1 -> "紙将尽，请注意更换纸卷"
            0 -> " "
            -1 -> "未打印小票，请检查是否卡纸"
            -2 -> "切刀异常，请手动排除"
            -3 -> "打印头过热，请等待打印机冷却"
            -4 -> "打印机脱机"
            -5 -> "打印机缺纸"
            -6 -> "上盖打开"
            -7 -> "实时状态查询失败"
            -8 -> "查询状态失败，请检查通讯端口是否连接正常"
            -9 -> "打印过程中缺纸，请检查单据完整性"
            -10 -> "打印过程中上盖开启，请重新打印"
            -11 -> "连接中断，请确认打印机是否连线"
            -12 -> "请取走打印完的票据后，再进行打印！"
            else -> "未知错误"
        }
    }
}