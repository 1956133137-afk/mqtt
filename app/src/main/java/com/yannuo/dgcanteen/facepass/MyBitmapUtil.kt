package com.yannuo.dgcanteen.facepass

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.*
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

class MyBitmapUtil(context: Context) {
    private var rs: RenderScript?= null
    private var yuvToRgbIntrinsic : ScriptIntrinsicYuvToRGB?= null
    private var yuvType : Type.Builder ?=null
    private var rgbaType : Type.Builder ?=null
    private var ing : Allocation?=null
    private var outing : Allocation?=null

    init {
        rs = RenderScript.create(context)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    }

    fun nv21ToBitmap( nv21 :ByteArray,  width :Int, height :Int) : Bitmap {
        yuvType = Type.Builder(rs, Element.U8(rs)).setX(nv21.size)
        ing = Allocation.createTyped(rs, yuvType?.create(), Allocation.USAGE_SCRIPT)
        rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        outing = Allocation.createTyped(rs, rgbaType?.create(), Allocation.USAGE_SCRIPT)

        ing?.copyFrom(nv21)
        yuvToRgbIntrinsic?.setInput(ing)
        yuvToRgbIntrinsic?.forEach(outing)
        val bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outing?.copyTo(bmpout)
        return bmpout
    }

    fun bitmapToBase64(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        // 压缩成字节数组
        bitmap.compress(format, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP) // NO_WRAP避免换行
    }

    fun imagePathToBase64(path: String): String {
        val file = File(path)
        val bytes = file.readBytes()
        // NO_WRAP：避免换行符，适合上传服务器
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}