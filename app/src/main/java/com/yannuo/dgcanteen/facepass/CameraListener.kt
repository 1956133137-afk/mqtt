package com.yannuo.dgcanteen.facepass

interface CameraListener {
    /***
     * 彩色摄像头数据帧回调
     */
    fun onRGBPreviewDataTaken(cameraPreviewData: CameraPreviewData)

    /**
     * 黑白摄像头数据帧回调
     */
    fun onIRPreviewDataTaken(cameraPreviewData: CameraPreviewData)
}