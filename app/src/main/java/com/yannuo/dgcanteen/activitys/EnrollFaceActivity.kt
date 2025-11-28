package com.yannuo.dgcanteen.activitys

import android.content.Intent
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.databinding.ActivityEnrollFaceBinding
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FaceSDKHelper

class EnrollFaceActivity : BaseActivity<ActivityEnrollFaceBinding>(),FacePassVM.RecognizeResListener {
    private val faceVM: FacePassVM = FacePassVM.instance
    private var facePath: String = ""
    private val faceHelper = FaceSDKHelper.getInstance()

    override fun bindLayout() {
        binding = ActivityEnrollFaceBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    fun initObject(){
        FacePassVM.instance.initAlgo()
        faceVM.setUploadListener(this)
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
                faceHelper.openCamera(rect, binding.faceView)
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
        //活检人脸图片
        this.facePath = path
        binding.faceImg.setImageURI(Uri.parse(this.facePath))
    }

    override fun onSuccessful(token: String, imgBase64: String, searchScore: Float) {

    }

    override fun onFailed(msg: String) {

    }

}