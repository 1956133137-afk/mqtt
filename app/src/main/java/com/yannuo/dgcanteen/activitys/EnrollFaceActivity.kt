package com.yannuo.dgcanteen.activitys

import android.content.Intent
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import androidx.lifecycle.ViewModelProvider
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.databinding.ActivityEnrollFaceBinding
import com.yannuo.dgcanteen.facepass.CameraManager
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FaceSDKHelper

class EnrollFaceActivity : BaseActivity<ActivityEnrollFaceBinding>() {
    private var facePath: String = ""
    private var facePassVM: FacePassVM? = null
    private var cameraManager: CameraManager? = null

    override fun bindLayout() {
        binding = ActivityEnrollFaceBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        facePassVM = ViewModelProvider(this)[FacePassVM::class.java]
        FaceSDKHelper.getInstance().initFaceSDK(this,facePassVM)
        facePassVM!!.facePath.observe(this){
            this.facePath = it
            binding.faceImg.setImageURI(Uri.parse(this.facePath))
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
        binding.btnCompleted.setOnClickListener {
            val intent = Intent()
            intent.putExtra("face_path",this.facePath)
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.btTryCapture.setOnClickListener {
            cameraManager?.getFacePass()?.openDetect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FaceSDKHelper.getInstance().closeCamera()
    }

}