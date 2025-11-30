package com.yannuo.dgcanteen.activitys

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.ViewModelProvider
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.databinding.ActivityLocalFaceRecognitionBinding
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import org.greenrobot.eventbus.EventBus

class LocalFaceRecognitionActivity : BaseActivity<ActivityLocalFaceRecognitionBinding>() {
    private var facePassVM: FacePassVM? = null

    override fun bindLayout() {
        binding = ActivityLocalFaceRecognitionBinding.inflate(layoutInflater)
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        FaceSDKHelper.getInstance().closeCamera()
//    }

    override fun onInit() {
        binding.lifecycleOwner = this
        facePassVM = ViewModelProvider(this)[FacePassVM::class.java]
        binding.viewModel = facePassVM
        facePassVM!!.facePath.observe(this){
//            EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE_PATH,it))
//            finish()
        }
        facePassVM!!.faceRecognized.observe(this){
//            EventBus.getDefault().post(MessageEvent(Constant.EVENT_LOCAL_FACE,it))
        }
        facePassVM!!.facePreview.observe(this){

        }
        FaceSDKHelper.getInstance().initFaceSDK(facePassVM)
        facePassVM!!.initAlgo()
        initObject()
        initEvent()
    }


    private fun initObject(){
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

    private fun initEvent(){
        binding.returnBtn.setOnClickListener {
            Log.d(TAG, "initEvent: 返回")
            finish()
        }
    }

}