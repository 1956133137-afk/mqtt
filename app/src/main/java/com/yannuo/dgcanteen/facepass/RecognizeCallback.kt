package com.yannuo.dgcanteen.facepass;

import android.graphics.Bitmap


interface RecognizeCallback {

    /** 摄像头预览画面 */
//    fun onPreView(data :ByteArray,  width :Int,  height :Int)

    /** 活检结果 */
    /** 活检结果 */
    fun onRecognized(cropBitmap :Bitmap,byteArray: ByteArray ,rect : DoubleArray,width: Int,height: Int,
                     livenessThreshold :String, livenessScore: String)

    /** 活检提示 */
    fun onTips(msg :String)

    /** 活检错误 */
//    fun onError(errCode :String,  errMsg :String)

    /** 活检取消 */
    /**
     *
     * @param code Int 0成功
     * @param message String
     */
    fun onInitCode(code :Int ,message : String)
}

