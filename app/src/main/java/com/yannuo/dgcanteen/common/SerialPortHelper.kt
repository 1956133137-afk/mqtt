package com.yannuo.dgcanteen.common



import android.os.SystemClock
import android.util.Log
import android_serialport_api.SerialPort
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.util.BytesUtils
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer
import com.tencent.mmkv.MMKV
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

    fun reverseHex(hex: String): String {
        val chars = hex.toCharArray()
        val length = chars.size
        val result = CharArray(length)

        chars.forEachIndexed { index, c ->
            result[index] = chars[length - index - 2]
            result[index + 1] = chars[length - index - 1]
        }
        return String(result)
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

    /*
     * W26(反序)：299f37d3 > 299f37（去掉后两位）> 379f29（反转） >（转10进制）
     */
    fun asciiReverseToDec(hex: String): String{
        val h = hex.substring(0, hex.length - 2)
        val reversedHex = reverseHex(h)
        LogUtil.d(tag,"W26 Reverse：${reversedHex}")
        val decimal = reversedHex.toInt(16)
        return decimal.toString()
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
                        LogUtil.d(tag,"ic卡：${content}")
                        delay(readTime)
                        if (mBufferedInputStream?.available() == 0) {
                            content = content.replace("\r\n","")
                            val mmkv=MMKV.defaultMMKV()
                            val selectedOption = mmkv.decodeInt(Constant.CARD_FORMAT)
                            if (content.length >= 5){
                                when(selectedOption){
                                    2-> {
                                        val asciiContent = asciiReverseToDec(content)
                                        readDataListener?.numberOfIcCard(asciiContent)
                                        LogUtil.d(tag,"10进制：${asciiContent}")
                                    }
                                    1->{
                                        val asciiContent = asciiTo10(content) //16进制转10进制
                                        readDataListener?.numberOfIcCard(asciiContent)
                                        LogUtil.d(tag,"10进制：${asciiContent}")
                                    }
                                    0->{
                                        readDataListener?.numberOfIcCard(content)//16进制
                                        LogUtil.d(tag,"16进制：${content}")
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