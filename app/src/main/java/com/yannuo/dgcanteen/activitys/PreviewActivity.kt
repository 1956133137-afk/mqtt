//package com.yannuo.dgcanteen.activitys
//
//import android.annotation.SuppressLint
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.Rect
//import android.os.Bundle
//import android.os.CountDownTimer
//import android.text.TextUtils
//import android.view.View
//import androidx.appcompat.app.AppCompatActivity
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import com.icbc.facepayment.deviceservice.utils.SpeakTTL
//import com.icbc.selfserviceticketing.deviceservice.facepass.RecognizeCallback
//import com.icbc.selfserviceticketing.deviceservice.utils.Constants
//import com.yannuo.dgcanteen.databinding.ActivityPreviewBinding
//import com.yannuo.dgcanteen.facepass.FaceHandler
//import com.yannuo.dgcanteen.facepass.MyBitmapUtil
//import com.yannuo.dgcanteen.util.LogUtil
//import io.reactivex.Observable
//import io.reactivex.android.schedulers.AndroidSchedulers
//import io.reactivex.disposables.Disposable
//import java.io.ByteArrayOutputStream
//import java.math.BigDecimal
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicBoolean
//
//
//class PreviewActivity : AppCompatActivity() {
//    private lateinit var mBinding : ActivityPreviewBinding
//    private var TAG = javaClass.simpleName
//    private var countDownTimer: CountDownTimer ?= null  //人脸检测倒计时
//    private var mMyListener: MyListener? = null
//    private lateinit var bitmapUtil : MyBitmapUtil
//    private lateinit var broadcast : MyBRReceiver
//    private val able = AtomicBoolean(false)
//    private var subscribe :  Disposable?= null
//    private var subscribe1 :  Disposable?= null
//    private var subscribe2 :  Disposable?= null
//    private var lastShowTime = 0L
//    //    private var longArray = LongArray(5)
//    private var diffTime = 1000
//    private var release = false//是否释放过资源,防止重复释放
//    private var hasInit = false
//    private var confirmFace = false //是否显示确认按钮，识别到人脸后，点击后才会退出页面
//    private var showBitmap : Bitmap ?= null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        mBinding = ActivityPreviewBinding.inflate(layoutInflater)
//        initSystemBar()
//        setContentView(mBinding.root)
//
//        configAndInit()
//        LogUtil.i(TAG,"进入人脸活检界面")
//
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (!hasInit){ //页面可见再加载
//            hasInit = true
//            broadcast =  MyBRReceiver()
//            val intentFilter = IntentFilter()
//            intentFilter.addAction(Constants.CLOSE_ACTION)
//            LocalBroadcastManager.getInstance(this).registerReceiver(broadcast,intentFilter)
//
//            mBinding.tvVersion.text = "v${packageManager.getPackageInfo(packageName, 0).versionName}"
//            initEvent()
//        }
//    }
//
//    /**
//     * 初始化算法参数
//     */
//    private fun configAndInit(){
//        val bundle = intent.extras
//        if (bundle != null) {
//            val timeOut = bundle.getInt(Constants.TIMEOUT_KEY, 60) //超时时间（单位： 秒） ， 默认 60s（如果值 <=0,则设置不超时）
//            val alive = bundle.getBoolean(Constants.ALIVE_ENABLE_KEY, false)  //活检控制
//            confirmFace = bundle.getBoolean(Constants.CONFIRM_FACE_KEY, false)  //活检控制
//            val voice = bundle.getBoolean(Constants.VOICE_PROMPT_KEY, false) //识别过程的语音提示开关， 默认 false 关闭
//            val volume = bundle.getInt(Constants.VOICE_VOLUME_KEY, 0) // 语音提示音量[0, 100]。 为 0 时表示 随终端的多媒体音量， 默认 0。
//            val titleText = bundle.getString(Constants.TITLE_TEXT_KEY, "") //在预览页面顶部的水平居中方向添加一个标题文本
//            val amount = bundle.getLong(Constants.AMOUNT_KEY, 0L) //金额(分)， 非空时在 UI 上显示金额
//
//            SpeakTTL.instance.setVolume(volume)
//            FaceHandler.getInstance().setControlParams(alive,voice)
//            //判断是否显示金额
//            if (amount > 0 ){
//                mBinding.tvSpendMoney.visibility = View.VISIBLE
//                var bigDecimal = BigDecimal(amount)
//                bigDecimal = bigDecimal.divide(BigDecimal(100))
//                mBinding.tvSpendMoney.text = String.format("金额(元)： ￥%.2f",bigDecimal.setScale(2).toFloat())
//            }
//
//            if (timeOut > 0){
//                mBinding.tvDownTime.visibility = View.VISIBLE
//                countDownTimer?.cancel()
//                countDownTimer = object : CountDownTimer(timeOut *1000L +300,1000) {
//                    override fun onTick(mil: Long) {
//                        mBinding.tvDownTime.text = resources.getString(R.string.str_timeout,mil /1000)
//                    }
//
//                    override fun onFinish() {
//                        mBinding.tvDownTime.text = "人脸检测超时"
//                        failCallback(Constants.TIMEOUT_ERROR_TYPE,"人脸检测超时" )
//                        finish()
//                    }
//                }
//            }
//            if (!TextUtils.isEmpty(titleText)) {
//                mBinding.tvTitle.text = titleText
//            }
//
//            FaceHandler.getInstance().setPreviewDisplay(mBinding.preview)
//            bitmapUtil = MyBitmapUtil(this)
//            mMyListener = MyListener()
//            FaceHandler.getInstance().open(Rect(60,20,420,620), mMyListener)
//        }else finish()
//    }
//
//
//    private fun initSystemBar() {
//        val window = window
//        val uiOption = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //                |View.SYSTEM_UI_FLAG_IMMERSIVE
//                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                or View.SYSTEM_UI_FLAG_FULLSCREEN)
//
//        window.statusBarColor = Color.TRANSPARENT
//        window.navigationBarColor = Color.TRANSPARENT
//        window.decorView.systemUiVisibility = uiOption
////        supportActionBar?.hide()
//
//    }
//
//
//    private fun initEvent(){
////        mBinding.preview.setOnClickListener {
////            System.arraycopy(longArray,1,longArray,0,longArray.size -1)
////            longArray[longArray.size -1] = System.currentTimeMillis()
////            val diff =  longArray.get(longArray.size -1) - longArray.get(0)
////            if (diff <= diffTime){
////                MyService(this).rebootSystem()
////                finish()
////            }
////        }
//
//        mBinding.btSure.setOnClickListener {
//            mBinding.btSure.isEnabled = false
//            finish()
//        }
//    }
//
//    inner class MyListener : RecognizeCallback {
//        @SuppressLint("CheckResult")
//        override fun onRecognized(
//            cropBitmap :Bitmap,
//            byteArray: ByteArray,
//            rect: DoubleArray,
//            width: Int,
//            height: Int,
//            livenessThreshold :String,
//            livenessScore: String
//        ) {
//            try {
//                if (!able.get())return
//                countDownTimer?.cancel()
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
////                        if (mBinding.cvLaylPic.visibility == View.GONE)
////                            mBinding.cvLaylPic.visibility = View.VISIBLE
//                        mBinding.cvCatchPhoto.setImageBitmap(cropBitmap)
//                        showBitmap = cropBitmap
//                        if (confirmFace) {   //显示确认退出按钮
//                            mBinding.btSure.visibility = View.VISIBLE
//                        } else {
//                            subscribe2 = Observable.timer(700, TimeUnit.MILLISECONDS)
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe {
////                                    mBinding.cvLaylPic.visibility = View.GONE
////                                    if (cropBitmap.isRecycled.not()) {
////                                        cropBitmap.recycle()
////                                    }
//                                    LogUtil.i(TAG, "人脸展示结束，结束界面释放资源")
//                                    finish()
//                                }
//                        }
//                    }
//
//                successCallback(picture, rect, livenessThreshold, livenessScore)
//
//            }catch (e : Exception){
//                e.printStackTrace()
//                LogUtil.e(TAG,e.message)
//                failCallback(Constants.CATCH_ERROR_TYPE,"处理抓拍人脸出错")
//            }
//        }
//
//        override fun onTips(msg: String) {
//            if ((System.currentTimeMillis() - lastShowTime)  >= 200) {
//                lastShowTime = System.currentTimeMillis()
//                runOnUiThread {
//                    mBinding.tvDetectInfo.text = "$msg"
//                }
//            }
//        }
//
//        override fun onInitCode(code :Int ,message : String) {
//            when(code){
//                Constants.NO_ERROR_TYPE ->{
//                    subscribe  = Observable.timer(1, TimeUnit.SECONDS).subscribe {
//                        countDownTimer?.start()
//                        able.set(true)
//                    }
//                }
//                Constants.OPEN_CAMERA_ERROR_TYPE -> {
//                    failCallback(code , message)
//                    finish()
//                }
//            }
//        }
//    }
//
//    override fun onStop() {
//        release()
//        super.onStop()
//    }
//
//    fun release(){
//        if (release) return
//        showBitmap?.also {
//            if (it.isRecycled.not()) {
//                it.recycle()
//            }
//        }
//        FaceHandler.getFaceHInstance().lock = false
//        release = true
//        try {
//            countDownTimer?.cancel()
//            subscribe?.dispose()
//            subscribe1?.dispose()
//            subscribe2?.dispose()
//        }catch (e :Exception){
//            e.printStackTrace()
//        }
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
//        FaceHandler.getInstance().closeCamera()
//    }
//
//    override fun onDestroy() {
//        LogUtil.i(TAG,"退出人脸活检界面")
//        super.onDestroy()
//    }
//
//    override fun onBackPressed() {}
//
//    /**
//     * 错误结果回调给调用app
//     * @param code Int
//     * @param message String
//     */
//    private fun failCallback(code :Int, message :String){
//        FaceHandler.getFaceHInstance().onFaceDetectFail(code,message)
//    }
//
//    private fun successCallback(imagedata :ByteArray, faceRect :DoubleArray, livenessThreshold: String, livenessScore  :String){
//        val bundle = Bundle()
//        bundle.putByteArray(Constants.IMAGEDATA_KEY,imagedata)
//        bundle.putDoubleArray(Constants.FACERECT_KEY,faceRect)
//        bundle.putString(Constants.FACESCORE_KEY,livenessScore)
//        val buffer = StringBuffer(50)
//        buffer.append("000") //1.算法厂商标识码（ 3 字节）
//        buffer.append("02") //2.活检方式（ 2 字节）
//        buffer.append("A") //3.活检算法能力（ 1 字节）
//        buffer.append("FacePass-90-V3.10.3 ") //4.活检算法信息（ 20 字节）
//        buffer.append("裕相YX-N7718-P2       ") //5.摄像头信息（ 20 字节） ： 摄像头厂商、 型号， 应与算法检测时相关信息一致， 不足为以空格填充
//        buffer.append(livenessThreshold)//6.活检通过阈值（2 字节） ：取值范围: 00-99(算法为受理终端配置的活检通过分值)
//        buffer.append(livenessScore)//7.活检评分（2 字节） ： 取值范围: 00-99， 如实际评分为 100， 以 99 上送(每次识别时实际活检评分)
//        bundle.putString(Constants.ALIVEINFO_KEY,buffer.toString())
//        FaceHandler.getFaceHInstance().onFaceDetectSuccess(bundle)
//    }
//
//
//    //    /**
////     * 关闭预览广播监听
////     */
//    inner class MyBRReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            val action = intent.action
//            if (!TextUtils.isEmpty(action) && Constants.CLOSE_ACTION.equals(action)){
//                release()
//                LogUtil.d(TAG,"关闭预览页面")
//                finish()
//            }
//        }
//    }
//
//}