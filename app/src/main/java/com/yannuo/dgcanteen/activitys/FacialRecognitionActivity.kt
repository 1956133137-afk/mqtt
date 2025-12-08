package com.yannuo.dgcanteen.activitys

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityFacialRecognitionBinding
import com.yannuo.dgcanteen.facepass.CameraManager
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import mcv.facepass.types.FacePassRecognitionResult
import org.greenrobot.eventbus.EventBus

class FacialRecognitionActivity : BaseActivity<ActivityFacialRecognitionBinding>() {
    private val kv = MMKV.defaultMMKV()
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private var cameraManager: CameraManager? = null
    private val myFaceResultListener by lazy { MyFaceResultListener() }

    private inner class MyFaceResultListener : FaceResultListener {
        override fun onInitFace(code: Int, message: String) {

        }

        override fun onPreView(data: ByteArray, width: Int, height: Int) {

        }

        override fun onRecognized(result: FacePassRecognitionResult?, path: String) {
            if(result != null){
                val eventDataBean = EventFaceBean().apply {
                    this.code = 0
                    this.msg = String(result.faceToken)
                    this.img = path
                    this.searchScore = result.detail.searchScore
                }
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,eventDataBean))
            } else {
                val eventDataBean = EventFaceBean().apply {
                    this.code = -1
                    this.msg = "未识别到人脸"
                }
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,eventDataBean))
            }
            finish()
        }

        override fun onTips(msg: String) {

        }

        override fun onError(errCode: String, errMsg: String) {

        }

        override fun onCancel() {

        }

    }

    override fun bindLayout() {
        binding = ActivityFacialRecognitionBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
    }

    override fun onInit() {
        FaceSDKHelper.getInstance().initFaceSDK(this,myFaceResultListener)
        initObject()
        initEvent()
    }

    fun initObject(){
        binding.faceView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val rect = Rect(290, 10, 990, 710)
                FaceSDKHelper.getInstance().openCamera(rect, binding.faceView)
                cameraManager = FaceSDKHelper.getInstance().getCameraManager()
                cameraManager?.setLiveness(kv.decodeBool(Constant.LIVE_ENABLE_SET, Constant.LIVE_ENABLE_SET_V))?. //是否活检
                setFaceMinThreshold((200 - kv.decodeFloat(Constant.DISTANCE_SET, Constant.DISTANCE_SET_V) * 100).toInt())?. //识别距离
                //预览角度和相机角度
                setRGBRotation(kv.decodeInt(Constant.PRE_ANGLE_SET, Constant.PRE_ANGLE_SET_V), kv.decodeInt(Constant.ROTATE_SET, Constant.ROTATE_SET_V))?.setIRRotation(kv.decodeInt(Constant.PRE_ANGLE_SET, Constant.PRE_ANGLE_SET_V), kv.decodeInt(Constant.ROTATE_SET, Constant.ROTATE_SET_V))?.
                setCameraMirror(kv.decodeBool(Constant.MIRROR_SET, Constant.MIRROR_SET_V),kv.decodeBool(Constant.MIRROR_SET, Constant.MIRROR_SET_V))?.  //是否镜像
                setFacePose(kv.decodeFloat(Constant.ROLL_SET, Constant.ROLL_SET_V),kv.decodeFloat(Constant.PITCH_SET, Constant.PITCH_SET_V),kv.decodeFloat(Constant.YAW_SET, Constant.YAW_SET_V))?. //三维角度
                setFaceLivenessThreshold(kv.decodeFloat(Constant.RECOGNIZE_VALUE_SET, Constant.RECOGNIZE_VALUE_SET_V))
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }
    }

    fun initEvent(){
        binding.returnBtn.setOnClickListener {
            val bean = EventFaceBean().apply {
                code = -1
                msg = "人脸检测取消"
            }
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,bean))
            finish()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        FaceSDKHelper.getInstance().closeCamera()
    }

}