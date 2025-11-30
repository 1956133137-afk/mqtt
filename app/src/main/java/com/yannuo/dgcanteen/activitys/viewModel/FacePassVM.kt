package com.yannuo.dgcanteen.activitys.viewModel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.facepass.CameraManager
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.model.EncryptedDataRequest
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.UploadFaceRequest
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FacePassVM: ViewModel(),FaceResultListener {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    val facePath: MutableLiveData<String> = MutableLiveData()
    val faceRecognized: MutableLiveData<EventFaceBean> = MutableLiveData()
    val facePreview: MutableLiveData<Bitmap?> = MutableLiveData()
    fun initAlgo() {
        //配置参数
        FaceSDKHelper.getInstance().getCameraManager()?.apply {
            setCameraFront(true).setIRRotation(0, 0)
                .setRGBRotation(0, 0)
                .setFaceSearchThreshold(75f)    //人脸分数
                .setDetect(true)    //人脸检测开关
                .setLiveness(true) //活检开关
                .setRecognize(true) //人脸识别开关
            getFacePass()?.startInputFrame()
        }
    }

    override fun onInitFace(code: Int, message: String) {
        //人脸初始化成功

    }

    override fun onPreView(data: ByteArray, width: Int, height: Int) {
        // 进入人脸算法之前的照片
        val bitmap = CameraUtil.instance.nv21ToBitmap(data, width, height)
        facePreview.postValue(bitmap)
    }

    override fun onLiveness(path: String): Boolean {
        LogUtil.d(TAG, "onLiveness 图片路径：$path")
        facePath.postValue(path)
        return true
    }

    override fun onRecognized(res: Boolean, msg: String, token: String, imgBase64: String, searchScore: Float) {
        Log.d(TAG, "onRecognized: 人脸识别成功信息：$msg")
        if (FaceSDKHelper.getInstance().getCameraManager() == null) {
            LogUtil.e(TAG, "onRecognized：cameraManager为空..")
            return
        }
        runBlocking(Dispatchers.IO) {
            FaceSDKHelper.getInstance().getCameraManager()?.getFacePass()?.stopInputFrame()
            if(res){
                val eventDataBean = EventFaceBean().apply {
                    this.code = 0
                    this.msg = token
                    this.img = imgBase64
                    this.searchScore = searchScore
                }
                faceRecognized.postValue(eventDataBean)
            } else {
                val eventDataBean = EventFaceBean().apply {
                    this.code = -1
                    this.msg = msg
                }
                faceRecognized.postValue(eventDataBean)
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