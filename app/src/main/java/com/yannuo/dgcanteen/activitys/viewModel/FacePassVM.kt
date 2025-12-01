package com.yannuo.dgcanteen.activitys.viewModel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mcv.facepass.types.FacePassRecognitionResult

class FacePassVM: ViewModel(),FaceResultListener {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    val facePath: MutableLiveData<String> = MutableLiveData()
    val faceRecognized: MutableLiveData<EventFaceBean> = MutableLiveData()
    val facePreview: MutableLiveData<Bitmap?> = MutableLiveData()

    fun initAlgo() {
        //配置参数
        FaceSDKHelper.getInstance().getCameraManager()?.apply {
            setCameraFront(true)
                .setIRRotation(0, 0)
                .setRGBRotation(0, 0)
                .setFaceSearchThreshold(75f)    //人脸分数
                .setLiveness(true) //活检开关
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

    override fun onRecognized(result: FacePassRecognitionResult?, path: String) {
        Log.d(TAG, "onRecognized: 人脸识别信息：${Gson().toJson(result)}")
        runBlocking(Dispatchers.IO) {
            if(result != null){
                val eventDataBean = EventFaceBean().apply {
                    this.code = 0
                    this.msg = String(result.faceToken)
                    this.img = path
                    this.searchScore = result.detail.searchScore
                }
                faceRecognized.postValue(eventDataBean)
            } else {
                val eventDataBean = EventFaceBean().apply {
                    this.code = -1
                    this.msg = "未识别到人脸"
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

    interface FaceListener{
        fun faceResults()
    }
}