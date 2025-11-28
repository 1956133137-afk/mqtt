package com.yannuo.dgcanteen.facepass

interface FaceInitListener {
    /**
     * 人脸算法初始化结果
     */
    fun faceInitResult(code: Int, message: String)

    /**
     * 人脸算法授权结果
     */
    fun faceLicenseResult(code: Int, message: String)
}