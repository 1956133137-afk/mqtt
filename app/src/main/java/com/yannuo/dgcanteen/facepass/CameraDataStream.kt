package com.yannuo.dgcanteen.facepass

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.yannuo.dgcanteen.util.LogUtil


/**
 * author:Almighty
 * date: 2022/5/23 19:26.
 */
class CameraDataStream(context :Context) {
    private var mContext :Context? = context
    private var mBlackWhitePreview: SurfaceView? = null
    private var mColorPreview: SurfaceView? = null
    private var mBlackWhitePreviewHolder: SurfaceHolder? = null
    private var mColorPreviewHolder: SurfaceHolder? = null
    private val TAG = "CameraDataStream"
    private var camera1: Camera? = null
    private var camera2: Camera? = null
    private var listener: CameraListener? = null

    private var previewSizeOne : Camera.Size? = null
    private var previewSizeTwo : Camera.Size? = null
    private var colorCameraRotation = 0
    private var colorPreRotation = 0

    private var blackCameraRotation = 0
    private var blackPreRotation = 0

    private var colorMirror = false
    private var blackMirror = false

    init {
//        initHolder()
        mBlackWhitePreview = SurfaceView(mContext)
        mColorPreview = SurfaceView(mContext)
    }


    fun setDisplayView(cameraPreview : SurfaceView){
        mColorPreview = cameraPreview
    }

    /**
     * 初始化视图Holder
     */
     fun initHolder() {

        mBlackWhitePreviewHolder = mBlackWhitePreview!!.holder
        mColorPreviewHolder = mColorPreview!!.holder
//        mBlackWhitePreviewHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
//        mColorPreviewHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mBlackWhitePreviewHolder!!.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                LogUtil.d(TAG, "blackWhitePreviewHolder surfaceCreated!")
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                LogUtil.d(TAG, "blackWhitePreviewHolder surfaceChanged!")
                restartPreview(holder, 2)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                LogUtil.d(TAG, "blackWhitePreviewHolder surfaceDestroyed!")
            }
        })
        mColorPreviewHolder!!.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                LogUtil.d(TAG, "colorPreviewHolder surfaceCreated!")
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                LogUtil.d(TAG, "colorPreviewHolder surfaceChanged!")
                restartPreview(holder, 1)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                LogUtil.d(TAG, "colorPreviewHolder surfaceDestroyed!")
            }
        })
    }

    fun release(){
        camera1?.stopPreview()
        camera2?.stopPreview()
        camera1?.setPreviewCallback(null)
        camera1?.release()
        camera2?.setPreviewCallback(null)
        camera2?.release()
        camera1 = null
        camera2 = null
//        mContext = null
    }

    fun setCamera(camera: Camera, id: Int) {
        when (id) {
            1 -> {
                this.camera1 = camera
                restartPreview(mColorPreviewHolder!!, 1) //1彩色
            }
            2 -> {
                this.camera2 = camera
                restartPreview(mBlackWhitePreviewHolder!!, 2) //2黑白
            }
        }
    }


    /**
     * 声明送识别的相机帧配置
     */
    fun setParameter(one : Camera.Size, two : Camera.Size,cRotation : Int,cPreRotation : Int,bRotation : Int,bPreRotation : Int,mirror : Boolean,bMirror : Boolean){
        previewSizeOne = one
        previewSizeTwo = two
        colorCameraRotation = cRotation
        colorPreRotation = cPreRotation
        blackCameraRotation = bRotation
        blackPreRotation = bPreRotation
        colorMirror = mirror
        blackMirror = bMirror
    }


    /**
     *设置设置相机并开启预览
     */
    private fun restartPreview(holder: SurfaceHolder, id: Int) {
        when (id) {
            1 -> if (camera1 != null) {
                if (holder.surface == null) return
                try {
                    camera1?.stopPreview()
                    camera1?.setPreviewDisplay(holder)
                    camera1?.startPreview()
                    camera1?.setPreviewCallbackWithBuffer(PreviewCallback { data, camera ->
                        camera.addCallbackBuffer(data)
                        listener?.onPictureTakenColor(
                            CameraPreviewData(
                                data,
                                previewSizeOne!!.width, previewSizeOne!!.height,
                                colorCameraRotation, colorPreRotation, colorMirror
                            )
                        )
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            2 -> if (camera2 != null) {
                if (holder.surface == null) return
                try {
                    camera2?.stopPreview()
                    camera2?.setPreviewDisplay(holder)
                    camera2?.startPreview()
                    camera2?.setPreviewCallbackWithBuffer(PreviewCallback { data, camera ->
                        camera.addCallbackBuffer(data)
                        listener?.onPictureTakenBlackWhite(
                            CameraPreviewData(
                                data,
                                previewSizeTwo!!.width, previewSizeTwo!!.height,
                                blackCameraRotation, blackPreRotation, blackMirror
                            )
                        )
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    interface CameraListener {
        /**
         * 黑白摄像头数据帧回调
         */

        fun onPictureTakenBlackWhite(cameraPreviewData: CameraPreviewData)

        /***
         *
         * 彩色摄像头数据帧回调
         */
        fun onPictureTakenColor(cameraPreviewData: CameraPreviewData)
    }

    //    public void finalRelease() {
    //        this.listener = null;
    //        this.cameraPreview = null;
    //        this.surfaceHolder = null;
    //    }
    //
    //    public void setPreviewDisplay(CameraPreview preview) {
    //        this.cameraPreview = preview;
    //        this.surfaceHolder = preview.getHolder();
    //        preview.setListener(this);
    //    }

    fun setListener(listener: CameraListener) {
        this.listener = listener
    }

}