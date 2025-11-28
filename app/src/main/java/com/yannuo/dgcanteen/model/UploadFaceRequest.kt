package com.yannuo.dgcanteen.model

data class UploadFaceRequest(
    var userId: String = "", //用户ID
    var custId: String = "", //custId
    var campusId: String = "", //园区ID
    var eigenvalue: String = "", //特征值
    var version: String = "", //版本号
)

data class UploadFaceImageRequest(
    var userId: String = "", //用户ID
    var custId: String = "", //custId
    var campusId: String = "", //园区ID
    var faceBase64: String = "", //人脸base64
)
