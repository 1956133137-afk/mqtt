package com.yannuo.dgcanteen.facepass


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Environment
import android.text.TextUtils

import com.my.yuvconvert.YuvUtil
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.facepass.*
import com.yannuo.dgcanteen.model.FaceDataBean
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mcv.facepass.FacePassException
import mcv.facepass.FacePassHandler
import mcv.facepass.types.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlin.math.abs


class FacePass(context: Context) : CameraDataStream.CameraListener {
    private val TAG = "FacePass"
    private var mScope:CoroutineScope
    private var mFacePassHandler : FacePassHandler? =null   //SDK实例
    private var mContext = context
    private var colorCameraChannel :Channel<CameraPreviewData>? = null   //彩色原始数据
    private var blackWhiteCameraChannel :Channel<CameraPreviewData> ?= null //黑白原始数据
    private var initializeResult = -1  //算法初始化结果（-1（未初始化），0（初始化成功），1（初始化出错））
    private var rect :Rect ?= null      //人脸识别区域
    private var isFreedInterrupt = false
    private var cameraWidth = 0
    private var cameraHeight = 0
    private var listener : RecognizeCallback?= null
    private var preViewChannel :Channel<CameraPreviewData>? = null
    //    private var livenessChannel :Channel<FacePassDetectionResult>? = null
    private var livenessChannel :Channel<FaceDataBean>? = null
    @Volatile private var detect = true   //默认开启检测人体
    private var minFaceThreshold = 125
    private var preViewRotation = 0
    private var mirror = false
    private var bitmapUtil : MyBitmapUtil
    @Volatile private var clearHint  = false  //相机清楚提示
    @Volatile private var voiceHit  = true    //是否声音提示
    @Volatile private var isMultiFace  = false //是否支持多人入境
    @Volatile private var protection = false  //掉线保护默认关闭
    @Volatile private var live = true //是否开启活检
    private var boardService : MyService  ?= null
    private var delayTime = 3000L


    private var configJob : Job ?= null
    private var converJob : Job ?= null
    private var livenessJob : Job ?= null
    private var feedJob : Job ?= null
    private var dieJob : Job ?= null
    private var yuvUtil: YuvUtil? = null


    private var roll = 10f //人脸旋转角度
    private var pitch = 10f  //人脸俯仰角
    private var yaw = 10f   //偏航角

    private var searchThreshold = 70f
    private var livenessThreshold = 70f
    private var blurThreshold = 0.2f
    private var lowBrightnessThreshold = 70f
    private var handler :CoroutineExceptionHandler
    private var sound = true
//    private var currentTime = AtomicLong(0)



    init {
        mScope = CoroutineScope(Dispatchers.Default)
        colorCameraChannel = Channel(2)
        blackWhiteCameraChannel = Channel(2)
        preViewChannel = Channel(2)
        livenessChannel = Channel(1)
        bitmapUtil = MyBitmapUtil(context)
        yuvUtil = YuvUtil()
        handler = CoroutineExceptionHandler { _, exception ->
            LogUtil.e(TAG,"CoroutineExceptionHandler got $exception")
        }

    }


    /**
     * 传递相机的预览分辨率，用于计算人脸检测区域
     *
     */
    fun setCameraPreSize(colorPreViewWidth: Int, colorPreViewHeight: Int) {
        this.cameraWidth =colorPreViewWidth
        this.cameraHeight = colorPreViewHeight
    }


    /**
     * 活检回调
     */
    fun setListener(listener: RecognizeCallback?){
        this.listener = listener
    }


    /**
     * 活体检测区域
     */
    fun setRect(rect : Rect){
        this.rect = rect
    }

    /**
     * 开关活体,控制逻辑，不是控制算法本身
     */
    fun setLiveness(enable : Boolean){
        detect = enable
        when(enable){
            true ->  {
                LogUtil.d(TAG,"开启人体检测")
            }
            false -> {
                LogUtil.d(TAG,"关闭人体检测")
            }
        }
    }



    /**
     * 设置最小人脸大小识别参数
     *[0-512]
     */
    fun setFaceMinThreshold(threshold : Int){
        if (threshold<0 || threshold>512)return
        minFaceThreshold = threshold
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前 识别距离")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.faceMinThreshold = threshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后 识别距离")
            }
        }
    }


    /**
     * 设置人脸识别三维角度[0,90]
     * roll 旋转角度
     * pitch 垂直角度
     * yaw  水平角度
     *
     */
    fun setFacePose(roll : Float , pitch :Float ,yaw :Float){
        if (roll<0 || roll>90 || pitch <0 || pitch>90 || yaw<0 || yaw>90)return
        this.roll = roll
        this.pitch = pitch
        this.yaw = yaw
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前 设置人脸识别三维角度")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.poseThreshold =  FacePassPose(roll, pitch, yaw)
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后 设置人脸识别三维角度")
            }
        }
    }


    /**
     * 设置人脸识别分数
     *
     */
    fun setFaceSearchThreshold(searchThreshold :Float){
        if (searchThreshold<0 || searchThreshold>100)return
        this.searchThreshold  = searchThreshold
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前 设置人脸识别分数")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.searchThreshold =  searchThreshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后 设置人脸识别分数")
            }
        }
    }

    /**
     * 设置人脸活检分数
     *
     */
    fun setFaceLivenessThreshold(livenessThreshold :Float){
        if (livenessThreshold<0 || livenessThreshold>100)return
        this.livenessThreshold  = livenessThreshold
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前 设置人脸活检分数")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.livenessThreshold =  livenessThreshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后 设置人脸活检分数")
            }
        }
    }


    /**
     * 设置人脸模糊度0 ~1  ,default 0.3f
     *
     */
    fun setFaceBlurThreshold(blurThreshold :Float){
        if (blurThreshold<0 || blurThreshold>1)return
        val value = this.blurThreshold + blurThreshold
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前 设置人脸模糊度 $value")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.blurThreshold =  value
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后 设置人脸模糊度 $value")
            }
        }
    }

    /**
     * 设置人脸算法活检开关
     */
    private  fun setFaceLive(alive :Boolean){
        if (live.equals(alive))return
        live = alive
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前,设置开启活检 $live")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.rgbIrLivenessEnabled =  live
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后,设置开启活检 $live")
            }
        }
    }




    /**
     * 设置人脸识别亮度  ,default 70f
     *
     */
    fun setFaceIlluminationThreshold(illumination :Float){
        if (illumination <0 || illumination > 200)return
        this.lowBrightnessThreshold  = illumination
        when(initializeResult){
            -1 ->  {
                LogUtil.d(TAG,"未初始化前 设置人脸识别亮度 $lowBrightnessThreshold")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.lowBrightnessThreshold =  lowBrightnessThreshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG,"初始化后 设置人脸识别亮度 $lowBrightnessThreshold")
            }
        }
    }


    /**
     *
     */
    fun config(callback : SDKInitResult?){
        configJob =  mScope.launch(handler) {
            release()
            FacePassHandler.initSDK(mContext)
            LogUtil.i(TAG,"start initSDK ${Thread.currentThread().name}")
            val mv = MMKV.defaultMMKV()
            sound = mv.decodeBool(Constant.VOICE_ENABLE_SET,Constant.VOICE_ENABLE_SET_V)
            while (isActive) {
                while (FacePassHandler.isAvailable()) {
                    var message =""
                    try {
                        val config = FacePassConfig()
                        config.poseBlurModel = FacePassModel.initModel(mContext.assets, "attr.pose_blur.arm.190630.bin")
//                   config.livenessModel = FacePassModel.initModel(MyApplication.applicationContext.getAssets(), "liveness.CPU.rgb.G.bin");
//                     config.livenessModel = FacePassModel.initModel(MyApplication.applicationContext.getAssets(), "liveness.CPU.rgb.G.bin");
                        config.rgbIrLivenessModel =
                            FacePassModel.initModel(mContext.assets, "liveness.CPU.rgbir.G.bin")
                        config.searchModel = FacePassModel.initModel(mContext.assets, "feat2.arm.J2.v1.0_1core.bin")
                        config.detectModel =
                            FacePassModel.initModel(mContext.assets, "detector.arm.G.bin")
                        config.landmarkModel =
                            FacePassModel.initModel(mContext.assets, "pf.lmk.arm.E.bin")
                        config.detectRectModel =
                            FacePassModel.initModel(mContext.assets, "detector_rect.arm.G.bin")
                        config.rcAttributeModel =
                            FacePassModel.initModel(mContext.assets, "attr.RC.arm.E.bin")
                        config.occlusionFilterModel = FacePassModel.initModel(mContext.assets, "attr.occlusion.arm.20201209.bin")

                        //识别阈值
                        config.searchThreshold = searchThreshold
                        //活体阈值
                        //活体阈值
                        livenessThreshold = mv.decodeFloat(Constant.LIVE_VALUE_SET,Constant.LIVE_VALUE_SET_V) //活检阈值
                        config.livenessThreshold = livenessThreshold
                        //活体开关
                        config.livenessEnabled = false
                        //红外活体开关
                        config.rgbIrLivenessEnabled = mv.decodeBool(Constant.LIVE_ENABLE_SET,Constant.LIVE_ENABLE_SET_V) //活检开关
                        //遮挡不给入库和不送识别
                        config.rcAttributeAndOcclusionMode = 2
                        //识别距离阈值
                        minFaceThreshold = when(mv.decodeFloat(Constant.DISTANCE_SET,Constant.DISTANCE_SET_V)){
                            1f -> 70
                            0.9f -> 80
                            0.8f -> 90
                            0.7f -> 100
                            0.6f -> 110
                            0.5f -> 120
                            else -> 100
                        }
                        minFaceThreshold = 80
                        config.faceMinThreshold = minFaceThreshold
                        config.poseThreshold = FacePassPose(this@FacePass.roll, this@FacePass.pitch, this@FacePass.yaw)

                        config.blurThreshold = blurThreshold
                        config.lowBrightnessThreshold = lowBrightnessThreshold
                        config.highBrightnessThreshold = 210f
                        config.brightnessSTDThreshold = 80f
                        config.retryCount = 100
                        config.smileEnabled = false
                        config.maxFaceEnabled = false
                        config.fileRootPath = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                        /* 创建SDK实例 */

                        mFacePassHandler = FacePassHandler(config)
                        LogUtil.i(TAG, "算法模型配置成功...")
                        initializeResult = 0
                        message="算法模型配置成功"
//                        listener?.onInitSuccess()
//                        checkDie()
                        return@launch
                    } catch (e: Exception) {
                        e.printStackTrace()
                        initializeResult = 1
                        message="相机算法初始化失败"
                        LogUtil.e(TAG, "相机算法初始化失败...")
                        return@launch
                    }finally {
                        callback?.faceInitResult(initializeResult,message)
                    }
                }

                delay(100)
            }
        }
    }


    fun setDectParams(alive : Boolean, voice :Boolean){
        voiceHit = voice
        setFaceLive(alive)
    }


    fun feedFrame(){
        detect = true
//        protection = true
//        currentTime.set(System.currentTimeMillis()) //此刻开始计时
        if (boardService == null) boardService = MyService(mContext)
        checkLiveness()
        feedJob = mScope.launch(handler) {
            var lastTime = 0L
            while (!isFreedInterrupt && (initializeResult !=1) && isActive) {
                if (mFacePassHandler == null) {
                    delay(50)
                    continue
                }
                val colorReceive = colorCameraChannel!!.receive()
                val blackWhiteReceive = blackWhiteCameraChannel!!.receive()
//                currentTime.set(System.currentTimeMillis())

                var colorImage : FacePassImage
                var blackWhiteImage : FacePassImage
                try {
                    colorImage = FacePassImage(colorReceive.nv21Data,colorReceive.width, colorReceive.height,colorReceive.rotation,FacePassImageType.NV21)
                    blackWhiteImage = FacePassImage(blackWhiteReceive.nv21Data,blackWhiteReceive.width, blackWhiteReceive.height,blackWhiteReceive.rotation,FacePassImageType.NV21)
                    if (preViewRotation != blackWhiteReceive.preRotation) preViewRotation = blackWhiteReceive.preRotation
                    if (mirror != blackWhiteReceive.mirror) mirror = blackWhiteReceive.mirror
                }catch (e :FacePassException) {
                    e.printStackTrace()
                    continue
                }
                try {
                    val detectionResult = mFacePassHandler!!.feedFrameRGBIR(colorImage, blackWhiteImage)
                    clearHit(lastTime)
                    if (detectionResult != null && detectionResult.faceList.isNotEmpty()) {
                        LogUtil.d(TAG,"此帧人脸数  ${ detectionResult.faceList.size}")
                        val dataBean = FaceDataBean()
                        dataBean.detectionResult = detectionResult
                        dataBean.colorReceive = colorReceive
                        //计算出最大的人脸下标
                        calculatorMax(dataBean, colorReceive.rotation, mirror)
                        val check = check(detectionResult.faceList[dataBean.index], lastTime,
                            dataBean.rect, dataBean.detectionResult.faceList.size)
                        if (check != -1L)lastTime = check

                        LogUtil.i(TAG,"实际检测出人脸数：${dataBean.detectionResult.faceList.size}  $isMultiFace")
                        if( !isMultiFace && dataBean.detectionResult.faceList.size > 1){
                            LogUtil.i(TAG,"不合格：多人脸")
                            for (bean in dataBean.detectionResult.faceList) {
                                mFacePassHandler?.setMessage(bean.trackId, FacePassTrackIdState.TRACK_ID_RETRY)
                            }
                        }
                        else if (detectionResult.message.size != 0) {
                            livenessChannel?.offer(dataBean)
                        }
                    }
                }catch (e : Exception){
                    LogUtil.e(TAG,"人脸帧处理过程出错  ${e.message}")
                }
            }
        }
    }

    private fun check(face: FacePassFace,time :Long,rect :RectF?,faceCount :Int) :Long{
        //检测是否遮挡
        val lmkoccsta = lmkoccsta(face)
        var hit =""
        LogUtil.d(TAG, "人脸是否有遮挡：$lmkoccsta")
        if (faceCount > 1){
            if (!isMultiFace){
                hit = "拍摄不支持多人入镜"
                clearHint = true
                LogUtil.d(TAG,hit)
                listener?.onTips(hit)
                if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                    CommonAndDpToPxUtil.speakWork(hit)
                    return System.currentTimeMillis()
                }
            }
        }
        else if (lmkoccsta){
            hit = "请不要遮挡脸部"
            clearHint = true
            LogUtil.d(TAG,hit)
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }
        //检测人脸角度
        val pose = face.pose
        if (pose.roll < (-roll) || (pose.roll > roll)){
            hit = "请不要侧头"
            LogUtil.d(TAG,hit)
            clearHint = true
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }

        else if (pose.pitch < (-pitch)){
            hit = "请稍微低头"
            LogUtil.d(TAG,hit)
            clearHint = true
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }else if (pose.pitch > pitch){
            hit = "请稍微抬头"
            clearHint = true
            LogUtil.d(TAG,hit)
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }
        else if (pose.yaw < (-yaw)){
            hit = "请向右稍微调整人脸"
            clearHint = true
            LogUtil.d(TAG,hit)
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }else if (pose.yaw > yaw){
            hit = "请向左稍微调整人脸"
            clearHint = true
            LogUtil.d(TAG,hit)
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }else if (rect != null){
            if (abs(rect.right - rect.left)<100){
                hit = "请靠近"
                clearHint = true
                LogUtil.d(TAG,hit)
                listener?.onTips(hit)
                if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                    CommonAndDpToPxUtil.speakWork(hit)
                    return System.currentTimeMillis()
                }
            }
        }
        else{
            hit = "请在有效区域内识别"
            clearHint = true
            LogUtil.d(TAG,hit)
            listener?.onTips(hit)
            if (sound && voiceHit && (System.currentTimeMillis() - time)>delayTime){
                CommonAndDpToPxUtil.speakWork(hit)
                return System.currentTimeMillis()
            }
        }
        return -1L
    }

    private fun  clearHit(time: Long) {
        if (clearHint && (System.currentTimeMillis() - time) > delayTime){
            clearHint = false
            listener?.onTips("")
        }
    }

//    private  fun checkDie(){
//        dieJob =  mScope.launch(handler) {
//            var interval = 0L
//            while (isActive) {
//                delay(30000)
//                if (protection) {
//                    interval = System.currentTimeMillis() - currentTime.get()
//                    if ((interval >= 40000) && (currentTime.get() != 0L) ) {
//                        LogUtil.e(TAG, "异常页面卡")
//                        boardService?.rebootSystem()
//                    }
//                }else{
//                    if(currentTime.get() == 0L)
//                        continue
//                    currentTime.set(0)
//                }
//            }
//        }
//    }
    /**
     * 相机帧质量通过处理
     */
    private fun checkLiveness(){
        livenessJob = mScope.launch(handler) {
            while (isActive) {
                try {
                    val faceData = livenessChannel!!.receive()
                    if (!detect)continue
                    if (faceData.rect != null && ( isMultiFace || faceData.detectionResult.faceList.size < 2) ){
                        val livenessClassify = mFacePassHandler!!.livenessClassify(faceData.detectionResult.message)
                        if (livenessClassify != null && livenessClassify.isNotEmpty()) {
                            for (bean in livenessClassify){
                                if (bean.trackId != faceData.trackId)continue
                                LogUtil.i(TAG, "活体阈值 -> ${bean.livenessThreshold} " + "活体识别分数 ->  ${bean.livenessScore}   FacePassLivenessState--> ${bean.livenessState}")
                                when (bean.livenessState) {
                                    0 -> {
                                        val facePassImage = faceData.detectionResult.images[faceData.index]
                                        var cropBitmap = bitmapUtil.nv21ToBitmap(
                                            facePassImage.image,
                                            facePassImage.width,
                                            facePassImage.height
                                        )
                                        val matrix = Matrix()
                                        matrix.setRotate(faceData.colorReceive!!.preRotation.toFloat())
                                        cropBitmap = Bitmap.createBitmap(
                                            cropBitmap, 0, 0, facePassImage.width,
                                            facePassImage.height, matrix, false
                                        )
                                        val ruslt = ByteArray(faceData.colorReceive!!.nv21Data.size)
                                        yuvUtil!!.yuvCompress(faceData.colorReceive!!.nv21Data, faceData.colorReceive!!.width,
                                            faceData.colorReceive!!.height, ruslt, 1f, 1f, 0,
                                            preViewRotation, mirror)
                                        var width = faceData.colorReceive!!.width
                                        var height = faceData.colorReceive!!.height
                                        if (preViewRotation == 90 || preViewRotation == 270) {
                                            width = faceData.colorReceive!!.height
                                            height = faceData.colorReceive!!.width
                                        }

                                        LogUtil.i(TAG,"detect :${detect} listener:${listener}")
                                        if (detect) {
                                            val doubleArray = doubleArrayOf(
                                                faceData.rect!!.left.toDouble(), faceData.rect!!.top.toDouble(),
                                                faceData.rect!!.right.toDouble(), faceData.rect!!.bottom.toDouble()
                                            )
                                            listener?.onRecognized(
                                                cropBitmap, ruslt, doubleArray, width, height,
                                                String.format("%02d", bean.livenessThreshold.toInt()),
                                                String.format("%02d", bean.livenessScore.toInt()))
                                        }
                                    }
                                    else -> {
                                        //活检失败
                                        val message = when (bean.livenessErrorCode) {
                                            1 -> "可能是irDetect没有检测到人脸、overlapScore小于阈值或者pf小于阈值"
                                            2 -> "识别活体分数小于阈值"
                                            else -> "未知错误"
                                        }
                                        LogUtil.e(TAG, "${bean.livenessErrorCode},message:${message}")
                                        if (detect) listener?.onTips(bean.livenessErrorCode.toString() + message)
                                    }
                                }
                            }
                        }
                    }
                    else {
                        try {
                            mFacePassHandler!!.livenessClassify(faceData.detectionResult.message)
                            LogUtil.d(TAG, "不在活检有效区域：${faceData.rect} ${isMultiFace}  ${faceData.detectionResult.faceList.size}")
                            if (detect) listener?.onTips("不在活检有效区域或不支持多人识别")
                        }catch (e :Exception){
                            LogUtil.e(TAG,e.message)
                        }
                    }
                    for (bean in faceData.detectionResult.faceList) {
                        mFacePassHandler?.setMessage(bean.trackId, FacePassTrackIdState.TRACK_ID_RETRY)
                    }
                }catch (e :Exception){
                    LogUtil.e(TAG, "识别处理异常 ${e.message}")
                }
            }
        }
    }


    /**
     * 裁剪图片保存在指定目录
     * 返回绝对路径
     */
    private fun savePicture(facePassImage: FacePassImage):String?{
        //格式转换
        var result : String ?= null
        var fileOutputStream :FileOutputStream? = null
        try {
            val catchBitmap = bitmapUtil.nv21ToBitmap(
                facePassImage.image,
                facePassImage.width,
                facePassImage.height
            )
            val matrix = Matrix()
            matrix.postRotate(preViewRotation.toFloat())
            val rotationBitmap = Bitmap.createBitmap(
                catchBitmap,
                0,
                0,
                catchBitmap.width,
                catchBitmap.height,
                matrix,
                true
            )
            if (!catchBitmap.isRecycled)catchBitmap.recycle()
            val file = mContext.getDir("face", Context.MODE_PRIVATE)
            val fileName = SimpleDateFormat("yyyyMMddHHmmssS").format(System.currentTimeMillis()).toString() + ".jpeg"
            val filePath = File(file, fileName)
            if (!filePath.exists()) {
                file.mkdirs()
                filePath.createNewFile()
            }
            fileOutputStream = FileOutputStream(filePath)

            rotationBitmap.compress(Bitmap.CompressFormat.JPEG, 90,fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            if (!rotationBitmap.isRecycled)rotationBitmap.recycle()
            LogUtil.i(TAG,"absolutePath --${filePath.absolutePath}")
            result = filePath.absolutePath
        }catch (e :Exception){
            LogUtil.e(TAG,"文件保存中出错"+e.printStackTrace())
            fileOutputStream?.close()
        }
        finally {
            return result
        }
    }


    /**
     * NV21格式转Bitmap预览帧
     * 带转角和镜像
     */
    private fun convert(){
        converJob = mScope.launch {
            var ruslt = ByteArray(0)
            var colorReceive : CameraPreviewData
            while (isActive) {
                try {
                    colorReceive = preViewChannel!!.receive()
                    val start =  System.currentTimeMillis()
                    if (ruslt.size != colorReceive.nv21Data.size)
                        ruslt = ByteArray(colorReceive.nv21Data.size)
                    yuvUtil!!.yuvCompress(colorReceive.nv21Data,colorReceive.width,colorReceive.height,ruslt,
                        1f,1f,0,preViewRotation,mirror)
                    var width = colorReceive.width
                    var height = colorReceive.height
                    if (preViewRotation == 90 || preViewRotation == 270 )
                    {
                        width = colorReceive.height
                        height = colorReceive.width
                    }
                    LogUtil.d(TAG, "预览帧旋转镜像处理耗时：${System.currentTimeMillis() - start} ms")
                }catch (e :Exception){
                    LogUtil.e(TAG, "预览帧处理异常  ${e.message}")
                }
            }
        }
    }

    /**
     * 关闭协程
     */
    fun closeJob(){
//        converJob?.cancel()
//        dieJob?.cancel()
        feedJob?.cancel()
        livenessJob?.cancel()

//        protection = false
//        mScope.cancel()
        LogUtil.i(TAG,"协程关闭")
//        Observable.timer(50,TimeUnit.MILLISECONDS).subscribe {
//        boardService?.setBGDStatus(1,0)  //关闭补光灯
//        }

    }

    fun release(){
        mFacePassHandler?.release()
        boardService = null
//        dieJob?.cancel()
    }

    /**
     * 判断人脸是否遮挡
     */
    private fun lmkoccsta(face: FacePassFace) :Boolean{
        return face.facePassFeedFrameErrorCode == 7
    }

    /**
     * 计算最大索引
     * 返回索引
     */
    private fun calculatorMax(faceDataBean: FaceDataBean, preRotation: Int, mirror: Boolean){
        var left = 0f
        var top = 0f
        var right = 0f
        var bottom = 0f
        var mat :Matrix
        var maxWidth = 0f
        for ((index, face) in faceDataBean.detectionResult.faceList.withIndex()){
            mat = Matrix()
            when(preRotation){
                0 ->{
                    left = face.rect.left.toFloat()
                    top = face.rect.top.toFloat()
                    right = face.rect.right.toFloat()
                    bottom = face.rect.bottom.toFloat()
                    mat.setScale((if (mirror) -1f else 1.toFloat()), 1f)
                    mat.postTranslate(if (mirror) cameraWidth.toFloat() else 0f, 0f)
                }
                90 ->{
                    mat.setScale(if (mirror) -1f else 1.toFloat(), 1f)
                    mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                    left = face.rect.top.toFloat()
                    top = cameraWidth - face.rect.right.toFloat()
                    right = face.rect.bottom.toFloat()
                    bottom = cameraWidth - face.rect.left.toFloat()
                }
                180 ->{
                    mat.setScale(1f, if (mirror) -1f else 1.toFloat())
                    mat.postTranslate(0f, if (mirror) cameraHeight.toFloat() else 0f)
                    left = face.rect.right.toFloat()
                    top = face.rect.bottom.toFloat()
                    right = face.rect.left.toFloat()
                    bottom = face.rect.top.toFloat()
                }
                270 ->{
                    mat.setScale(if (mirror) -1f else 1.toFloat(), 1f)
                    mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                    left = cameraHeight - face.rect.bottom.toFloat()
                    top = face.rect.left.toFloat()
                    right = cameraHeight - face.rect.top.toFloat()
                    bottom = face.rect.right.toFloat()
                }
            }
            val drect = RectF()
            val srect = RectF(left, top, right, bottom)
            mat.mapRect(drect, srect)
            val absValue = abs(drect.right - drect.left)
            if (absValue > maxWidth){
                maxWidth = absValue
                faceDataBean.index = index
                faceDataBean.trackId = face.trackId
                var region = false
                if (rect == null){
                    faceDataBean.rect = drect
                }
                else{
                    if(drect.left >= rect!!.left && drect.top >= rect!!.top && drect.right <= rect!!.right && drect.bottom <=rect!!.bottom) {
                        faceDataBean.rect = drect
                        region = true
                    }

                }
                //判断是否在识别区域内
                LogUtil.i(TAG," \n有效区域:${rect?.left},${rect?.top},${rect?.right},${rect?.bottom}\n" +
                        "当前区域:${drect.left},${drect.top},${drect.right},${drect.bottom}  result: $region")
            }
        }
    }

    /**
     * 判断人脸是否在区域
     * 在 true ;不在 false
     */
    private fun judgeArea(face: FacePassFace, preRotation: Int, mirror: Boolean):RectF?{
        if (rect == null)return null
        val mat = Matrix()
        var left = 0f
        var top = 0f
        var right = 0f
        var bottom = 0f
        when(preRotation){
            0 ->{
                left = face.rect.left.toFloat()
                top = face.rect.top.toFloat()
                right = face.rect.right.toFloat()
                bottom = face.rect.bottom.toFloat()
                mat.setScale((if (mirror) -1f else 1.toFloat()), 1f)
                mat.postTranslate(if (mirror) cameraWidth.toFloat() else 0f, 0f)
            }
            90 ->{
                mat.setScale(if (mirror) -1f else 1.toFloat(), 1f)
                mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                left = face.rect.top.toFloat()
                top = cameraWidth - face.rect.right.toFloat()
                right = face.rect.bottom.toFloat()
                bottom = cameraWidth - face.rect.left.toFloat()
            }
            180 ->{
                mat.setScale(1f, if (mirror) -1f else 1.toFloat())
                mat.postTranslate(0f, if (mirror) cameraHeight.toFloat() else 0f)
                left = face.rect.right.toFloat()
                top = face.rect.bottom.toFloat()
                right = face.rect.left.toFloat()
                bottom = face.rect.top.toFloat()
            }
            270 ->{
                mat.setScale(if (mirror) -1f else 1.toFloat(), 1f)
                mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                left = cameraHeight - face.rect.bottom.toFloat()
                top = face.rect.left.toFloat()
                right = cameraHeight - face.rect.top.toFloat()
                bottom = face.rect.right.toFloat()
            }
        }
        val drect = RectF()
        val srect = RectF(left, top, right, bottom)
        mat.mapRect(drect, srect)
        LogUtil.d(TAG," \r\n识别区域:{left:${rect?.left},top:${rect?.top},right:${rect?.right},bottom:${rect?.bottom}}\n" +
                "人脸当前区域：left:${drect.left}  top:${drect.top} right:${drect.right}, bottom:${drect.bottom}")
        if (drect.left >= rect!!.left && drect.top >= rect!!.top && drect.right <= rect!!.right && drect.bottom <=rect!!.bottom)return drect
        return null
    }

    private fun rotateNV21(angle :Int,dst :ByteArray,src: CameraPreviewData){
        when(angle){
            0 ->{
                System.arraycopy(src.nv21Data,0,dst,0,src.nv21Data.size)
                LogUtil.d(TAG,"进行0°旋转")
            }
            90 ->{
                var index = 0
                var oldIndex = 0

                for (y in 0 until src.width){
                    for (x in 0 until src.height){
                        oldIndex  = (src.height - 1  - x) * src.width + y
                        dst[index++] = src.nv21Data[oldIndex]
                    }
                }

                for (y in 0 until src.width step 2){
                    for (x in 0 until src.height step 2){
                        oldIndex  = (src.height + (src.height  - x - 2) / 2) * src.width +y
                        dst[index++] = src.nv21Data[oldIndex]
                        dst[index++] = src.nv21Data[oldIndex + 1]
                    }
                }
                LogUtil.d(TAG,"进行90°旋转")
            }
            180 ->{
                var index = 0
                var oldX = 0
                var oldY = 0
                var oldIndex = 0
                var vuY = 0

                for (y in 0 until src.height){
                    for (x in 0 until src.width){
                        oldY = (src.height - 1 ) - y
                        oldX = (src.width - 1) - x
                        oldIndex  = oldY * src.width + oldX
                        dst[index++] = src.nv21Data[oldIndex]
                    }
                }

                for (y in 0 until src.height step 2){
                    for (x in 0 until src.width step 2){
                        oldY  = (src.height - 1 ) - (y +1)
                        oldX  = (src.width - 1 ) - (x +1 )
                        vuY  = src.height + oldY / 2
//                        val vuX  = oldX
                        oldIndex  = vuY * src.width +oldX
                        dst[index++] = src.nv21Data[oldIndex]
                        dst[index++] = src.nv21Data[oldIndex + 1]
                    }
                }
                LogUtil.d(TAG,"进行180°旋转")
            }
            270 ->{
                var index = 0
                var oldX = 0
//                var oldY = 0
                var oldIndex = 0
                var vuY = 0

                for (y in 0 until src.width){
                    for (x in 0 until src.height){
//                        val oldY = x
                        oldX = src.width - 1 - y
                        oldIndex  = x * src.width + oldX
                        dst[index++] = src.nv21Data[oldIndex]
                    }
                }

                for (y in 0 until src.width step 2){
                    for (x in 0 until src.height step 2){
//                        val oldY  = x
                        oldX  = src.width - 1  - (y +1 )
                        vuY  = src.height + x / 2
//                        val vuX  = oldX
                        oldIndex  = vuY * src.width +oldX
                        dst[index++] = src.nv21Data[oldIndex]
                        dst[index++] = src.nv21Data[oldIndex + 1]
                    }
                }
                LogUtil.d(TAG,"进行270°旋转")
            }
        }
    }

    //镜像处理
    private fun reverse(src :ByteArray ,dst :ByteArray,width :Int ,height : Int){
        var index = 0
        var oldX = 0
        var oldIndex = 0
        var vuY = 0
        for (y in 0 until height){
            for (x in 0 until width){
//                val oldY =  y
                oldX = width - 1 - x
                oldIndex  = y * width + oldX
                dst[index++] = src[oldIndex]
            }
        }

        for (y in 0 until height step 2){
            for (x in 0 until width step 2){
//                val oldY = y
                oldX = width - 1  - (x +1 )
                vuY  = height + y / 2
//                val vuX  = oldX
                oldIndex  = vuY * width +oldX
                dst[index++] = src[oldIndex]
                dst[index++] = src[oldIndex + 1]
            }
        }
        LogUtil.d(TAG,"进行镜像处理")
    }


    override fun onPictureTakenBlackWhite(cameraPreviewData: CameraPreviewData) {
        blackWhiteCameraChannel?.offer(cameraPreviewData)
    }

    override fun onPictureTakenColor(cameraPreviewData: CameraPreviewData) {
        colorCameraChannel?.offer(cameraPreviewData)

    }


}
