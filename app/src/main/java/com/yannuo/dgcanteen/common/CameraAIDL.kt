package com.yannuo.dgcanteen.common

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.ccb.smartcanteen.ZHSTFacePayService
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil

object CameraAIDL {
    private val TAG = javaClass.simpleName
    private var mFacePayService: ZHSTFacePayService? = null
    private var logic :CameraLogic

    init {
        logic = CameraLogic()
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, " onServiceConnected")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mFacePayService = null
            LogUtil.d(TAG, " onServiceDisconnected")
        }
    }

    fun connectAIDL():Boolean{
        if (mFacePayService == null) {
            val lIntent = Intent()
            lIntent.action = "com.ccb.smartcanteen.FacePayService"
            lIntent.setPackage("com.ccb.smartcanteen")
            MyApplication.applicationContext.bindService(
                lIntent,
                mServiceConnection,
                AppCompatActivity.BIND_AUTO_CREATE
            )
            return true
        }
        return false
    }



    fun startCamera(amount:Float , listener : IProductsVM?){
        if (mFacePayService == null) {
            CommonAndDpToPxUtil.speakWork("人脸服务通信异常")
            return
        }
        logic.startPayWithFace(mFacePayService,amount,listener)
    }





}