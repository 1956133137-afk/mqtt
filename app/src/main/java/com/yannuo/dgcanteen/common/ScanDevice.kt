package com.yannuo.dgcanteen.common

import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.libscan.ScanListener
import com.yannuo.libscan.ScanThread

class ScanDevice {
    private val TAG = javaClass.simpleName
    private var callback : MyScanListener?= null
    private var dataCallBack : DataCallBack?= null
    private var scanopenState = false  //扫码头打开状态
    @Volatile private var startTime = 0L //开始

    init {
        callback = MyScanListener()
    }

    /**
     * 打开扫码头
     */
    fun openScan(){
        if(scanopenState)return

//        val path = "/dev/ttyS4"
        val path = "/dev/ttyXRUSB0"
        ScanThread.ScanThreadEnum.INSTNACE.instance.open(path, callback)
    }

    fun setCallbackListener(dataListener : DataCallBack?){
        if (!scanopenState) LogUtil.w(TAG, "扫码头未打开!")
        dataCallBack = dataListener
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
        }finally {
            dataCallBack = null
            scanopenState = false
        }
    }

    //扫码回调
    inner class MyScanListener : ScanListener() {
        override fun onOpen(code: Int) {
            super.onOpen(code)
            scanopenState = true
            startTime = System.currentTimeMillis()
            LogUtil.i(TAG, "扫码头已打开")
        }

        override fun onRead(dat: String?) {

            dat?.trim()?.also {
                LogUtil.i(TAG, "扫码数据: $it")
                if(it.isEmpty())return
                if (System.currentTimeMillis()-startTime <1000)return
                dataCallBack?.onData(it)
            }
        }
    }

    interface DataCallBack{
        fun onData(data : String)
    }

}