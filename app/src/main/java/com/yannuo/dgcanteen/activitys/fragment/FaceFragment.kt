package com.yannuo.dgcanteen.activitys.fragment

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.FragmentFaceBinding
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.facepass.MyBitmapUtil
import com.yannuo.dgcanteen.facepass.RecognizeCallback
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 */
class FaceFragment : Fragment(), View.OnClickListener {
    private val TAG = javaClass.simpleName
    private lateinit var binding: FragmentFaceBinding
    private var mMyListener: MyListener? = null
    private val able = AtomicBoolean(false)
    private lateinit var bitmapUtil : MyBitmapUtil
    private var subscribe :  Disposable?= null
    private var subscribe1 :  Disposable?= null
    private var subscribe2 :  Disposable?= null
    private var showBitmap : Bitmap ?= null
    private var lastShowTime = 0L
    private lateinit var scope : CoroutineScope


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = FragmentFaceBinding.inflate(inflater, container, false)
        val job = SupervisorJob()
        scope = CoroutineScope(job + Dispatchers.IO)
        initFace()
        initEvent()
        return binding.root
    }


    private fun initFace() {
        FaceHandler.getFaceHInstance().lock = true
        FaceHandler.getInstance()?.setPreviewDisplay(binding.preview)
        bitmapUtil = MyBitmapUtil(requireContext())
        mMyListener = MyListener()
        FaceHandler.getInstance()?.open(Rect(60,20,420,620), mMyListener)
    }


    private fun initEvent() {
//        binding!!.btToScan.setOnClickListener(this)
//        binding!!.btToSuccess.setOnClickListener(this)
//        binding!!.btToFail.setOnClickListener(this)
    }

    override fun onClick(v: View) {
//        if (v.id == binding!!.btToScan.id) Navigation.findNavController(v).navigate(
//            FaceFragmentDirections.actionFaceToScan()
//        ) else if (v.id == binding!!.btToSuccess.id) Navigation.findNavController(v).navigate(
//            FaceFragmentDirections.actionFaceToSuccess()
//        ) else if (v.id == binding!!.btToFail.id) Navigation.findNavController(v).navigate(
//            FaceFragmentDirections.actionFaceToFail()
//        )
    }


    fun release(){
        try {
        showBitmap?.also {
            if (it.isRecycled.not()) {
                it.recycle()
            }
        }
        FaceHandler.getFaceHInstance().lock = false
            subscribe?.dispose()
            subscribe1?.dispose()
            subscribe2?.dispose()
        }catch (e :Exception){
            e.printStackTrace()
        }
        FaceHandler.getInstance().closeCamera()
    }


    inner class MyListener : RecognizeCallback {
        override fun onRecognized(
            cropBitmap : Bitmap,
            byteArray: ByteArray,
            rect: DoubleArray,
            width: Int,
            height: Int,
            livenessThreshold :String,
            livenessScore: String
        ) {
            try {
                if (!able.get())return

                //停止抓拍
                FaceHandler.getInstance().setLiveness(false)

                val catchBitmap = bitmapUtil.nv21ToBitmap(byteArray, width, height)
                val ops = ByteArrayOutputStream()
                catchBitmap.compress(Bitmap.CompressFormat.JPEG, 90, ops)
                val picture = ops.toByteArray()
                ops.flush()
                ops.close()
                LogUtil.d(TAG, "全景图片大小: ${picture.size}")
                subscribe1 =  Observable.just(1)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        binding.cvCatchPhoto.setImageBitmap(cropBitmap)
                        showBitmap = cropBitmap
                    }

            }catch (e : Exception){
                e.printStackTrace()
                LogUtil.e(TAG,e.message)
            }
        }

        override fun onTips(msg: String) {
            if ((System.currentTimeMillis() - lastShowTime)  >= 200) {
                lastShowTime = System.currentTimeMillis()
                scope.launch {
                    withContext(Dispatchers.Main){
                        binding.tvDetectInfo.text = "$msg"
                    }
                }

            }
        }

        override fun onInitCode(code :Int ,message : String) {
            when (code) {
                Constant.NO_ERROR -> {

                }

                Constant.OPEN_CAMERA_ERROR_TYPE -> {

                }
            }
        }
    }

}