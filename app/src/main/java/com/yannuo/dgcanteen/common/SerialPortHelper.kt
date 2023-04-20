package com.yannuo.dgcanteen.common


import android.content.Context
import android.os.SystemClock
import android.util.Log
import android_serialport_api.SerialPort
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.util.LogUtil

import java.io.*
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
    private var byteBufferLength = 128
    private var sleepTime = 2L
    private var readTime = 5L
    private var baudrate = 9600

    private var mBufferedInputStream: BufferedInputStream? = null
    private var mInputStream: InputStream? = null
    private var mOutputStream: OutputStream? = null
    private var mSerialPort: SerialPort? = null
    private var mReadThread: ReadThread? = null
    private var mPort : String ?= null
    private var startRead = false

    private val HexCode = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")

    // private var isParse = true   //false为发送数据的响应模式，true为接收数据的解析模式，主要是解析一帧数据
    private var rxArray = ByteArray(byteBufferLength)
    private var rxBuffer = ByteBuffer.wrap(rxArray)




    fun openSerialPort(port: String, baudrate: Int = 9600): Boolean {
        this.baudrate = baudrate
        if (mPort != port) {
            mPort = port
            closeSerialPort()
            initSerialPort()
        } else {
            mPort = port
            if (mSerialPort == null) {
                initSerialPort()
            }
        }
        startReadThread()
        return true
    }

    private fun initSerialPort() {
        try {
            //   mSerialPort = SerialPort(File(mPort), null, baudrate, 0, 8, 1, 0, 0)
            mSerialPort = SerialPort(File(mPort),  baudrate, 0)
//            mSerialPort!!.tcflush()
            mOutputStream = mSerialPort!!.outputStream
            mInputStream = mSerialPort!!.inputStream
            if (mInputStream != null) {
                mBufferedInputStream = BufferedInputStream(mInputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private inner class ReadThread() : Thread() {
        //  val lock = Any()
        override fun run() {
            super.run()
            if (!startRead) {
                Log.i(tag, "读取线程停止读取数据...")
                return
            }
            var content =""
            while (!isInterrupted && mBufferedInputStream != null) {
                try {
                    var read = -1
                    read = mBufferedInputStream!!.read(rxArray)
                    while (read > 0) {
                        SystemClock.sleep(readTime)
                        val buffer = ByteArray(read)
                        System.arraycopy(rxArray, 0, buffer, 0, read)
                        content += byteArrayToHexString(buffer)
//                        if (mBufferedInputStream?.available() == 0) {
//                            if (content.length >= 10){
//                                content = hex2Str(content)
//                                content = content.replace("\r\n","")
//                                readDataListener?.numberOfIcCard(content)
//                            }
//                            content =""
//                        }
                        content = content.replace("\r\n","")
                        if (read > 1){
                            readDataListener?.numberOfIcCard(content)
                        }
                        content =""
                        read = -1
                    }
                    sleep(50)
                } catch (e: Exception) {
                    Log.i(tag, "SerialPortHelper  Exception ...")
                    e.printStackTrace()
                }
            }
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

    /*fun send(bytes: ByteArray) :Boolean{
        if (mOutputStream != null) {
            try {
                SystemClock.sleep(10);
                val stringBuilder = StringBuilder()
                stringBuilder.append("串口工具类发送串口数据:  ")
                stringBuilder.append(bytes.size)
                LogUtil.i(tag, stringBuilder.toString());
                isParse = false
                mOutputStream!!.write(bytes)
                mOutputStream!!.flush()
                LogUtil.i(tag, "===========发送串口数据结束============");
                return true
            } catch (e: IOException) {
                e.printStackTrace();
                LogUtil.i(tag, "===========发送串口数据异常============");
            }
        }
        return false
    }*/
    fun send(bytes: ByteArray) :Boolean{
        if (mOutputStream != null) {
            try {
                SystemClock.sleep(10);
                val stringBuilder = StringBuilder()
                stringBuilder.append("串口工具类发送串口数据: ")
                stringBuilder.append(bytes.size)
//                LogUtil.i(tag, stringBuilder.toString()+","+ HexUtils.bytesToHexString(bytes,bytes.size))
                //  this.isParse = isParse
                mOutputStream!!.write(bytes)
                mOutputStream!!.flush()
                LogUtil.i(tag, "===========发送串口数据结束============");
                return true
            } catch (e: IOException) {
                e.printStackTrace();
                LogUtil.i(tag, "===========发送串口数据异常============");
            }
        }
        return false
    }

    fun stopReadData() {
        startRead = false
    }

    fun startReadData() {
        startRead = true
    }

    /**
     * 读取之前，先清空输入流,接收数据连续的时候以下不用写
     */
    fun flushInputStream() {
        if (mBufferedInputStream != null) {
            try {
                while (mBufferedInputStream!!.available() > 0) {
                    mBufferedInputStream!!.read()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 设置读取线程，读一次数据的缓存字节数组长度
     */
    fun setByteBufferLength(length: Int) {
        byteBufferLength = length
        rxArray = ByteArray(byteBufferLength)
    }
    /**
     * 设置读取串口数据的时间，readTime
     */
    fun setReadTime(readTime: Long) {
        this.readTime = readTime
    }

    /**
     * 设置线程读取一次数据的时间，sleepTime，线程心跳
     */
    fun setSleepTime(sleepTime: Long) {
        this.sleepTime = sleepTime
    }

    fun closeSerialPort() {
        closeReadThread()
        if (mBufferedInputStream != null) {
            mBufferedInputStream!!.close()
            mBufferedInputStream = null
        }
        if (mInputStream != null) {
            mInputStream!!.close()
            mInputStream = null
        }
        if (mOutputStream != null) {
            mOutputStream!!.close()
            mOutputStream = null
        }
        if (mSerialPort != null) {
            mSerialPort!!.close()
            mSerialPort = null
        }
        rxBuffer.clear()
    }

    fun closeReadThread() {
        startRead = false

        if (mReadThread != null && mReadThread!!.isAlive) {
            if (!mReadThread!!.isInterrupted) {
                mReadThread!!.interrupt()
            }
            mReadThread = null
        }
    }

    fun startReadThread() {
        startRead = true
        if (mReadThread == null) {
            mReadThread = ReadThread()
            mReadThread!!.start()
        } else if (mReadThread != null && mReadThread!!.isAlive) {
            if (mReadThread!!.isInterrupted) {
                mReadThread = ReadThread()
                mReadThread!!.start()
            } else {
                mReadThread!!.interrupt()
                mReadThread!!.start()
            }
        }
    }

    fun setOnReadDataListener(readDataListener: OnReadDataListener?) {
        this.readDataListener = readDataListener
    }


}