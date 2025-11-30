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
    private var mFacePass: FacePass? = null
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
        mFacePass = FacePass(MyApplication.applicationContext)
        mFacePass!!.facePassConfig(faceInitListener)
        mCameraManager = CameraManager().initAlgorithm(mFacePass!!)
        CameraUtil.instance.initCamera(MyApplication.applicationContext)
    }

    fun initFaceSDK(listener: FaceResultListener?) {
        faceResultListener = listener
    }

    fun getCameraManager(): CameraManager? = mCameraManager
    fun getFacePass(): FacePass? = mFacePass

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
}