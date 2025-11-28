package com.yannuo.dgcanteen.facepass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.my.yuvconvert.YuvUtil
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import mcv.facepass.FacePassException
import mcv.facepass.FacePassHandler
import mcv.facepass.types.FacePassAddFaceResult
import mcv.facepass.types.FacePassConfig
import mcv.facepass.types.FacePassDetectionResult
import mcv.facepass.types.FacePassFace
import mcv.facepass.types.FacePassFeatureAppendInfo
import mcv.facepass.types.FacePassImage
import mcv.facepass.types.FacePassImageType
import mcv.facepass.types.FacePassModel
import mcv.facepass.types.FacePassPose
import mcv.facepass.types.FacePassRecognitionState
import mcv.facepass.types.FacePassRect
import mcv.facepass.types.FacePassTrackIdState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicBoolean

class FacePass(context: Context) : CameraListener {
    private val TAG = javaClass.simpleName
    private val mContext = context
    private var mFacePassHandler: FacePassHandler? = null   //SDK实例
    private val mScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var yuvUtil: YuvUtil = YuvUtil()
    private val myBitmapUtil: MyBitmapUtil = MyBitmapUtil(context)

    @Volatile
    private var allowAddFrame = true

    @Volatile
    private var rgbCameraChannel: Channel<CameraPreviewData>? = Channel(2)         //彩色原始数据

    @Volatile
    private var irCameraChannel: Channel<CameraPreviewData>? = Channel(2)    //黑白原始数据

    @Volatile
    private var previewChannel: Channel<CameraPreviewData>? = Channel(2)             //预览数据

    @Volatile
    private var livenessChannel: Channel<FacePassDetectionResult>? = Channel(1)      //活检数据

    @Volatile
    private var recognizeChannel: Channel<RecognizeData>? = Channel(1)     //人脸识别数据

    private var initializeResult = -1  //算法初始化结果（-1（未初始化），0（初始化成功），1（初始化出错））
    private var rect: Rect? = null      //人脸识别区域
    private var isFreedInterrupt = false
    private var cameraWidth = 0
    private var cameraHeight = 0
    private var faceResultListener: FaceResultListener? = null

    private var detect = AtomicBoolean(false) //默认关闭检测人脸
    private var liveness = AtomicBoolean(false) //默认关闭活体检测
    private var recognize = AtomicBoolean(false) // 默认关闭人脸识别
    private var minFaceThreshold = 125

    private var preViewRotation = 0
    private var mirror = true

    private var configJob: Job? = null
    private var converJob: Job? = null
    private var recognizeJob: Job? = null
    private var livenessJob: Job? = null
    private var feedJob: Job? = null

    private var roll = 10f
    private var pitch = 25f
    private var yaw = 25f

    private var searchThreshold = 65f
    private var livenessThreshold = 65f
    private var blurThreshold = 0.3f

    private var isLocalGroupExist = false //底库是否存在
    private val faceGroupName = "facePass" //人脸底库名称

    @Volatile
    private var discard = 2  //防止上次缓存存留

    /**
     * 活检回调
     */
    fun setListener(listener: FaceResultListener?) {
        faceResultListener = listener
    }

//    companion object {
//        val instance: FacePass by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
//            synchronized(FacePass::class.java) { FacePass(MyApplication.applicationContext) }
//        }
//    }

    /**
     * 配置人脸算法
     */
    fun facePassConfig(listener: SDKInitResult?) {
        configJob = mScope.launch {
            release()
//            FacePassHandler.initSDK(mContext)
//            LogUtil.i(TAG, "start initSDK ${Thread.currentThread().name}")
            while (isActive) {
                while (FacePassHandler.isAvailable()) {
                    var message = ""
                    try {
                        val config = FacePassConfig()
                        config.poseBlurModel = FacePassModel.initModel(mContext.assets, "attr.pose_blur.arm.190630.bin")
                        config.rgbIrLivenessModel = FacePassModel.initModel(mContext.assets, "liveness.CPU.rgbir.G.bin")
                        config.searchModel = FacePassModel.initModel(mContext.assets, "feat2.arm.H.v1.0_1core.bin")
                        config.detectModel = FacePassModel.initModel(mContext.assets, "detector.arm.G.bin")
                        config.landmarkModel = FacePassModel.initModel(mContext.assets, "pf.lmk.arm.E.bin")
                        config.detectRectModel = FacePassModel.initModel(mContext.assets, "detector_rect.arm.G.bin")
                        config.rcAttributeModel = FacePassModel.initModel(mContext.assets, "attr.RC.arm.E.bin")
                        config.occlusionFilterModel = FacePassModel.initModel(mContext.assets, "attr.occlusion.arm.20201209.bin")
                        config.searchThreshold = searchThreshold    //识别阈值
                        config.livenessThreshold = livenessThreshold    //活体阈值
                        config.livenessEnabled = false  //活体开关
                        config.rgbIrLivenessEnabled = true  //红外活体开关
                        config.rcAttributeAndOcclusionMode = 2  //遮挡不给入库和不送识别
                        config.faceMinThreshold = minFaceThreshold
                        config.poseThreshold = FacePassPose(this@FacePass.roll, this@FacePass.pitch, this@FacePass.yaw)
                        config.blurThreshold = blurThreshold    //模糊度
                        config.lowBrightnessThreshold = 30f
                        config.highBrightnessThreshold = 210f
                        config.brightnessSTDThreshold = 80f
                        config.retryCount = 5
                        config.smileEnabled = false
                        config.maxFaceEnabled = true
                        config.fileRootPath = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath

                        /* 创建SDK实例 */
//                        mFacePassHandler = FacePassHandler(config)
                        mFacePassHandler = FacePassHandler()
                        val ret: Int = mFacePassHandler!!.initHandle(config)
                        if (ret != 0) {
                            LogUtil.e(TAG, "Build FacePassHandler failed, error: $ret")
                            return@launch
                        }
                        LogUtil.i(TAG, "算法模型配置成功...")
                        initializeResult = 0
                        message = "算法模型配置成功"
                        return@launch
                    } catch (e: Exception) {
                        e.printStackTrace()
                        initializeResult = 1
                        message = "相机算法初始化失败"
                        LogUtil.e(TAG, "相机算法初始化失败...")
                        return@launch
                    } finally {
                        listener?.faceInitResult(initializeResult, message)
                    }
                }
                delay(100)
            }
        }
    }

    fun release() {
        mFacePassHandler?.release()
    }

    private fun createChannel() {
        if (rgbCameraChannel == null) rgbCameraChannel = Channel(2)
        if (irCameraChannel == null) irCameraChannel = Channel(2)
        if (previewChannel == null) previewChannel = Channel(2)
        if (livenessChannel == null) livenessChannel = Channel(1)
        if (recognizeChannel == null) recognizeChannel = Channel(1)
    }

    private fun clearChannel() {
        if (rgbCameraChannel != null) {
            rgbCameraChannel?.cancel()
            rgbCameraChannel = null
        }
        if (irCameraChannel != null) {
            irCameraChannel?.cancel()
            irCameraChannel = null
        }
        if (previewChannel != null) {
            previewChannel?.cancel()
            previewChannel = null
        }
        if (livenessChannel != null) {
            livenessChannel?.cancel()
            livenessChannel = null
        }
        if (recognizeChannel != null) {
            recognizeChannel?.cancel()
            recognizeChannel = null
        }
    }

    /**
     * 传递相机的预览分辨率，用于计算人脸检测区域
     */
    fun setCameraPreSize(colorPreViewWidth: Int, colorPreViewHeight: Int) {
        this.cameraWidth = colorPreViewWidth
        this.cameraHeight = colorPreViewHeight
    }

    /**
     * 活体检测区域
     */
    fun setRect(rect: Rect) {
        this.rect = rect
    }

    fun setDetect(enable: Boolean) {
        detect.set(enable)
        LogUtil.d(TAG, "人脸侦测是否打开：${enable}")
    }

    fun getDetect() = detect.get()

    /**
     * 开关活体
     */
    fun setLiveness(enable: Boolean) {
        liveness.set(enable)
        mFacePassHandler?.config?.rgbIrLivenessEnabled = enable
        LogUtil.d(TAG, "活检是否打开：${enable}")
    }

    /**
     * 开关人脸识别
     */
    fun setRecognize(enable: Boolean) {
        recognize.set(enable)
        LogUtil.d(TAG, "人脸识别是否打开：${enable}")
    }

    /**
     * 设置最小人脸大小识别参数
     * [0-512]
     */
    fun setFaceMinThreshold(threshold: Int) {
        if (threshold < 0 || threshold > 512) return
        minFaceThreshold = threshold
        when (initializeResult) {
            -1 -> {
                LogUtil.d(TAG, "未初始化前 识别距离")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.faceMinThreshold = threshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG, "初始化后 识别距离")
            }
        }
    }

    /**
     * 设置人脸识别三维角度[-90,90]
     * roll 旋转角度
     * pitch 垂直角度
     * yaw  水平角度
     */
    fun setFacePose(roll: Float, pitch: Float, yaw: Float) {
        if (roll < -90 || roll > 90 || pitch < -90 || pitch > 90 || yaw < -90 || yaw > 90) return
        this.roll = roll
        this.pitch = pitch
        this.yaw = yaw
        when (initializeResult) {
            -1 -> {
                LogUtil.d(TAG, "未初始化前 设置人脸识别三维角度")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.poseThreshold = FacePassPose(roll, pitch, yaw)
                mFacePassHandler?.config = config
                LogUtil.d(TAG, "初始化后 设置人脸识别三维角度")
            }
        }
    }

    /**
     * 设置人脸识别分数[0,100]
     */
    fun setFaceSearchThreshold(searchThreshold: Float) {
        if (searchThreshold < 0 || searchThreshold > 100) return
        this.searchThreshold = searchThreshold
        when (initializeResult) {
            -1 -> {
                LogUtil.d(TAG, "未初始化前 设置人脸识别分数")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.searchThreshold = searchThreshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG, "初始化后 设置人脸识别分数")
            }
        }
    }

    /**
     * 设置人脸活检分数[0,100]
     */
    fun setFaceLivenessThreshold(livenessThreshold: Float) {
        if (livenessThreshold < 0 || livenessThreshold > 100) return
        this.livenessThreshold = livenessThreshold
        when (initializeResult) {
            -1 -> {
                LogUtil.d(TAG, "未初始化前 设置人脸活检分数")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.livenessThreshold = livenessThreshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG, "初始化后 设置人脸活检分数")
            }
        }
    }

    /**
     * 设置人脸模糊度0 ~1  ,default 0.3f
     */
    fun setFaceBlurThreshold(blurThreshold: Float) {
        if (blurThreshold < 0 || blurThreshold > 1) return
        this.blurThreshold = blurThreshold
        when (initializeResult) {
            -1 -> {
                LogUtil.d(TAG, "未初始化前 设置人脸模糊度")
            }
            0 -> {
                val config = mFacePassHandler?.config
                config?.blurThreshold = blurThreshold
                mFacePassHandler?.config = config
                LogUtil.d(TAG, "初始化后 设置人脸模糊度")
            }
        }
    }

    override fun onRGBPreviewDataTaken(cameraPreviewData: CameraPreviewData) {
        try {
            if (allowAddFrame) rgbCameraChannel?.let { it.trySend(cameraPreviewData).isSuccess }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onIRPreviewDataTaken(cameraPreviewData: CameraPreviewData) {
        try {
            if (allowAddFrame) irCameraChannel?.let { it.trySend(cameraPreviewData).isSuccess }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 人脸检测
     */
    fun feedFrame() {
        discard = 2
        createChannel()
        convert()
        recognizeFace()
        checkLiveness()
        feedJob = mScope.launch {
            while (isActive && !isFreedInterrupt && initializeResult != 1) {
//                LogUtil.d(TAG, "进入人脸检测协程")
                if (mFacePassHandler == null) {
                    delay(50)
                    continue
                }
                try {
                    val rgbReceiveRes = rgbCameraChannel?.receiveCatching()
                    val irReceiveRes = irCameraChannel?.receiveCatching()
                    if (rgbReceiveRes == null || irReceiveRes == null) continue
                    if (rgbReceiveRes.isClosed || rgbReceiveRes.isFailure) continue
                    if (irReceiveRes.isClosed || irReceiveRes.isFailure) continue
                    val rgbReceive = rgbReceiveRes.getOrNull()
                    val irReceive = irReceiveRes.getOrNull()
                    if (rgbReceive == null || irReceive == null) continue
//                    LogUtil.d(TAG, "接收预览帧")
                    previewChannel?.let { it.trySend(rgbReceive).isSuccess }
//                    LogUtil.d(TAG, "发送预览帧")
                    if (!detect.get()) continue   //关闭检测就返回
                    val rgbImage = FacePassImage(rgbReceive.nv21Data, rgbReceive.width, rgbReceive.height, rgbReceive.rotation, FacePassImageType.NV21)
                    val irImage = FacePassImage(irReceive.nv21Data, irReceive.width, irReceive.height, irReceive.rotation, FacePassImageType.NV21)

                    preViewRotation = rgbReceive.preRotation
                    mirror = rgbReceive.mirror

//                    LogUtil.d(TAG, "设置预览的参数信息")
                    yield()
                    val detectionResult = mFacePassHandler!!.feedFrameRGBIR(rgbImage, irImage)
//                    LogUtil.d(TAG, "将帧数据送入模型")
                    if (detectionResult == null || detectionResult.faceList.isEmpty()) {
//                        LogUtil.v(TAG, "未检测到人脸")
                    } else {
                        //检测是否遮挡
                        val lmkoccsta = lmkoccsta(detectionResult.faceList[0])
                        if (lmkoccsta && liveness.get()) faceResultListener?.onTips("人脸被遮挡")
                        if (detectionResult.message.isNotEmpty()) {
                            if (liveness.get()) {
                                livenessChannel?.let { it.trySend(detectionResult).isSuccess }
                                LogUtil.d(TAG, "发送活检")
                            } else {
                                val judgeArea = judgeArea(detectionResult.faceList[0], preViewRotation, mirror)
                                if (!judgeArea) continue
                                val recognizeData = RecognizeData().apply {
                                    path = ""
                                    detectionRes = detectionResult
                                }
                                recognizeChannel?.let { it.trySend(recognizeData).isSuccess }
                                LogUtil.d(TAG, "发送人脸识别")
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.w(TAG, "帧检测 ${e.message}")
                }
            }
        }
    }

    /**
     * 相机帧质量通过处理
     */
    private fun checkLiveness() {
        livenessJob = mScope.launch {
//            LogUtil.d(TAG, "相机帧处理完成，开启活检：$isActive")
            while (isActive) {
                try {
                    val channelRes = livenessChannel?.receiveCatching() ?: continue
                    if (channelRes.isClosed || channelRes.isFailure) continue
                    val detectionResult = channelRes.getOrNull()
                    //                    LogUtil.d(TAG, "${!detect.get()} 和 ${detectionResult==null}")
                    if (!detect.get()) continue
                    if (!liveness.get()) continue
                    if (detectionResult == null) continue
//                    LogUtil.d(TAG, "检测识别区域")
                    //检测识别区域
                    val judgeArea = judgeArea(detectionResult.faceList[0], preViewRotation, mirror)
//                    LogUtil.d(TAG, "识别区域：$judgeArea")
                    when (judgeArea) {
                        true -> {
                            LogUtil.d(TAG, "识别人脸通过，进入活检")
//                            deleteAllFacePicture()
                            val livenessClassify = mFacePassHandler!!.livenessClassify(detectionResult.message)
                            discard--
//                            LogUtil.d(TAG, "$discard")
                            if (discard < 0) {
                                LogUtil.i(TAG, "活体阈值 -> ${livenessClassify[0].livenessThreshold} 活体识别分数 -> ${livenessClassify[0].livenessScore} FacePassLivenessState -> ${livenessClassify[0].livenessState}")
                                when (livenessClassify[0].livenessState) {
                                    0 -> {
//                                        feedJob?.cancel()
                                        //活检通过
                                        if (detect.get() && (faceResultListener != null)) {
                                            val facePassImage = detectionResult.images[0]
                                            val recognizeData = RecognizeData().apply {
                                                detectionRes = detectionResult
                                            }

                                            val path = handlePicture(facePassImage.image, facePassImage.width, facePassImage.height, facePassImage.rect_ex, preViewRotation, mirror)
                                            var consumePic = false
                                            if (path.isNullOrEmpty().not()) {
                                                recognizeData.path = path
                                                consumePic = faceResultListener?.onLiveness(path ?: "") ?: false
                                            }

                                            LogUtil.d(TAG, "送去人脸识别的照片：${recognizeData.path}")

                                            if (!consumePic) {
                                                val byteArray = ByteArray(facePassImage.image.size)
                                                yuvUtil.yuvCompress(facePassImage.image, facePassImage.width, facePassImage.height, byteArray, 1f, 1f, 0, preViewRotation, mirror)
                                                var width = facePassImage.width
                                                var height = facePassImage.height
                                                if (preViewRotation == 90 || preViewRotation == 270) {
                                                    width = facePassImage.height
                                                    height = facePassImage.width
                                                }
                                                faceResultListener?.onLiveness(byteArray, width, height)
                                            }

                                            // 人脸识别
                                            recognizeChannel?.trySend(recognizeData)?.isSuccess
                                            LogUtil.d(TAG, "发送人脸识别")

                                            /* 保存图片需要进行裁剪 */
                                        }
//                                        ensureActive()
                                    }
                                    else -> {
                                        //活检失败
                                        when (livenessClassify[0].livenessErrorCode) {
                                            1 -> if (detect.get()) faceResultListener?.onTips("没有检测到人脸")
                                            2 -> if (detect.get()) faceResultListener?.onTips("请靠近一点")
                                            else -> if (detect.get()) faceResultListener?.onTips("未知错误!")
                                        }
                                    }
                                }
                            }
                        }
                        false -> {
                            LogUtil.d(TAG, "不在活检有效区域")
                            if (detect.get()) faceResultListener?.onTips("不在活检有效区域")
                        }
                    }
                    mFacePassHandler?.setMessage(detectionResult.faceList[0].trackId, FacePassTrackIdState.TRACK_ID_RETRY)
//                    LogUtil.d(TAG, detectionResult.faceList[0].trackId.toString())
                } catch (e: Exception) {
//                    LogUtil.w(TAG, "识别处理异常 ${e.message}")
//                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 人脸识别
     */
    private fun recognizeFace() {
        recognizeJob = mScope.launch {
            while (isActive) {
                try {
                    val channelRes = recognizeChannel?.receiveCatching() ?: continue
                    if (channelRes.isClosed || channelRes.isFailure) continue
                    val recognizeData = channelRes.getOrNull()
                    if (!detect.get()) continue
                    if (!recognize.get()) continue
                    if (recognizeData == null) continue
                    val detectionResult = recognizeData.detectionRes ?: continue

//                    LogUtil.d(TAG, "人脸数据：${recognizeData.detectionRes?.faceList?.get(0)?.trackId}")

                    resetMessage(detectionResult.faceList[0].trackId)

                    val localGroupFaceNum = getGroupFaceCount()
                    if (localGroupFaceNum <= 0) {
                        LogUtil.e(TAG, "人脸库人脸数量为0")
                        // 识别失败
                        faceResultListener?.onRecognized(false, "识别失败，人脸库人脸数量为0")
                        continue
                    }
                    LogUtil.d(TAG, "当前人脸库人脸数量：$localGroupFaceNum")
                    val recognizeRes = mFacePassHandler?.recognize(faceGroupName, detectionResult.message)
                    if (recognizeRes == null || recognizeRes.isEmpty()) {
                        faceResultListener?.onRecognized(false, "识别失败，人脸库没有该人脸数据1")
                    } else {
                        for (result in recognizeRes) {
                            if (result.trackId == detectionResult.faceList[0].trackId) {
                                val faceToken = String(result.faceToken)
                                //人脸识别分数
                                val searchScore = result.detail.searchScore
                                if (FacePassRecognitionState.RECOGNITION_PASS == result.recognitionState) {
                                    LogUtil.d(TAG, "识别通过，token：$faceToken")
                                    var imgBase64 = ""
                                    val facePassImage = detectionResult.images[0]
                                    if (!liveness.get()) {
                                        // 没开活检
                                        val base64 = handlePicture(facePassImage.image, facePassImage.width, facePassImage.height, facePassImage.rect_ex, preViewRotation, mirror, false)
                                        imgBase64 = base64 ?: ""
                                    } else {
                                        // 开启活检
                                        val path = recognizeData.path
                                        if (path != null) {
                                            imgBase64 = myBitmapUtil.imagePathToBase64(path)
                                            val file = File(path)
                                            if (!file.delete()) file.deleteOnExit()
                                        }
                                    }
                                    faceResultListener?.onRecognized(true, "识别通过", faceToken, imgBase64, searchScore)
                                } else {
                                    faceResultListener?.onRecognized(false, "识别不通过，人脸库没有该人脸数据2")
                                }
                                resetMessage(detectionResult.faceList[0].trackId)
                            } else resetMessage(result.trackId)
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, "识别失败，异常错误：${e.message}")
                    // 识别失败
                    faceResultListener?.onRecognized(false, "识别失败，异常错误：${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 为了防止同一个人，重复识别
     * 停止相机帧流入，以及清空所有队列数据
     */
    fun stopInputFrame() {
        allowAddFrame = false

        if (rgbCameraChannel != null) {
            rgbCameraChannel?.clear()
            rgbCameraChannel?.close()
            rgbCameraChannel = null
        }
        if (irCameraChannel != null) {
            irCameraChannel?.clear()
            irCameraChannel?.close()
            irCameraChannel = null
        }
        if (previewChannel != null) {
            previewChannel?.clear()
            previewChannel?.close()
            previewChannel = null
        }
        if (livenessChannel != null) {
            livenessChannel?.clear()
            livenessChannel?.close()
            livenessChannel = null
        }
        if (recognizeChannel != null) {
            recognizeChannel?.clear()
            recognizeChannel?.close()
            recognizeChannel = null
        }
    }

    /**
     * 开始接收相机帧
     */
    fun startInputFrame() {
        allowAddFrame = true

        createChannel()
    }

    fun <E> Channel<E>.clear() {
        while (true) {
            val result = tryReceive()
            if (result.isFailure) break
        }
    }


    private fun resetMessage(trackId: Long) {
        mFacePassHandler?.setMessage(trackId, 0)
    }

    fun getGroupFaceCount(): Int {
        return mFacePassHandler?.getLocalGroupFaceNum(faceGroupName) ?: 0
    }

    /**
     * 裁剪图片保存在指定目录
     * save为true时返回绝对路径，为false时返回Base64
     */
    private fun handlePicture(data: ByteArray, width: Int, height: Int, rect: FacePassRect, rotation: Int, mirror: Boolean, save: Boolean = true): String? {
        //格式转换
        var result: String? = null
        var fileOutputStream: FileOutputStream? = null
        var filePath: File? = null
        try {
            val bitmap = CameraUtil.instance.nv21ToBitmap2(data, width, height)

            /* 裁剪图片 */
            val cropBitmap = CameraUtil.instance.cropBitmap(bitmap, rect.left, rect.top, rect.right, rect.bottom)
            if (!bitmap.isRecycled) bitmap.recycle()
            /* 图片旋转镜像 */
            val rotationBitmap = CameraUtil.instance.getRotateBitmap(cropBitmap, rotation.toFloat(), mirror, true)
            if (!cropBitmap.isRecycled) cropBitmap.recycle()

            if (save) {
                val file = mContext.getDir("face", Context.MODE_PRIVATE)
                val fileName = SimpleDateFormat("yyyyMMddHHmmssS").format(System.currentTimeMillis()).toString() + ".jpeg"
//            val fileName = SimpleDateFormat().format("yyyyMMddHHmmssS", System.currentTimeMillis()).toString() + ".jpeg"
                filePath = File(file, fileName)
                if (!filePath.exists()) {
                    file.mkdirs()
                    filePath.createNewFile()
                }
                fileOutputStream = FileOutputStream(filePath)

                rotationBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
                /*回收Bitmap资源*/
                if (!rotationBitmap.isRecycled) rotationBitmap.recycle()
                LogUtil.i(TAG, "absolutePath --${filePath.absolutePath}")
                result = filePath.absolutePath
            } else {
                result = myBitmapUtil.bitmapToBase64(rotationBitmap)
                if (!rotationBitmap.isRecycled) rotationBitmap.recycle()
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, "文件保存中出错" + e.printStackTrace())
            filePath?.also {
                filePath.delete()
            }
            fileOutputStream?.close()
        } finally {
            return result
        }
    }

    /**
     * 删除指定目录下所有人脸图片
     */
    private fun deleteAllFacePicture() {
        val file = mContext.getDir("face", Context.MODE_PRIVATE)
        val files = file.listFiles()
        files?.forEach {
            it.delete()
        }
        LogUtil.d(TAG, "删除指定目录下所有人脸图片")
    }


    /**
     * NV21格式转Bitmap预览帧
     * 带转角和镜像
     */
    private fun convert() {
        converJob = mScope.launch {
            var ruslt = ByteArray(0)
            var rgbReceive: CameraPreviewData?
            while (isActive) {
                try {
                    val channelRes = previewChannel?.receiveCatching() ?: continue
                    if (channelRes.isClosed || channelRes.isFailure) continue
                    rgbReceive = channelRes.getOrNull()
                    if (rgbReceive == null) continue
//                    LogUtil.d(TAG, "人脸图片大小：${colorReceive.nv21Data.size}")
                    ruslt = ByteArray(rgbReceive.nv21Data.size)
                    yuvUtil.yuvCompress(rgbReceive.nv21Data, rgbReceive.width, rgbReceive.height, ruslt, 1f, 1f, 0, rgbReceive.preRotation, rgbReceive.mirror)
                    ensureActive()
                    var width = rgbReceive.width
                    var height = rgbReceive.height
                    if (rgbReceive.preRotation == 90 || rgbReceive.preRotation == 270) {
                        width = rgbReceive.height
                        height = rgbReceive.width
                    }
                    faceResultListener?.onPreView(ruslt, width, height)
//                    LogUtil.d(TAG, "预览回调: $ruslt")
                } catch (e: Exception) {
                    LogUtil.w(TAG, "预览帧处理异常  ${e.message}")
                    continue
                }
            }
        }
    }

    /**
     * 关闭协程
     */
//    fun closeJobo() {
//        try {
//            LogUtil.i(TAG, "start协程关闭")
//            recognizeCallback = null
//            this@FacePass.rect = null
//            converJob?.cancel()
//            feedJob?.cancel()
//            livenessJob?.cancel()
//            clearChannel()
//            LogUtil.i(TAG, "all协程关闭")
//        } catch (e: Exception) {
//        }
//    }

    fun closeJob() {
        runBlocking {
            try {
                faceResultListener = null
                this@FacePass.rect = null
                converJob?.cancelAndJoin()
                feedJob?.cancelAndJoin()
                livenessJob?.cancelAndJoin()
                clearChannel()
//                deleteAllFacePicture()
                LogUtil.d(TAG, "关闭协程 => 清除所有存在的相机帧数据")
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 判断人脸是否遮挡
     */
    private fun lmkoccsta(face: FacePassFace): Boolean {
        return face.facePassFeedFrameErrorCode == 7
    }

    /**
     * 判断人脸是否在区域
     * 在 true ;不在 false
     */
    private fun judgeArea(face: FacePassFace, preRotation: Int, mirror: Boolean): Boolean {
        if (rect == null) return true
        val mat = Matrix()
        var left = 0f
        var top = 0f
        var right = 0f
        var bottom = 0f
        when (preRotation) {
            0 -> {
                left = face.rect.left.toFloat()
                top = face.rect.top.toFloat()
                right = face.rect.right.toFloat()
                bottom = face.rect.bottom.toFloat()
                mat.setScale((if (mirror) -1f else 1.toFloat()), 1f)
                mat.postTranslate(if (mirror) cameraWidth.toFloat() else 0f, 0f)
            }
            90 -> {
                mat.setScale(if (mirror) -1f else 1.toFloat(), 1f)
                mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                left = face.rect.top.toFloat()
                top = cameraWidth - face.rect.right.toFloat()
                right = face.rect.bottom.toFloat()
                bottom = cameraWidth - face.rect.left.toFloat()
            }
            180 -> {
                mat.setScale(1f, if (mirror) -1f else 1.toFloat())
                mat.postTranslate(0f, if (mirror) cameraHeight.toFloat() else 0f)
                left = face.rect.right.toFloat()
                top = face.rect.bottom.toFloat()
                right = face.rect.left.toFloat()
                bottom = face.rect.top.toFloat()
            }
            270 -> {
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
        LogUtil.d(TAG, " \r\n识别区域:{left:${rect?.left},top:${rect?.top},right:${rect?.right},bottom:${rect?.bottom}}\n当前区域：left:${drect.left}  top:${drect.top} right:${drect.right}, bottom:${drect.bottom}")
        if (drect.left >= rect!!.left && drect.top >= rect!!.top && drect.right <= rect!!.right && drect.bottom <= rect!!.bottom) return true
        return false
    }

    private fun rotateNV21(angle: Int, dst: ByteArray, src: CameraPreviewData) {
        when (angle) {
            0 -> {
                System.arraycopy(src.nv21Data, 0, dst, 0, src.nv21Data.size)
                LogUtil.d(TAG, "进行0°旋转")
            }
            90 -> {
                var index = 0
                var oldIndex = 0

                for (y in 0 until src.width) {
                    for (x in 0 until src.height) {
                        oldIndex = (src.height - 1 - x) * src.width + y
                        dst[index++] = src.nv21Data[oldIndex]
                    }
                }

                for (y in 0 until src.width step 2) {
                    for (x in 0 until src.height step 2) {
                        oldIndex = (src.height + (src.height - x - 2) / 2) * src.width + y
                        dst[index++] = src.nv21Data[oldIndex]
                        dst[index++] = src.nv21Data[oldIndex + 1]
                    }
                }
                LogUtil.d(TAG, "进行90°旋转")
            }
            180 -> {
                var index = 0
                var oldX = 0
                var oldY = 0
                var oldIndex = 0
                var vuY = 0

                for (y in 0 until src.height) {
                    for (x in 0 until src.width) {
                        oldY = (src.height - 1) - y
                        oldX = (src.width - 1) - x
                        oldIndex = oldY * src.width + oldX
                        dst[index++] = src.nv21Data[oldIndex]
                    }
                }

                for (y in 0 until src.height step 2) {
                    for (x in 0 until src.width step 2) {
                        oldY = (src.height - 1) - (y + 1)
                        oldX = (src.width - 1) - (x + 1)
                        vuY = src.height + oldY / 2
//                        val vuX  = oldX
                        oldIndex = vuY * src.width + oldX
                        dst[index++] = src.nv21Data[oldIndex]
                        dst[index++] = src.nv21Data[oldIndex + 1]
                    }
                }
                LogUtil.d(TAG, "进行180°旋转")
            }
            270 -> {
                var index = 0
                var oldX = 0
//                var oldY = 0
                var oldIndex = 0
                var vuY = 0

                for (y in 0 until src.width) {
                    for (x in 0 until src.height) {
//                        val oldY = x
                        oldX = src.width - 1 - y
                        oldIndex = x * src.width + oldX
                        dst[index++] = src.nv21Data[oldIndex]
                    }
                }

                for (y in 0 until src.width step 2) {
                    for (x in 0 until src.height step 2) {
//                        val oldY  = x
                        oldX = src.width - 1 - (y + 1)
                        vuY = src.height + x / 2
//                        val vuX  = oldX
                        oldIndex = vuY * src.width + oldX
                        dst[index++] = src.nv21Data[oldIndex]
                        dst[index++] = src.nv21Data[oldIndex + 1]
                    }
                }
                LogUtil.d(TAG, "进行270°旋转")
            }
        }
    }

    //镜像处理
    private fun reverse(src: ByteArray, dst: ByteArray, width: Int, height: Int) {
        var index = 0
        var oldX = 0
        var oldIndex = 0
        var vuY = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
//                val oldY =  y
                oldX = width - 1 - x
                oldIndex = y * width + oldX
                dst[index++] = src[oldIndex]
            }
        }

        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
//                val oldY = y
                oldX = width - 1 - (x + 1)
                vuY = height + y / 2
//                val vuX  = oldX
                oldIndex = vuY * width + oldX
                dst[index++] = src[oldIndex]
                dst[index++] = src[oldIndex + 1]
            }
        }
        LogUtil.d(TAG, "进行镜像处理")
    }

    /**
     * 添加人脸
     */
    fun registerFace(imageUrl: String): ByteArray? {
        if (mFacePassHandler == null) {
            LogUtil.d(TAG, "registerFace ==> facePassHandler is null")
            return null
        }
        try {
            val imageFile: File = File(imageUrl)
            if (!imageFile.exists()) {
                LogUtil.d(TAG, "registerFace ==> image not exist")
                return null
            }
            val bitmap = BitmapFactory.decodeFile(imageUrl)
            try {
                val result: FacePassAddFaceResult? = mFacePassHandler?.addFace(bitmap)
                if (result != null) {
                    when (result.result) {
                        0 -> {
                            LogUtil.d(TAG, "registerFace ==> add face success")
                            return result.faceToken
                        }
                        1 -> LogUtil.d(TAG, "registerFace ==> face not found")
                        else -> LogUtil.d(TAG, "registerFace ==> face quality problem")
                    }
                }
            } catch (e: FacePassException) {
                e.printStackTrace()
                LogUtil.d(TAG, "registerFace ==> ${e.message}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 绑定底库和人脸
     */
    fun bindFaceToGroup(faceToken: ByteArray): Boolean {
        if (mFacePassHandler == null) {
            LogUtil.d(TAG, "bindFaceToGroup ==> facePassHandler is null")
            return false
        }
        var isSuccess = false
        try {
            isSuccess = mFacePassHandler?.bindGroup(faceGroupName, faceToken) ?: false
            if (isSuccess) LogUtil.d(TAG, "bindFaceToGroup ==> bind success")
            else LogUtil.d(TAG, "bindFaceToGroup ==> bind failure")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.d(TAG, "bindFaceToGroup ==> ${e.message}")
        }
        return isSuccess
    }

    //抽取特征值
    fun extractFeature(imagePath: String): ByteArray? {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        return try {
            val extractFeature = mFacePassHandler?.extractFeature(bitmap)
            extractFeature?.featureData
        } catch (e: FacePassException) {
            LogUtil.d(TAG,"抽取特征值异常")
            null
        }
    }

    /**
     * 抽取特征值
     */
    fun extractFeature(bitmap: Bitmap): ByteArray? {
        return try {
            if(mFacePassHandler == null) {
                ToastShowUtil.show("mFacePassHandler为空")
            }
            val extractFeature = mFacePassHandler?.extractFeature(bitmap)
            if(extractFeature == null) {
                ToastShowUtil.show("extractFeature为空")
            }
            extractFeature?.featureData
        } catch (e: FacePassException) {
            LogUtil.d(TAG,"抽取特征值异常")
            null
        }
    }

    fun initLocalGroup(): Int {
        var i = 0
        if (mFacePassHandler == null) {
            return i
        }
        i = mFacePassHandler!!.initLocalGroup(faceGroupName)
        return i
    }

    //注册人脸
    fun registerFaces(message: String): String {
        if (mFacePassHandler == null) {
            return ""
        }
        //转换特征值
        val token = stringToByteArray(message)
        val insertFeature = mFacePassHandler!!.insertFeature(token, FacePassFeatureAppendInfo())
        val bindFaceToGroup = bindFaceToGroup(insertFeature)
        return if(bindFaceToGroup){
            insertFeature
        }else{
            ""
        }
    }

    //注册人脸
    fun registerFaces(messageArray: ByteArray): String {
        if (mFacePassHandler == null) {
            return ""
        }
        //转换特征值
        val insertFeature = mFacePassHandler!!.insertFeature(messageArray, FacePassFeatureAppendInfo())
        val bindFaceToGroup = bindFaceToGroup(insertFeature)
        return if(bindFaceToGroup){
            insertFeature
        }else{
            ""
        }
    }

    //将人脸绑定底库
    fun bindFaceToGroup(insertFeature: String): Boolean {
        if(mFacePassHandler == null){
            return false
        }
        var b = false
        val faceToken = insertFeature.toByteArray()
        if (faceToken.isEmpty() || TextUtils.isEmpty(faceGroupName)) {
            LogUtil.d(TAG, "params error！")
            return false
        }
        try {
            b = mFacePassHandler!!.bindGroup(faceGroupName,faceToken)
        }catch (e: Exception){
            e.printStackTrace()
            LogUtil.d(TAG, e.message!!)
        }
        return b
    }

    /**
     * 删除人脸
     */
    fun deleteFace(insertFeature: String): Boolean {
        if(mFacePassHandler == null){
            return false
        }
        val faceToken = insertFeature.toByteArray()
        if (faceToken.isEmpty()) {
            LogUtil.d(TAG, "error！faceToken is null")
            return false
        }
        return mFacePassHandler!!.deleteFace(faceToken)
    }

    /**
     * 新增底库
     */
    fun createFaceGroup(): Boolean{
        if(mFacePassHandler == null){
            return false
        }
        try {
            mFacePassHandler!!.createLocalGroup(faceGroupName)
        } catch (e: FacePassException) {
            e.printStackTrace()
        }
        checkGroupExist(faceGroupName)
        return isLocalGroupExist
    }

    /**
     * 查找底库是否存在
     */
    private fun checkGroupExist(groupName: String): Boolean{
        if(mFacePassHandler == null){
            return false
        }
        try {
            isLocalGroupExist = false
            var localGroups = mFacePassHandler!!.localGroups
            if (localGroups.isEmpty()) {
                LogUtil.d(TAG, "底库不存在group...")
                return false
            }
            for (group in localGroups) {
                if (groupName == group) {
                    isLocalGroupExist = true
                    break
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
        return isLocalGroupExist
    }

    /**
     * 删除底库数据
     */
    fun deleteFaceLocalGroup(): Boolean{
        if(mFacePassHandler == null){
            return false
        }
        var isSuccess = false
        try {
            isSuccess = mFacePassHandler!!.clearAllGroupsAndFaces()
            if(isSuccess){
                isLocalGroupExist = false
            }
        }catch (e: FacePassException){
            e.printStackTrace()
        }
        return isSuccess
    }

    //将String转换byte[]
    private fun stringToByteArray(byteString: String): ByteArray {
        // 去掉字符串中的方括号
        val trimmedString = byteString.substring(1, byteString.length - 1)
        // 按逗号分割字符串
        val byteStrings = trimmedString.split(", ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        // 创建 byte 数组
        val bytes = ByteArray(byteStrings.size)
        // 将每个字符串解析为 byte
        for (i in byteStrings.indices) {
            bytes[i] = byteStrings[i].toByte()
        }
        return bytes
    }
}