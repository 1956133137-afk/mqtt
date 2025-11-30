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
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FaceSDKHelper

class EnrollFaceActivity : BaseActivity<ActivityEnrollFaceBinding>() {
    private var facePath: String = ""

    override fun bindLayout() {
        binding = ActivityEnrollFaceBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    fun initObject(){
        val faceVM = ViewModelProvider(this)[FacePassVM()::class.java]
        FaceSDKHelper.getInstance().initFaceSDK(faceVM)
        faceVM.initAlgo()
        faceVM.facePath.observe(this){
            binding.faceImg.setImageURI(Uri.parse(it))
            facePath = it
        }
    }

    fun initEvent(){
        binding.returnBtn.setOnClickListener {
            val intent = Intent()
            intent.putExtra("face_path",this.facePath)
            setResult(RESULT_OK, intent)
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

    override fun onDestroy() {
        super.onDestroy()
        FaceSDKHelper.getInstance().closeCamera()
    }

}