package com.yannuo.dgcanteen.facepass

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.view.SurfaceHolder
import android.view.TextureView
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.util.LogUtil
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

class CameraManager {
    private val TAG = javaClass.simpleName

    private var cameraFront = true
    private var mRGBCameraId = -1   //彩色摄像头
    private var mIRCameraId = -1    //黑白摄像头
    private var mRGBCamera: Camera? = null
    private var mIRCamera: Camera? = null
    private var rgbPreviewWidth = 1280  //彩色预览宽度
    private var rgbPreviewHeight = 720  //彩色预览高度
    private var irPreviewWidth = 1280   //黑白预览宽度
    private var irPreviewHeight = 720  //黑白预览高度
    private var rgbPreviewRotation = 0  //彩色预览旋转
    private var rgbCameraRotation = 0   //彩色摄像头旋转
    private var irPreviewRotation = 0   //黑白预览旋转
    private var irCameraRotation = 0    //黑白摄像头旋转
    private var rgbMirror = false       //彩色摄像头镜像
    private var irMirror = false        //黑白摄像头镜像
    private var textureViewMirror = false   //TextureView预览镜像

    private var mContext: Context? = null

    private var mFacePass: FacePass? = null
    private val wan_Height: Short = 0
    private val wan_Width: Short = 0

    @Volatile
    private var state: CameraState = CameraState.IDEL
    private val surfaceTexture1 = SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    private val surfaceTexture2 = SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

    private var irFrameCount = 0
    private var rgbFrameCount = 0
    private var irFrameCountListener: FrameCountListener? = null
    private var rgbFrameCountListener: FrameCountListener? = null

    enum class CameraState {
        IDEL, OPENING, OPENED
    }

    fun initAlgorithm(facePass: FacePass): CameraManager {
        this.mFacePass = facePass
        return this
    }

    fun getCameraManager(): CameraManager = this

    fun setCameraFront(cameraFront: Boolean): CameraManager {
        this.cameraFront = cameraFront
        return this
    }

    fun setRGBPreview(rgbPreviewWidth: Int, rgbPreviewHeight: Int): CameraManager {
        this.rgbPreviewWidth = rgbPreviewWidth
        this.rgbPreviewHeight = rgbPreviewHeight
        return this
    }

    fun setIRPreview(irPreviewWidth: Int, irPreviewHeight: Int): CameraManager {
        this.irPreviewWidth = irPreviewWidth
        this.irPreviewHeight = irPreviewHeight
        return this
    }

    fun setRGBRotation(rgbPreviewRotation: Int, rgbCameraRotation: Int): CameraManager {
        when (rgbPreviewRotation) {
            0, 90, 180, 270 -> this.rgbPreviewRotation = rgbPreviewRotation
        }
        when (rgbCameraRotation) {
            0, 90, 180, 270 -> this.rgbCameraRotation = rgbCameraRotation
        }
        return this
    }

    fun setIRRotation(irPreviewRotation: Int, irCameraRotation: Int): CameraManager {
        when (irPreviewRotation) {
            0, 90, 180, 270 -> this.irPreviewRotation = irPreviewRotation
        }
        when (irCameraRotation) {
            0, 90, 180, 270 -> this.irCameraRotation = irCameraRotation
        }
        return this
    }

    fun setCameraMirror(rgbMirror: Boolean, irMirror: Boolean): CameraManager {
        this.rgbMirror = rgbMirror
        this.irMirror = irMirror
        return this
    }

    fun setTextureViewMirror(textureViewMirror: Boolean): CameraManager {
        this.textureViewMirror = textureViewMirror
        return this
    }

    fun setDetect(enable: Boolean): CameraManager {
        mFacePass?.setDetect(enable)
        return this
    }

    fun getDetect() = mFacePass?.getDetect() ?: false

    fun setLiveness(enable: Boolean): CameraManager {
        mFacePass?.setLiveness(enable)
        return this
    }

    fun setRecognize(enable: Boolean): CameraManager {
        mFacePass?.setRecognize(enable)
        return this
    }

    fun setFaceMinThreshold(threshold: Int): CameraManager {
        mFacePass?.setFaceMinThreshold(threshold)
        return this
    }

    fun setFacePose(roll: Float, pitch: Float, yaw: Float): CameraManager {
        mFacePass?.setFacePose(roll, pitch, yaw)
        return this
    }

    fun setFaceSearchThreshold(searchThreshold: Float): CameraManager {
        mFacePass?.setFaceSearchThreshold(searchThreshold)
        return this
    }

    fun setFaceLivenessThreshold(livenessThreshold: Float): CameraManager {
        mFacePass?.setFaceLivenessThreshold(livenessThreshold)
        return this
    }

    fun setFaceBlurThreshold(blurThreshold: Float): CameraManager {
        mFacePass?.setFaceBlurThreshold(blurThreshold)
        return this
    }

    fun setRgbFrameListener(listener: FrameCountListener): CameraManager {
        rgbFrameCountListener = listener
        return this
    }

    fun setIrFrameListener(listener: FrameCountListener): CameraManager {
        irFrameCountListener = listener
        return this
    }

    /**
     *
     */
    fun getFacePass() = mFacePass

    fun open(rect: Rect, listener: FaceResultListener?): Boolean {
        irFrameCount = 0
        rgbFrameCount = 0
        val openStatus = openCamera(rect, listener)
        if (openStatus) startCameraPreview2()
        return openStatus
    }

    fun open(rect: Rect, textureView: TextureView, listener: FaceResultListener?): Boolean {
        irFrameCount = 0
        rgbFrameCount = 0
        /* 设置TextureView镜像 */
        textureView.scaleX = if (textureViewMirror) -1F else 1F
        val openStatus = openCamera(rect, listener)
        if (openStatus) startCameraPreview2(textureView.surfaceTexture)
        return openStatus
    }

    fun openCamera(rect: Rect, listener: FaceResultListener?): Boolean {
        return if (state != CameraState.OPENING) {
            state = CameraState.OPENING
//            closeCameraw()
            closeCamera()
            try {
                if (Camera.getNumberOfCameras() > 1) {
                    initRGBCamera()
                    initIRCamera()
                } else LogUtil.e(TAG, "相机少于2个")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (mRGBCamera == null || mIRCamera == null) return false
            val mRGBSize = mRGBCamera!!.parameters.previewSize
            val mIRSize = mIRCamera!!.parameters.previewSize

            mFacePass?.setListener(listener)
            mFacePass?.setCameraPreSize(mRGBSize.width, mRGBSize.height)
            mFacePass?.setRect(rect)
            LogUtil.d(TAG, "开启线程feedFrame")
            mFacePass?.feedFrame()
            state = CameraState.OPENED
            LogUtil.i(TAG, "ok camera..")
            true
        } else false
    }

    fun initRGBCamera() {
        mRGBCameraId = if (cameraFront) Camera.CameraInfo.CAMERA_FACING_BACK else Camera.CameraInfo.CAMERA_FACING_FRONT
        mRGBCamera = Camera.open(mRGBCameraId)
        mRGBCamera?.setDisplayOrientation(if (cameraFront) rgbPreviewRotation else irPreviewRotation)
        val rgbParameters = mRGBCamera!!.parameters
        if (cameraFront) {
            if (rgbPreviewWidth > 0 && rgbPreviewHeight > 0 && isSupportedPreviewSize(rgbPreviewWidth, rgbPreviewHeight, rgbParameters)) {
                rgbParameters.setPreviewSize(rgbPreviewWidth, rgbPreviewHeight)
            } else {
                val bestPreviewSize: Camera.Size = getBestPreviewSize(rgbParameters)
//                LogUtil.i(TAG, "rgbParameters best height is ${bestPreviewSize.height} width is ${bestPreviewSize.width}")
                rgbPreviewWidth = bestPreviewSize.width.toShort().toInt()
                rgbPreviewHeight = bestPreviewSize.height.toShort().toInt()
                rgbParameters.setPreviewSize(rgbPreviewWidth, rgbPreviewHeight)
            }
        } else {
            if (irPreviewWidth > 0 && irPreviewHeight > 0 && isSupportedPreviewSize(irPreviewWidth, irPreviewHeight, rgbParameters)) {
                rgbParameters.setPreviewSize(irPreviewWidth, irPreviewHeight)
            } else {
                val bestPreviewSize: Camera.Size = getBestPreviewSize(rgbParameters)
//                LogUtil.i(TAG, "rgbParameters best height is ${bestPreviewSize.height} width is ${bestPreviewSize.width}")
                irPreviewWidth = bestPreviewSize.width.toShort().toInt()
                irPreviewHeight = bestPreviewSize.height.toShort().toInt()
                rgbParameters.setPreviewSize(irPreviewWidth, irPreviewHeight)
            }
        }
        rgbParameters.previewFormat = ImageFormat.NV21
        mRGBCamera!!.parameters = rgbParameters
        val pixelinfo = PixelFormat()
        PixelFormat.getPixelFormatInfo(rgbParameters.previewFormat, pixelinfo)
        val previewSize = rgbParameters.previewSize
        val bufSize = previewSize.width * previewSize.height * pixelinfo.bitsPerPixel / 8
        mRGBCamera!!.addCallbackBuffer(ByteArray(bufSize))
        LogUtil.d(TAG, "mRGBCamera：${previewSize.width} * ${previewSize.height} bufSize--> $bufSize")
    }

    fun initIRCamera() {
        mIRCameraId = if (!cameraFront) Camera.CameraInfo.CAMERA_FACING_BACK else Camera.CameraInfo.CAMERA_FACING_FRONT
        mIRCamera = Camera.open(mIRCameraId)
        mIRCamera?.setDisplayOrientation(if (!cameraFront) rgbPreviewRotation else irPreviewRotation)
        val irParameters = mIRCamera!!.parameters
        if (!cameraFront) {
            if (rgbPreviewWidth > 0 && rgbPreviewHeight > 0 && isSupportedPreviewSize(rgbPreviewWidth, rgbPreviewHeight, irParameters)) {
                irParameters.setPreviewSize(rgbPreviewWidth, rgbPreviewHeight)
            } else {
                val bestPreviewSize: Camera.Size = getBestPreviewSize(irParameters)
//                LogUtil.i(TAG, "rgbParameters best height is ${bestPreviewSize.height} width is ${bestPreviewSize.width}")
                rgbPreviewWidth = bestPreviewSize.width.toShort().toInt()
                rgbPreviewHeight = bestPreviewSize.height.toShort().toInt()
                irParameters.setPreviewSize(rgbPreviewWidth, rgbPreviewHeight)
            }
        } else {
            if (irPreviewWidth > 0 && irPreviewHeight > 0 && isSupportedPreviewSize(irPreviewWidth, irPreviewHeight, irParameters)) {
                irParameters.setPreviewSize(irPreviewWidth, irPreviewHeight)
            } else {
                val bestPreviewSize: Camera.Size = getBestPreviewSize(irParameters)
//                LogUtil.i(TAG, "rgbParameters best height is ${bestPreviewSize.height} width is ${bestPreviewSize.width}")
                irPreviewWidth = bestPreviewSize.width.toShort().toInt()
                irPreviewHeight = bestPreviewSize.height.toShort().toInt()
                irParameters.setPreviewSize(irPreviewWidth, irPreviewHeight)
            }
        }
        irParameters.previewFormat = ImageFormat.NV21
        mIRCamera!!.parameters = irParameters
        val pixelinfo = PixelFormat()
        PixelFormat.getPixelFormatInfo(irParameters.previewFormat, pixelinfo)
        val previewSize = irParameters.previewSize
        val bufSize = previewSize.width * previewSize.height * pixelinfo.bitsPerPixel / 8
        mIRCamera!!.addCallbackBuffer(ByteArray(bufSize))
        LogUtil.d(TAG, "mIRCamera：${previewSize.width} * ${previewSize.height} bufSize--> $bufSize")
    }

    fun startCameraPreview(surfaceHolder1: SurfaceHolder? = null, surfaceHolder2: SurfaceHolder? = null) {
        try {
            if (mRGBCamera != null && mIRCamera != null) {
                Thread {
                    mRGBCamera?.stopPreview()
                    if (surfaceHolder1 != null) mRGBCamera?.setPreviewDisplay(surfaceHolder1)
                    mRGBCamera?.startPreview()
                    mRGBCamera?.setPreviewCallbackWithBuffer(createRGBPreviewCallback())

                    mIRCamera?.stopPreview()
                    if (surfaceHolder2 != null) mIRCamera?.setPreviewDisplay(surfaceHolder2)
                    mIRCamera?.startPreview()
                    mIRCamera?.setPreviewCallbackWithBuffer(createIRPreviewCallback())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startCameraPreview2(surfaceTexture: SurfaceTexture? = null) {
        try {
            if (mRGBCamera != null && mIRCamera != null) {
                Thread {
                    mRGBCamera?.stopPreview()
                    mRGBCamera?.setPreviewTexture(surfaceTexture ?: surfaceTexture1)
                    mRGBCamera?.startPreview()
                    mRGBCamera?.setPreviewCallbackWithBuffer(createRGBPreviewCallback())

                    mIRCamera?.stopPreview()
                    mIRCamera?.setPreviewTexture(surfaceTexture2)
                    mIRCamera?.startPreview()
                    mIRCamera?.setPreviewCallbackWithBuffer(createIRPreviewCallback())
                }.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // RGB摄像头回调的预览帧数据
    private fun createRGBPreviewCallback(): Camera.PreviewCallback {
        return Camera.PreviewCallback { data, camera ->
            rgbFrameCount++
            rgbFrameCountListener?.onFrameCount(rgbFrameCount)
            // LogUtil.d(TAG, "RGB --> 相机数据")
            val rgbPreviewData = CameraPreviewData(
                data,
                rgbPreviewWidth,
                rgbPreviewHeight,
                rgbCameraRotation,
                rgbPreviewRotation,
                rgbMirror
            )
            // 传给人脸识别 FaceViewFragment
            mFacePass?.onRGBPreviewDataTaken(rgbPreviewData)
            camera.addCallbackBuffer(data)
        }
    }

    // IR红外摄像头回调的预览帧数据
    private fun createIRPreviewCallback(): Camera.PreviewCallback {
        return Camera.PreviewCallback { data, camera ->
            irFrameCount++
            irFrameCountListener?.onFrameCount(irFrameCount)
            // LogUtil.d(TAG, "IR --> 相机数据")
            val irPreviewData = CameraPreviewData(
                data,
                irPreviewWidth,
                irPreviewHeight,
                irCameraRotation,
                irPreviewRotation,
                irMirror
            )
            // 传给人脸识别 FaceViewFragment
            mFacePass?.onIRPreviewDataTaken(irPreviewData)
            camera.addCallbackBuffer(data)
        }
    }

    private fun isSupportedPreviewSize(width: Int, height: Int, camPara: Camera.Parameters): Boolean {
        val allSupportedSize = camPara.supportedPreviewSizes
        for (tmpSize in allSupportedSize) {
//            Log.i(TAG, "metrics: support height" + tmpSize.height + "width " + tmpSize.width)
            if (tmpSize.height == height && tmpSize.width == width) return true
        }
        return false
    }

    private fun getBestPreviewSize(camPara: Camera.Parameters): Camera.Size {
        val allSupportedSize = camPara.supportedPreviewSizes
        val widthLargerSize = ArrayList<Camera.Size>()
        var max = Int.MIN_VALUE
        var maxSize: Camera.Size? = null
        for (tmpSize in allSupportedSize) {
            val multi = tmpSize.height * tmpSize.width
            if (multi > max) {
                max = multi
                maxSize = tmpSize
            }
            //选分辨率比较高的
            if (tmpSize.width > tmpSize.height && (tmpSize.width > wan_Height / 2 || tmpSize.height > wan_Width / 2)) {
                widthLargerSize.add(tmpSize)
            }
        }
        if (widthLargerSize.isEmpty()) widthLargerSize.add(maxSize!!)
        val propotion: Float = if (wan_Width >= wan_Height) wan_Width.toFloat() / wan_Height.toFloat() else wan_Height.toFloat() / wan_Width.toFloat()
        Collections.sort(widthLargerSize, object : Comparator<Camera.Size> {
            override fun compare(lhs: Camera.Size, rhs: Camera.Size): Int {
//                 int off_one = Math.abs(lhs.width * lhs.height - Screen.mWidth * Screen.mHeight);
//                 int off_two = Math . abs (rhs.width * rhs.height - Screen.mWidth * Screen.mHeight);
//                 return off_one - off_two;
                //选预览比例跟屏幕比例比较接近的
                val a = getPropotionDiff(lhs, propotion)
                val b = getPropotionDiff(rhs, propotion)
                return ((a - b) * 10000).toInt()
            }
        })
        val minPropotionDiff = getPropotionDiff(widthLargerSize[0], propotion)
        val validSizes = ArrayList<Camera.Size>()
        for (i in widthLargerSize.indices) {
            val size = widthLargerSize[i]
            val propotionDiff = getPropotionDiff(size, propotion)
            if (propotionDiff > minPropotionDiff) break
            validSizes.add(size)
        }
        Collections.sort(validSizes, object : Comparator<Camera.Size> {
            override fun compare(lhs: Camera.Size, rhs: Camera.Size): Int {
                return rhs.width * rhs.height - lhs.width * lhs.height
            }
        })
        return widthLargerSize[0]
    }

    fun getPropotionDiff(size: Camera.Size?, standardPropotion: Float): Float {
        return Math.abs(size!!.width.toFloat() / size.height.toFloat() - standardPropotion)
    }

    fun closeCamera() {
        state = CameraState.IDEL
        LogUtil.d(TAG, "关闭相机 => 停止预览")
        mFacePass?.closeJob()
        releaseCamera()
    }

    fun releaseCamera() {
        if (mRGBCamera != null) {
            mRGBCamera!!.stopPreview()
            mRGBCamera!!.setPreviewCallback(null)
            mRGBCamera!!.release()
            mRGBCamera = null
        }
        if (mIRCamera != null) {
            mIRCamera!!.stopPreview()
            mIRCamera!!.setPreviewCallback(null)
            mIRCamera!!.release()
            mIRCamera = null
        }
    }

    fun release() {
        closeCamera()
        mFacePass?.release()
        mFacePass = null
//        mContext = null
    }
}