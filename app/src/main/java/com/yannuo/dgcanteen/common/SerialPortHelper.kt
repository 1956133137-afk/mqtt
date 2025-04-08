package com.yannuo.dgcanteen.common



import android.R.attr.digits
import android.util.Log
import android_serialport_api.SerialPort
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.*
import java.io.*
import java.math.BigInteger
import java.nio.ByteBuffer


/**
 * @ClassName: SerialPortHelper
 * @Description:
 * @Author: cgz
 * @CreateDate: 2020/8/29 9:58
 * @Version:     1.0
 */
class SerialPortHelper() {
    private val tag = javaClass.simpleName
    var readDataListener : OnReadDataListener?= null
    private var byteBufferLength = 512
    private var readTime = 10L
    private var baudrate = 9600
    private var mInputStream: InputStream? = null
    private var mOutputStream: OutputStream? = null
    private var mSerialPort: SerialPort? = null
    private var mPort : String ?= null
    private val HexCode = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")
    private var rxArray = ByteArray(byteBufferLength)
    private var rxBuffer = ByteBuffer.wrap(rxArray)
    private var mScope :CoroutineScope ?= null
    private var readJob :Job ?= null
    private var mBufferedInputStream: BufferedInputStream? = null


    init {
        mScope = CoroutineScope(Dispatchers.IO)
    }

    fun openSerialPort(port: String, baudrate: Int = 9600): Boolean {
        this.baudrate = baudrate
        mPort = port
        if (mSerialPort == null) {
            initSerialPort()
        }
        return true
    }

    private fun initSerialPort() {
        try {
            mSerialPort = SerialPort(File(mPort),  baudrate, 0)
            mOutputStream = mSerialPort!!.getOutputStream()
            mInputStream = mSerialPort!!.getInputStream()
            mBufferedInputStream = BufferedInputStream(mInputStream)
            startReadThread()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    // 16进制转ASCII文本
    fun hex2Str(hex: String): String {

        val sb = StringBuilder()
        var i = 0
        while (i < hex.length - 1) {
            val h = hex.substring(i, i + 2)
            val decimal = h.toInt(16)
            sb.append(decimal.toChar())
            i += 2
        }
        return sb.toString()
    }

    fun byteArrayToHexString(b: ByteArray): String? {
        val result = java.lang.StringBuilder()
        for (value in b) {
            result.append(byteToHexString(value))
        }
        return result.toString()
    }

    fun byteToHexString(b: Byte): String? {
        var n = b.toInt()
        if (n < 0) {
            n = 256 + n
        }
        val d1 = n / 16
        val d2 = n % 16
        return HexCode.get(d1) + HexCode.get(d2)
    }
    /*
        W26(直转10进制):将8位数16进制去掉前两位,将剩余6位转为10进制
        */
    fun asciiTo10(hex: String): String{
        val h = hex.substring(2, hex.length)
        LogUtil.d(tag,"W26：${h }")
        val decimal = h.toInt(16)
        return decimal.toString()
    }

    /**
     * 反码16进制转10进制
     */
    fun hexStringToTenString(content: String): String {
        val B: String = content.substring(0, 2)
        val U: String = content.substring(2, 4)
        val I: String = content.substring(4, 6)
        val D: String = content.substring(6, 8)
        val bUid = String.format("%0"+10+"d",BigInteger(D + I + U + B, 16),true)
        return bUid
    }


    fun closeSerialPort() {
        readJob?.cancel()
//        if (mInputStream != null) {
//            mInputStream!!.close()
//            mInputStream = null
//        }

        if (mOutputStream != null) {
            mOutputStream!!.close()
            mOutputStream = null
        }
        if (mBufferedInputStream != null) {
            mBufferedInputStream?.close()
            mBufferedInputStream = null
        }
        if (mSerialPort != null) {
            mSerialPort!!.close()
            mSerialPort = null
        }

        rxBuffer.clear()
    }


    private fun startReadThread() {
        readJob =  mScope?.launch {
            var content =""
            while (isActive) {
                try {
                    delay(50)
                    var read = -1
                    if (mBufferedInputStream?.available() == 0)continue
                    read = mBufferedInputStream!!.read(rxArray)
                    while (read > 0) {
                        val buffer = ByteArray(read)
                        System.arraycopy(rxArray, 0, buffer, 0, read)
                        content += byteArrayToHexString(buffer)
//                        LogUtil.d(tag,"ic卡：${content}")
                        delay(readTime)
                        if (mBufferedInputStream?.available() == 0) {
                            content = content.replace("\r\n","")
                            val mmkv=MMKV.defaultMMKV()
                            val selectedOption = mmkv.decodeInt(Constant.CARD_FORMAT)
                            if (content.length >= 5){
                                when(selectedOption){
                                    1->{
                                        val asciiContent = asciiTo10(content) //16进制转10进制
                                        LogUtil.d(tag,"10进制：${asciiContent}")
                                        readDataListener?.numberOfIcCard(asciiContent)
                                    }
                                    0->{
                                        LogUtil.d(tag,"16进制：${content}")
                                        readDataListener?.numberOfIcCard(content)//16进制
                                    }
                                    2-> {
                                        val tenContent = hexStringToTenString(content)
                                        LogUtil.d(tag,"反码16进制转10进制：${tenContent}")
                                        readDataListener?.numberOfIcCard(tenContent)
                                    }
                                }
                            }
                            content =""
                        }
                        read = -1
                    }

                } catch (e: Exception) {
                    Log.i(tag, "SerialPortHelper  Exception ...")
                    e.printStackTrace()
                }
            }
            Log.d(tag, "read 结束")
        }
    }



}