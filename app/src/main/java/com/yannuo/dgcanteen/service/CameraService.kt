package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.hardware.usb.UsbManager
import android.os.IBinder
import com.yannuo.dgcanteen.facepass.AuthFace
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.facepass.SDKInitResult
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
                        LogUtil.d(TAG,"已获取授权准备初始化 version: ${FacePassHandler.getVersion()} 算法")
                        FaceHandler.getFaceHInstance().isFaceInit  = false
                    }
                    else -> {
                        //todo 算法初始化失败
                        LogUtil.e(TAG,"算法初始化失败")
                        ToastShowUtil.showt("算法初始化失败")
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
                    }
                }
            }
        }
        authFace.authCheck(initcallback)


        deviceInit()
    }


    private fun deviceInit(){


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