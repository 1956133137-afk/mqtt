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
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import mcv.facepass.types.FacePassRecognitionResult
import org.greenrobot.eventbus.EventBus

class EnrollFaceActivity : BaseActivity<ActivityEnrollFaceBinding>() {
    private var facePath: String = ""
    private var cameraManager: CameraManager? = null
    private val myFaceResultListener by lazy { MyFaceResultListener() }

    private inner class MyFaceResultListener : FaceResultListener {
        override fun onInitFace(code: Int, message: String) {

        }

        override fun onPreView(data: ByteArray, width: Int, height: Int) {

        }

//        override fun onLiveness(path: String): Boolean {
//            facePath = path
//            binding.faceImg.setImageURI(Uri.parse(facePath))
//            return super.onLiveness(path)
//        }

        override fun onRecognized(result: FacePassRecognitionResult?, path: String) {
            facePath = path
            binding.faceImg.setImageURI(Uri.parse(facePath))
        }

        override fun onTips(msg: String) {

        }

        override fun onError(errCode: String, errMsg: String) {

        }

        override fun onCancel() {

        }

    }

    override fun bindLayout() {
        binding = ActivityEnrollFaceBinding.inflate(layoutInflater)
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