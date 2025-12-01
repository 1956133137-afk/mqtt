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
import com.yannuo.dgcanteen.databinding.ActivityLocalFaceRecognitionBinding
import com.yannuo.dgcanteen.facepass.CameraManager
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FacePass
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil
import org.greenrobot.eventbus.EventBus

class LocalFaceRecognitionActivity : BaseActivity<ActivityLocalFaceRecognitionBinding>() {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private var facePassVM: FacePassVM? = null
    private var cameraManager: CameraManager? = null

    override fun bindLayout() {
        binding = ActivityLocalFaceRecognitionBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
    }

    override fun onDestroy() {
        super.onDestroy()
        FaceSDKHelper.getInstance().closeCamera()
    }

    override fun onInit() {
        facePassVM = ViewModelProvider(this)[FacePassVM::class.java]
        FaceSDKHelper.getInstance().initFaceSDK(this,facePassVM)
        facePassVM!!.facePath.observe(this){
            binding.recImg.setImageURI(Uri.parse(it))
        }
        facePassVM!!.faceRecognized.observe(this){
            Log.d(TAG, "onInit: 识别结果：${Gson().toJson(it)}")
            finish()
        }
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

    private fun initEvent(){
        binding.returnBtn.setOnClickListener {
            Log.d(TAG, "initEvent: 返回")
            finish()
        }
        binding.open.setOnClickListener {
            val rect = Rect(290, 10, 990, 710)
            FaceSDKHelper.getInstance().openCamera(rect, binding.faceView)
        }
    }

}