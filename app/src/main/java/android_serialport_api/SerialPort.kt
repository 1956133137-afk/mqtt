package android_serialport_api

import com.yannuo.dgcanteen.util.LogUtil
import java.io.*

/**
 * Author: filowl
 * Description: android串口api
 * Date: 2024/4/29 9:21
 **/
class SerialPort {
    private val TAG = javaClass.simpleName
    private var mFd: FileDescriptor? = null
    private var fileInputStream: FileInputStream? = null
    private var fileOutputStream: FileOutputStream? = null

    constructor(sPort: File, baudRate: Int, flags: Int) {
        /* Check access permission */
        if (!sPort.canRead() || !sPort.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                val process = Runtime.getRuntime().exec(getSystemSuFilePath())
                val cmd = "chmod 666 ${sPort.absolutePath}\nexit\n"
                process.outputStream.write(cmd.toByteArray())
                if (process.waitFor() != 0 || !sPort.canRead() || !sPort.canWrite()) {
                    throw SecurityException()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw SecurityException()
            }
        }
        mFd = open(sPort.absolutePath, baudRate, flags)
        if (mFd == null) {
            LogUtil.e(TAG, "native open returns null")
            throw IOException()
        }
        LogUtil.d(TAG, "串口打开成功: ${sPort.absolutePath}")
        fileInputStream = FileInputStream(mFd)
        fileOutputStream = FileOutputStream(mFd)
    }

    init {
        System.loadLibrary("serial_port")
    }

    fun getInputStream(): InputStream? {
        return fileInputStream
    }

    fun getOutputStream(): OutputStream? {
        return fileOutputStream
    }

    private external fun open(path: String, baudRate: Int, flags: Int): FileDescriptor?
    external fun close()

    private fun getSystemSuFilePath(): String {
        var filepath = "/system/bin/su"
        val kSuSearchPaths = arrayOf("/system/bin/", "/system/xbin/")
        try {
            for (i in kSuSearchPaths.indices) {
                val file = File(kSuSearchPaths[i] + "su")
                if (file != null && file.exists()) {
                    filepath = kSuSearchPaths[i] + "su"
                    return filepath
                }
            }
            return filepath
        } catch (e: Exception) {
            return filepath
        }
    }
}