package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.hardware.usb.UsbManager
import android.os.IBinder
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.facepass.AuthFace
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.facepass.SDKInitResult
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.*
import mcv.facepass.FacePassHandler

class CameraService : Service() {
    private val TAG = javaClass.simpleName

    private lateinit var initcallback : SDKInitResult
    private lateinit var scope : CoroutineScope

    override fun onCreate() {
        super.onCreate()
        val job = SupervisorJob()
        scope = CoroutineScope( Dispatchers.IO + job)
        if (applicationInfo != null) {
            if((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0)
                LogUtil.currentLev = 3
        }

        val authFace = AuthFace(this) //算法授权检查
        initcallback  = object : SDKInitResult {
            override fun faceInitResult(code: Int, message: String) {
                when(code){
                    0 -> {
                        LogUtil.d(TAG,"初始化 version: ${FacePassHandler.getVersion()} 算法完成")
                        FaceHandler.getFaceHInstance().isFaceInit  = true
                    }
                    else -> {
                        //todo 算法初始化失败
                        LogUtil.e(TAG,"算法初始化失败")
                        ToastShowUtil.showt("算法初始化失败")
                        CommonAndDpToPxUtil.speakWork("算法初始化失败")
                        FaceHandler.getFaceHInstance().isFaceInit  = false
                    }
                }
            }

            override fun faceLicenseResult(code: Int, message: String) {
                LogUtil.i(TAG,"算法授权: $message")
                when(code){
                    0 -> {
                        FaceHandler.getInstance(applicationContext).initAlgorithm(initcallback)
                    }
                    else -> {
                        //todo 算法授权失败
                        ToastShowUtil.showt("算法未授权")
                        CommonAndDpToPxUtil.speakWork("算法未授权, $message")
                    }
                }
            }
        }
        authFace.authCheck(initcallback)

        scope.launch {  //获取配置信息
            getPayCfg()
        }

        deviceInit()
    }


    private fun deviceInit(){


    }

    private suspend fun getPayCfg() {
        val result = PayRepositoryOfPay().getPayCfg()
        if (result.code == 200){
            val mv = MMKV.defaultMMKV()
            mv.encode(Constant.PAY_CONFIG,result.data)
            LogUtil.i(TAG,"已更新配置信息！")
        }else{
            LogUtil.w(TAG,"更新配置信息失败！")
        }
    }




    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }



    override fun onDestroy() {
        LogUtil.i(TAG,"camera service close!")
        release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun release() {
        FaceHandler.getInstance()?.release()
    }






}