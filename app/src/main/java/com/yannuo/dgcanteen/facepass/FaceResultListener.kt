package com.yannuo.dgcanteen.facepass

import mcv.facepass.types.FacePassRecognitionResult

interface FaceResultListener {
    /**
     * 人脸SDK初始化结果
     * @param code 0-初始化成功 其它-初始化失败
     * @param message 初始化信息
     */
    fun onInitFace(code: Int, message: String = "")

    /**
     * 摄像头预览画面
     * @param data 预览数据
     * @param width 图片宽度
     * @param height 图片高度
     */
    fun onPreView(data: ByteArray, width: Int, height: Int)

    /**
     * 活检结果
     * @param data 识别数据
     * @param width 图片宽度
     * @param height 图片宽度
     */
    fun onLiveness(data: ByteArray, width: Int, height: Int) {}

    /**
     * 活检结果
     *
     * @param path 图片路径
     */
    fun onLiveness(path: String): Boolean = false

    /**
     * 人脸识别
     *
     * @param result 识别结果
     * @param path 识别图片
     */
    fun onRecognized(result: FacePassRecognitionResult?, path: String = "") {}

    /** 活检提示 */
    fun onTips(msg: String)

    /** 活检错误 */
    fun onError(errCode: String, errMsg: String)

    /** 活检取消 */
    fun onCancel()
}