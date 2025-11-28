package com.yannuo.dgcanteen.facepass

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import android.view.TextureView
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.util.LogUtil

class FaceSDKHelper {
    private val TAG = javaClass.simpleName
//    private val myFaceInitListener by lazy { MyFaceInitListener() }
//    private var mContext: Context? = null
    private var mCameraManager: CameraManager? = null
    private var faceResultListener: FaceResultListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var mFaceSDKHelper: FaceSDKHelper? = null

        @JvmStatic
        fun getInstance(): FaceSDKHelper {
            if (mFaceSDKHelper == null) {
                synchronized(FaceSDKHelper::class.java) {
                    if (mFaceSDKHelper == null) mFaceSDKHelper = FaceSDKHelper()
                }
            }
            return mFaceSDKHelper!!
        }
    }

    fun initFacePass(faceInitListener: SDKInitResult){
        mCameraManager = CameraManager()
        mCameraManager?.initAlgorithm(faceInitListener)
    }

    fun initFaceSDK(listener: FaceResultListener?) {
//        mContext = context
        faceResultListener = listener
        faceResultListener?.onInitFace(-1, "算法初始化中")
//        val authFace = AuthFace(context)
//        authFace.authCheck(myFaceInitListener)
        CameraUtil.instance.initCamera(MyApplication.applicationContext)
    }

    fun getCameraManager(): CameraManager? = mCameraManager?.getCameraManager()

    fun openCamera(rect: Rect): Boolean? = mCameraManager?.open(rect, faceResultListener)

    fun openCamera(rect: Rect, texture: TextureView): Boolean? = mCameraManager?.open(rect, texture, faceResultListener)

    /**
     * Android 低版本使用
     * @param surfaceHolder1 预览1
     * @param surfaceHolder2 预览2
     */
    fun startCameraPreview(surfaceHolder1: SurfaceHolder? = null, surfaceHolder2: SurfaceHolder? = null) {
        mCameraManager?.startCameraPreview(surfaceHolder1, surfaceHolder2)
    }

    /**
     * Android 高版本使用
     * @param surfaceTexture 预览值
     */
    fun startCameraPreview2(surfaceTexture: SurfaceTexture? = null) {
        mCameraManager?.startCameraPreview2(surfaceTexture)
    }

    fun closeCamera() {
        mCameraManager?.closeCamera()
    }

    fun releaseFaceSDK() {
        mCameraManager?.release()
    }

//    private inner class MyFaceInitListener : SDKInitResult {
//        override fun faceInitResult(code: Int, message: String) {
//            LogUtil.i(TAG, "faceInitResult code= $code message = $message")
//            faceResultListener?.onInitFace(0, if (code == 0) "初始化成功" else message)
//        }
//
//        override fun faceLicenseResult(code: Int, message: String) {
//            LogUtil.i(TAG, "初始化成功 code= $code message = $message")
//            if (code == 0) {
//                mCameraManager = CameraManager()
//                mCameraManager!!.initAlgorithm(mContext!!, myFaceInitListener)
//            } else faceResultListener?.onInitFace(code, message)
//        }
//    }
}