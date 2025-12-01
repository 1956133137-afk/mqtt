package com.yannuo.dgcanteen.activitys

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityFacialRecognitionBinding
import com.yannuo.dgcanteen.facepass.CameraManager
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import org.greenrobot.eventbus.EventBus

class FacialRecognitionActivity : BaseActivity<ActivityFacialRecognitionBinding>() {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private var facePassVM: FacePassVM? = null
    private var cameraManager: CameraManager? = null

    override fun bindLayout() {
        binding = ActivityFacialRecognitionBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
    }

    override fun onInit() {
        facePassVM = ViewModelProvider(this)[FacePassVM::class.java]
        FaceSDKHelper.getInstance().initFaceSDK(this,facePassVM)
        facePassVM!!.faceRecognized.observe(this){
            Log.d(TAG, "onInit: 识别结果：${Gson().toJson(it)}")
            val bean = EventFaceBean().apply {
                code = 0
                msg = it.msg
                img = it.img
                searchScore = it.searchScore
            }
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,bean))
            finish()
        }
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
                cameraManager?.setLiveness(true)?.setRGBRotation(0, 0)?.setIRRotation(0, 0)
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