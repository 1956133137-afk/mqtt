package com.yannuo.dgcanteen.facepass

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import android.view.TextureView
import com.yannuo.dgcanteen.common.MyApplication

class FaceSDKHelper {
    private val TAG = javaClass.simpleName
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

    fun initAlgorithm() {
        mCameraManager = CameraManager().initAlgorithm(MyApplication.applicationContext)
    }

    fun initFaceSDK(context: Context, listener: FaceResultListener?) {
        faceResultListener = listener
        CameraUtil.instance.initCamera(context)
    }

    fun getCameraManager(): CameraManager? = mCameraManager?.getCameraManager()

    fun openCamera(rect: Rect): Boolean = mCameraManager?.open(rect, faceResultListener) ?: false

    fun openCamera(rect: Rect, texture: TextureView): Boolean = mCameraManager?.open(rect, texture, faceResultListener) ?: false

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