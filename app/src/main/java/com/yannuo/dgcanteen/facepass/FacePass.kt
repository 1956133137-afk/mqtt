package com.yannuo.dgcanteen.facepass

import android.annotation.SuppressLint
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
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import mcv.facepass.types.FacePassRecognitionResult
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
    private var GROUP_NAME = ""
    private var mFacePassHandler: FacePassHandler? = null   //SDK实例
    private val mScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var yuvUtil: YuvUtil = YuvUtil()

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mFacePass: FacePass? = null

        @JvmStatic
        fun getInstance(context: Context, groupName: String): FacePass? {
            if (mFacePass == null) {
                synchronized(AuthFace::class.java) {
                    if (mFacePass == null) {
                        mFacePass = FacePass(context)
                        /* 初始化配置 */
                        val configStatus = mFacePass?.facePassConfig() ?: false
                        /* 创建底库 */
                        val groupStatus = mFacePass?.createFaceGroup(groupName) ?: false
                        /* 初始化失败释放资源 */
                        if (!configStatus || !groupStatus) {
                            mFacePass?.release()
                            mFacePass = null
                        }
                    }
                }
                LogUtil.d("FacePass", if (mFacePass == null) "算法初始化失败" else "算法初始化成功")
            }
            return mFacePass
        }

        @JvmStatic
        fun getInstance(): FacePass? {
            if (mFacePass == null) LogUtil.d("FacePass", "算法未初始化")
            return mFacePass
        }
    }

    @Volatile
    private var rgbCameraChannel: Channel<CameraPreviewData>? = Channel(2)         //彩色原始数据

    @Volatile
    private var irCameraChannel: Channel<CameraPreviewData>? = Channel(2)    //黑白原始数据

    @Volatile
    private var previewChannel: Channel<CameraPreviewData>? = Channel(2)             //预览数据

    @Volatile
    private var livenessChannel: Channel<FacePassDetectionResult>? = Channel(1)      //活检数据

    private var initializeResult = -1  //算法初始化结果（-1（未初始化），0（初始化成功），1（初始化出错））
    private var rect: Rect? = null      //人脸识别区域
    private var isFreedInterrupt = false
    private var cameraWidth = 0
    private var cameraHeight = 0
    private var recognizeCallback: FaceResultListener? = null

    private var detect = AtomicBoolean(false)//默认开启检测人体
    private var minFaceThreshold = 125

    private var preViewRotation = 0
    private var mirror = true

    private var converJob: Job? = null
    private var livenessJob: Job? = null
    private var feedJob: Job? = null

    private var roll = 15f
    private var pitch = 25f
    private var yaw = 25f

    private var searchThreshold = 65f
    private var livenessThreshold = 65f
    private var blurThreshold = 0.3f

    @Volatile
    private var discard = 2  //防止上次缓存存留

    /**
     * 活检回调
     */
    fun setListener(callback: FaceResultListener?) {
        recognizeCallback = callback
    }

    /**
     * 开启人脸检测
     */
    fun openDetect(){
        detect.set(true)
    }

    /** 配置人脸算法 **/
    fun facePassConfig(): Boolean {
        if (!FacePassHandler.isAvailable()) return false
        if (mFacePassHandler != null) return true
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
            mFacePassHandler = FacePassHandler()
            val ret: Int = mFacePassHandler!!.initHandle(config)
            if (ret != 0) {
                LogUtil.e(TAG, "Build FacePassHandler failed, error: $ret")
                return false
            }
            LogUtil.i(TAG, "算法模型配置成功...")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, "相机算法初始化失败...")
            return false
        }
    }

    fun release() {
        mFacePassHandler?.release()
        mFacePassHandler = null
    }

    private fun createChannel() {
        if (rgbCameraChannel == null) rgbCameraChannel = Channel(2)
        if (irCameraChannel == null) irCameraChannel = Channel(2)
        if (previewChannel == null) previewChannel = Channel(2)
        if (livenessChannel == null) livenessChannel = Channel(1)
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

    /**
     * 开关活体
     */
    fun setLiveness(enable: Boolean) {
        detect.set(enable)
        mFacePassHandler?.config?.rgbIrLivenessEnabled = enable
        LogUtil.d(TAG, "活检是否打开：${enable}")
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
            rgbCameraChannel?.let { it.trySend(cameraPreviewData).isSuccess }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onIRPreviewDataTaken(cameraPreviewData: CameraPreviewData) {
        try {
            irCameraChannel?.let { it.trySend(cameraPreviewData).isSuccess }
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
        checkLiveness()
        feedJob = mScope.launch {
            while (isActive && !isFreedInterrupt && (initializeResult != 1) && (rgbCameraChannel != null) && (irCameraChannel != null)) {
//                LogUtil.d(TAG, "进入人脸检测协程")
                if (mFacePassHandler == null) {
                    delay(50)
                    continue
                }
                try {
                    val rgbReceive = rgbCameraChannel!!.receive()
                    val irReceive = irCameraChannel!!.receive()
//                    LogUtil.d(TAG, "接收预览帧")
                    previewChannel?.let { it.trySend(rgbReceive).isSuccess }
//                    LogUtil.d(TAG, "发送预览帧")
                    if (!detect.get()) continue   //关闭检测就返回
                    val rgbImage = FacePassImage(rgbReceive.nv21Data, rgbReceive.width, rgbReceive.height, rgbReceive.rotation, FacePassImageType.NV21)
                    val irImage = FacePassImage(irReceive.nv21Data, irReceive.width, irReceive.height, irReceive.rotation, FacePassImageType.NV21)

                    preViewRotation = rgbReceive.preRotation
                    mirror = rgbReceive.mirror

                    yield()
                    val detectionResult = mFacePassHandler!!.feedFrameRGBIR(rgbImage, irImage)
                    if (detectionResult == null || detectionResult.faceList.isEmpty()) LogUtil.d(TAG, "未检测到人脸")
                    else {
                        //检测是否遮挡
                        val lmkoccsta = lmkoccsta(detectionResult.faceList[0])
                        if (lmkoccsta && detect.get()) recognizeCallback?.onTips("人脸被遮挡")
                        if (isActive && detectionResult.message.isNotEmpty()) {
                            livenessChannel?.let { it.trySend(detectionResult).isSuccess }
                            LogUtil.d(TAG, "发送活检")
                        }else{
                            Log.d(TAG, "feedFrame: 数据为空 $isActive ${detectionResult.message}")
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
                    val detectionResult = livenessChannel?.receive()
//                    LogUtil.d(TAG, "${!detect.get()} 和 ${detectionResult==null}")
                    if (!detect.get()) continue
//                    LogUtil.d(TAG, "检测识别区域")
                    //检测识别区域
                    val judgeArea = judgeArea(detectionResult!!.faceList[0], preViewRotation, mirror)
//                    LogUtil.d(TAG, "识别区域：$judgeArea")
                    when (judgeArea) {
                        true -> {
                            LogUtil.d(TAG, "识别人脸通过，进入活检")
//                            deleteAllFacePicture()
                            val livenessClassify = mFacePassHandler!!.livenessClassify(detectionResult.message)
                            discard--
//                            LogUtil.d(TAG, "$discard")
                            if (discard < 0) {
                                LogUtil.i(
                                    TAG,
                                    "活体阈值 -> ${livenessClassify[0].livenessThreshold} 活体识别分数 -> ${livenessClassify[0].livenessScore} FacePassLivenessState -> ${livenessClassify[0].livenessState}"
                                )
                                when (livenessClassify[0].livenessState) {
                                    0 -> {
//                                        feedJob?.cancel()
                                        //活检通过
                                        if (detect.get() && (recognizeCallback != null)) {
                                            val facePassImage = detectionResult.images[0]
                                            /* 保存图片需要进行裁剪 */
                                            val path = savePicture(facePassImage.image, facePassImage.width, facePassImage.height, facePassImage.rect_ex, preViewRotation, mirror)
                                            if (path != null) {
                                                detect.set(false)
                                                setTrackStatus(detectionResult.faceList[0].trackId, FacePassTrackIdState.TRACK_ID_RETRY)
                                                val recognize = recognize(detectionResult)
                                                recognizeCallback?.onRecognized(recognize, path)
                                            }
                                        }
//                                        ensureActive()
                                    }
                                    else -> {
                                        //活检失败
                                        when (livenessClassify[0].livenessErrorCode) {
                                            1 -> if (detect.get()) recognizeCallback?.onTips("没有检测到人脸")
                                            2 -> if (detect.get()) recognizeCallback?.onTips("请靠近一点")
                                            else -> if (detect.get()) recognizeCallback?.onTips("未知错误!")
                                        }
                                    }
                                }
                            }
                        }
                        false -> {
                            LogUtil.d(TAG, "不在活检有效区域")
                            if (detect.get()) recognizeCallback?.onTips("不在活检有效区域")
                        }
                    }
                    mFacePassHandler?.setMessage(detectionResult.faceList[0].trackId, FacePassTrackIdState.TRACK_ID_RETRY)
//                    LogUtil.d(TAG, detectionResult.faceList[0].trackId.toString())
                } catch (e: Exception) {
                    LogUtil.w(TAG, "识别处理异常 ${e.message}")
//                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 裁剪图片保存在指定目录
     * 返回绝对路径
     */
    private fun savePicture(data: ByteArray, width: Int, height: Int, rect: FacePassRect, rotation: Int, mirror: Boolean): String? {
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
        } catch (e: Exception) {
            LogUtil.e(TAG, "文件保存中出错" + e.printStackTrace())
            filePath?.also { filePath.delete() }
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
        files?.forEach { it.delete() }
//        LogUtil.d(TAG, "删除指定目录下所有人脸图片")
    }


    /**
     * NV21格式转Bitmap预览帧
     * 带转角和镜像
     */
    private fun convert() {
        converJob = mScope.launch {
            var ruslt = ByteArray(0)
            var rgbReceive: CameraPreviewData
            while (isActive && previewChannel != null) {
                try {
                    rgbReceive = previewChannel!!.receive()
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
                    recognizeCallback?.onPreView(ruslt, width, height)
//                    LogUtil.d(TAG, "预览回调: $ruslt")
                } catch (e: Exception) {
                    LogUtil.w(TAG, "预览帧处理异常  ${e.message}")
                }
            }
        }
    }

    /**
     * 关闭协程
     */
    fun closeJob() {
        runBlocking {
            try {
                recognizeCallback = null
                this@FacePass.rect = null
                detect.set(false)
//                converJob?.cancelAndJoin()
//                feedJob?.cancelAndJoin()
//                livenessJob?.cancelAndJoin()
                converJob?.cancel()
                feedJob?.cancel()
                livenessJob?.cancel()
                clearChannel()
                deleteAllFacePicture()
                LogUtil.d(TAG, "关闭协程 => 清除所有存在的相机帧数据")
            } catch (e: Exception) {
                e.printStackTrace()
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
        LogUtil.d(
            TAG, " \r\n识别区域:{left:${rect?.left},top:${rect?.top},right:${rect?.right},bottom:${rect?.bottom}}\n" +
                    "当前区域：left:${drect.left}  top:${drect.top} right:${drect.right}, bottom:${drect.bottom}"
        )
        if (drect.left >= rect!!.left && drect.top >= rect!!.top && drect.right <= rect!!.right && drect.bottom <= rect!!.bottom) return true
        return false
    }

    /*******************************   人脸、入库   *******************************/
    /** 新增底库 **/
    fun createFaceGroup(groupName: String): Boolean {
        if (mFacePassHandler == null) return false
        var isSuccess = false
        /* 判断底库是否存在 */
        val localGroups: Array<String>? = mFacePassHandler?.localGroups
        localGroups?.forEach { if (groupName == it) isSuccess = true }
        if (!isSuccess) {
            LogUtil.d(TAG, "createFaceGroup ==> 正在创建底库")
            try {
                isSuccess = mFacePassHandler?.createLocalGroup(groupName) ?: false
            } catch (e: FacePassException) {
                e.printStackTrace()
            }
        }
        if (isSuccess) GROUP_NAME = groupName
        return isSuccess
    }

    /** 添加人脸 faceToken **/
    fun registerFace(imageUrl: String): ByteArray? {
        if (mFacePassHandler == null) return null
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

    /** 图片提取特征值 **/
    fun extractFeature(imageUrl: String): ByteArray? {
        if (mFacePassHandler == null) return null
        var featureData: ByteArray? = null
        val bitmap = BitmapFactory.decodeFile(imageUrl)
        try {
            val extractFeature = mFacePassHandler?.extractFeature(bitmap)
            featureData = extractFeature?.featureData ?: ByteArray(1)
        } catch (e: FacePassException) {
            e.printStackTrace()
            LogUtil.d(TAG, "抽取特征值异常")
        }
        return featureData
    }

    //注册人脸
    fun registerFaces(message: String): String {
        try{
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
        }catch (e: Exception){
            LogUtil.e(TAG,e.message)
            return ""
        }
    }

    /** 图片提取特征值 **/
    fun extractFeature(bitmap: Bitmap): ByteArray? {
        if (mFacePassHandler == null) {
            LogUtil.d(TAG, "extractFeature: mFacePassHandler对象为null，特征值提取失败")
            return null
        }
        var featureData: ByteArray? = null
        try {
            val extractFeature = mFacePassHandler?.extractFeature(bitmap)
            if(extractFeature == null) LogUtil.d(TAG, "extractFeature: 特征值提取结果为空，提取失败")
            featureData = extractFeature?.featureData
        } catch (e: FacePassException) {
            e.printStackTrace()
            LogUtil.d(TAG, "抽取特征值异常")
        }
        return featureData
    }

    /** 特征值提取 faceToken **/
    fun insertFeature(feature: String): String {
        if (mFacePassHandler == null) {
            LogUtil.d(TAG, "registerFace ==> facePassHandler is null")
            return ""
        }
        var faceToken = ""
        try {
            val token = stringToByteArray(feature)
            faceToken = mFacePassHandler?.insertFeature(token, FacePassFeatureAppendInfo()) ?: ""
        } catch (e: FacePassException) {
            e.printStackTrace()
        }
        return faceToken
    }

    /** 绑定底库和人脸 **/
    fun bindFaceToGroup(faceToken: ByteArray?): Boolean {
        if (mFacePassHandler == null || GROUP_NAME.isEmpty()) return false
        if (faceToken == null || faceToken.isEmpty()) {
            LogUtil.d(TAG, "bindFaceToGroup ==> params is empty")
            return false
        }
        var isSuccess = false
        try {
            isSuccess = mFacePassHandler?.bindGroup(GROUP_NAME, faceToken) ?: false
            if (isSuccess) LogUtil.d(TAG, "bindFaceToGroup ==> bind success")
            else LogUtil.d(TAG, "bindFaceToGroup ==> bind failure")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.d(TAG, "bindFaceToGroup ==> ${e.message}")
        }
        return isSuccess
    }

    //将人脸绑定底库
    fun bindFaceToGroup(insertFeature: String): Boolean {
        if(mFacePassHandler == null){
            return false
        }
        var b = false
        val faceToken = insertFeature.toByteArray()
        if (faceToken.isEmpty() || TextUtils.isEmpty(GROUP_NAME)) {
            LogUtil.d(TAG, "params error！")
            return false
        }
        try {
            b = mFacePassHandler!!.bindGroup(GROUP_NAME,faceToken)
        }catch (e: Exception){
            e.printStackTrace()
            LogUtil.d(TAG, e.message!!)
        }
        return b
    }

    /** 添加相机帧 **/
    fun getFacePassImage(cameraPreviewData: CameraPreviewData): FacePassDetectionResult? {
        if (mFacePassHandler == null) return null
        return try {
            val image = FacePassImage(cameraPreviewData.nv21Data, cameraPreviewData.width, cameraPreviewData.height, cameraPreviewData.rotation, FacePassImageType.NV21)
            mFacePassHandler?.feedFrame(image)
        } catch (e: FacePassException) {
            e.printStackTrace()
            null
        }
    }

    /** 识别人脸 **/
    fun recognize(detectionResult: FacePassDetectionResult): FacePassRecognitionResult? {
        if (mFacePassHandler == null || GROUP_NAME.isEmpty()) return null
        try {
            val recognizeResult: Array<FacePassRecognitionResult> = mFacePassHandler?.recognize(GROUP_NAME, detectionResult.message) as Array<FacePassRecognitionResult>
            if (recognizeResult.isNotEmpty()) {
                recognizeResult.forEach {
                    if (FacePassRecognitionState.RECOGNITION_PASS == it.recognitionState) {
                        LogUtil.d(
                            TAG,
                            "TrackId:${it.trackId}\n" +
                                    "人脸识别分数:${it.detail.searchScore}\n" +
                                    "活体检测分数:${it.detail.livenessScore}\n" +
                                    "识别结果:${it.recognitionState}\n" +
                                    "FaceToken:${String(it.faceToken)}"
                        )
                        return it
                    }
                }
            } else {
                LogUtil.d(TAG, "陌生人")
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 设置trackId状态
     * TRACK_ID_RETRY: retry 继续产生送识别的message
     * TRACK_ID_PASS: pass 不再产生送识别的message
     * TRACK_ID_UNPASS: unpass 不再送识别的message
     */
    fun setTrackStatus(trackId: Long, trackStatus: Int) {
        if (mFacePassHandler == null) return
        try {
            mFacePassHandler?.setMessage(trackId, trackStatus)
        } catch (e: FacePassException) {
            e.printStackTrace()
        }
    }

    fun faceTokenUnbindGroup(faceToken: String): Boolean {
        // 解绑
        if (unbindFace(faceToken, GROUP_NAME)) {
            // 删除
            if (deleteFace(faceToken)) return true
        }
        return false
    }

    /** 解绑人脸 **/
    private fun unbindFace(faceToken: String, groupName: String): Boolean {
        try {
            mFacePassHandler?.unBindGroup(groupName, faceToken.toByteArray())
        } catch (e: FacePassException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /** 删除人脸 **/
    fun deleteFace(faceToken: String): Boolean {
        try {
            mFacePassHandler?.deleteFace(faceToken.toByteArray())
        } catch (e: FacePassException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun initFaceLocalGroup(groupName: String): Int? {
        return mFacePassHandler?.initLocalGroup(groupName)
    }

    fun getVersion(): String? {
        return mFacePassHandler?.featureVersion
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
        }catch (e: FacePassException){
            e.printStackTrace()
        }
        return isSuccess
    }

    private fun stringToByteArray(byteString: String): ByteArray {
        // 去掉字符串中的方括号
        val trimmedString = byteString.substring(1, byteString.length - 1)
        // 按逗号分割字符串
        val byteStrings = trimmedString.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // 创建 byte 数组
        val bytes = ByteArray(byteStrings.size)
        // 将每个字符串解析为 byte
        for (i in byteStrings.indices) {
            bytes[i] = byteStrings[i].toByte()
        }
        return bytes
    }
}