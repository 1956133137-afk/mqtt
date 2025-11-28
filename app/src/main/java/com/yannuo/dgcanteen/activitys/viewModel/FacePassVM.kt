package com.yannuo.dgcanteen.activitys.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.facepass.CameraManager
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.model.EncryptedDataRequest
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.UploadFaceRequest
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FacePassVM: ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()

    private var mCameraManager: CameraManager? = null
    private val myFaceResultListener by lazy { MyFaceResultListener() }

    private var listener: RecognizeResListener? = null
    private var uploadListener: RecognizeResListener? = null

    companion object {
        val instance: FacePassVM by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(FacePassVM::class.java) { FacePassVM() }
        }
    }

    fun getCameraManager(): CameraManager? {
        return mCameraManager
    }

    fun initAlgo() {
        FaceSDKHelper.getInstance().initFaceSDK(myFaceResultListener)
        //配置参数
        mCameraManager = FaceSDKHelper.getInstance().getCameraManager()?.apply {
            setCameraFront(true).setIRRotation(0, 0)
                .setRGBRotation(0, 0)
                .setFaceSearchThreshold(75f)    //人脸分数
                .setDetect(true)    //人脸检测开关
                .setLiveness(true) //活检开关
                .setRecognize(true) //人脸识别开关
        }
    }

    fun setListener(listener: RecognizeResListener){
        this.listener = listener
    }

    fun removeUploadListener(){
        uploadListener = null
    }

    fun setUploadListener(listener: RecognizeResListener){
        uploadListener = listener
    }

    private inner class MyFaceResultListener : FaceResultListener {
        override fun onInitFace(code: Int, message: String) {
            //人脸初始化成功

        }

        override fun onPreView(data: ByteArray, width: Int, height: Int) {
            // 进入人脸算法之前的照片
        }

        override fun onLiveness(path: String): Boolean {
            LogUtil.d(TAG, "onLiveness 图片路径：$path")
            runBlocking(Dispatchers.IO) {
                listener?.onLiveness(path)
                uploadListener?.onLiveness(path)
            }
            return true
        }

        override fun onRecognized(res: Boolean, msg: String, token: String, imgBase64: String, searchScore: Float) {
            Log.d(TAG, "onRecognized: 人脸识别成功信息：$msg")
            if (mCameraManager == null) {
                LogUtil.e(TAG, "onRecognized：cameraManager为空..")
                return
            }
            runBlocking(Dispatchers.IO) {
                mCameraManager?.getFacePass()?.stopInputFrame()
                if(res){
                    listener?.onSuccessful(token, imgBase64,searchScore)
                    uploadListener?.onSuccessful(token, imgBase64,searchScore)
                } else {
                    listener?.onFailed(msg)
                    uploadListener?.onFailed(msg)
                }
            }
        }

        override fun onTips(msg: String) {

        }

        override fun onError(errCode: String, errMsg: String) {

        }

        override fun onCancel() {

        }
    }

    interface RecognizeResListener{
        /**
         * 人脸活检成功
         */
        fun onLiveness(path: String)

        /**
         * 人脸识别成功
         */
        fun onSuccessful(token: String, imgBase64: String, searchScore: Float)

        /**
         * 人脸识别错误
         */
        fun onFailed(msg: String)
    }
}