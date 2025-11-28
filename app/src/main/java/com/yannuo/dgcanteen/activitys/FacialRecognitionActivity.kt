package com.yannuo.dgcanteen.activitys

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.view.TextureView
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityFacialRecognitionBinding
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.greenrobot.eventbus.EventBus

class FacialRecognitionActivity : BaseActivity<ActivityFacialRecognitionBinding>(),FacePassVM.RecognizeResListener {
    private lateinit var scope : CoroutineScope
    private val faceVM: FacePassVM = FacePassVM.instance
    private var facePath: String = ""
    private val handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout() {
        binding = ActivityFacialRecognitionBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        val job = SupervisorJob()
        scope = CoroutineScope(job + Dispatchers.IO)
        initObject()
        initEvent()
    }

    fun initObject(){
        FacePassVM.instance.initAlgo()
        faceVM.setListener(this)
    }

    fun initEvent(){
        binding.returnBtn.setOnClickListener {
            val bean = EventFaceBean().apply {
                code = 0
                msg = "人脸检测取消"
            }
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,bean))
            finish()
        }
        binding.faceView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val rect = Rect(290, 10, 990, 710)
                FaceSDKHelper.getInstance().openCamera(rect, binding.faceView)
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

    override fun onStop() {
        super.onStop()
        FaceSDKHelper.getInstance().closeCamera()
    }

    override fun onLiveness(path: String) {

    }

    override fun onSuccessful(token: String, imgBase64: String, searchScore: Float) {
        //识别成功
        val bean = EventFaceBean().apply {
            code = 0
            msg = token
            img = imgBase64
        }
        EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,bean))
        finish()
    }

    override fun onFailed(msg: String) {
        val bean = EventFaceBean().apply {
            code = 0
            this.msg = msg
        }
        //人脸识别失败
        EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,bean))
        finish()
    }

}