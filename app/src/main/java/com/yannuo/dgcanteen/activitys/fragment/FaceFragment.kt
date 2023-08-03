package com.yannuo.dgcanteen.activitys.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.Navigator
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.FragmentFaceBinding
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.facepass.MyBitmapUtil
import com.yannuo.dgcanteen.facepass.RecognizeCallback
import com.yannuo.dgcanteen.model.OrderPayInfo
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
class FaceFragment : Fragment() {
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
    private var timeOutJob : Job ?= null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = FragmentFaceBinding.inflate(inflater, container, false)
        val job = SupervisorJob()
        scope = CoroutineScope(job + Dispatchers.IO)
        initFace()
        initData()
        initEvent()

        return binding.root
    }

    private fun initData() {
        timeOutJob = scope.launch {
            repeat(60){
                delay(1000)
                withContext(Dispatchers.Main){
                    binding.btCancel.text = "退出 ${it}s"
                }
            }
        }
    }


    private fun initFace() {
        binding.tvVersion.text = "V${requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName}"
        FaceHandler.getFaceHInstance().lock = true
        FaceHandler.getInstance()?.setPreviewDisplay(binding.preview)
        bitmapUtil = MyBitmapUtil(requireContext())
        mMyListener = MyListener()
        FaceHandler.getInstance()?.open(Rect(60,20,620,420), mMyListener)
    }


    private fun initEvent() {
        binding.btCancel.setOnClickListener {
            timeOutJob?.cancel()
            requireActivity().finish()
        }
        binding.btChangPayByIc.setOnClickListener {
            timeOutJob?.cancel()

            Navigation.findNavController(it).navigate(R.id.scanFragment)
        }

        binding.btChangPayByQr.setOnClickListener {
            timeOutJob?.cancel()

            Navigation.findNavController(it).navigate(R.id.scanFragment)
        }
    }


    override fun onStop() {
        release()
        super.onStop()

    }



//    override fun onClick(v: View) {
//        if (v.id == binding!!.btToScan.id) Navigation.findNavController(v).navigate(
//            FaceFragmentDirections.actionFaceToScan()
//        ) else if (v.id == binding!!.btToSuccess.id) Navigation.findNavController(v).navigate(
//            FaceFragmentDirections.actionFaceToSuccess()
//        ) else if (v.id == binding!!.btToFail.id) Navigation.findNavController(v).navigate(
//            FaceFragmentDirections.actionFaceToFail()
//        )
//    }



    fun release(){
        try {
            LogUtil.d(TAG,"release")
            scope.cancel()
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
//        override fun onRecognized(
//            cropBitmap : Bitmap,
//            byteArray: ByteArray,
//            rect: DoubleArray,
//            width: Int,
//            height: Int,
//            livenessThreshold :String,
//            livenessScore: String
//        ) {
//            try {
//                if (!able.get())return
//
//                //停止抓拍
//                FaceHandler.getInstance().setLiveness(false)
//
//                val catchBitmap = bitmapUtil.nv21ToBitmap(byteArray, width, height)
//                val ops = ByteArrayOutputStream()
//                catchBitmap.compress(Bitmap.CompressFormat.JPEG, 90, ops)
//                val picture = ops.toByteArray()
//                ops.flush()
//                ops.close()
//                LogUtil.d(TAG, "全景图片大小: ${picture.size}")
//                subscribe1 =  Observable.just(1)
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe {
//                        binding.cvCatchPhoto.setImageBitmap(cropBitmap)
//                        showBitmap = cropBitmap
//                    }
//
//            }catch (e : Exception){
//                e.printStackTrace()
//                LogUtil.e(TAG,e.message)
//            }
//        }

        override fun onRecognized(cropBitmap: Bitmap, token: String, livenessScore: Float) {
            try {
                //停止抓拍
                FaceHandler.getInstance().setLiveness(false)
                scope.launch {
                    withContext(Dispatchers.Main){
                        binding.cvCatchPhoto.setImageBitmap(cropBitmap)
                        showBitmap = cropBitmap
                    }
                }
                //todo 查找人员卡号，发起支付

            }catch (e : Exception){
                e.printStackTrace()
                FaceHandler.getInstance().setLiveness(true)
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