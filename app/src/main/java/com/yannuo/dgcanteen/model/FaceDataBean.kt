package com.yannuo.dgcanteen.model

import android.graphics.RectF
import com.yannuo.dgcanteen.facepass.CameraPreviewData
import mcv.facepass.types.FacePassDetectionResult

class FaceDataBean {
    lateinit var detectionResult : FacePassDetectionResult  //此帧检测结果
    var colorReceive : CameraPreviewData?= null  //全景图片
    var index = 0    //最大人脸索引
    var rect : RectF?= null //最大人脸框坐标
    var trackId = 0L  //人脸的id

}