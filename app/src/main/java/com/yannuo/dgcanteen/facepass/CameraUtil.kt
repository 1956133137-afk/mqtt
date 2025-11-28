package com.yannuo.dgcanteen.facepass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import android.util.Base64
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class CameraUtil {
    private val TAG = javaClass.simpleName
    private var mContext: Context? = null
    private val cameraIdList: MutableList<String> = mutableListOf()

    private var rs: RenderScript? = null
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    private var yuvType: Type.Builder? = null
    private var rgbaType: Type.Builder? = null
    private var ing: Allocation? = null
    private var outing: Allocation? = null

    companion object {
        @JvmStatic
        val instance: CameraUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(CameraUtil::class.java) { CameraUtil() }
        }
    }

    fun initCamera(context: Context) {
        mContext = context
        rs = RenderScript.create(mContext)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val cameraManager = mContext!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 获取相机物理id
        cameraIdList.clear()
        cameraManager.cameraIdList.forEach {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != null) cameraIdList.add(it)
        }
    }

    fun getCameraIdList(): MutableList<String> = cameraIdList

    fun getCameraId(cameraId: Int): String {
        return if (cameraId >= cameraIdList.size) cameraIdList[0] else cameraIdList[cameraId]
    }

    /**
     * 旋转角度
     */
    fun getRotateBitmap(bitmap: Bitmap, rotateDegree: Float, mirror: Boolean, filter: Boolean): Bitmap {
        // 创建操作图片用的矩阵对象
        val matrix = Matrix()
        // 执行图片的旋转动作
        matrix.postRotate(rotateDegree)
        // 执行镜像翻转
        matrix.postScale(if (mirror) -1F else 1F, 1F)
        // 创建并返回旋转后的位图对象
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, filter)
    }

    /**
     * 截取图片
     */
    fun cropBitmap(srcBitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Bitmap {
        val newLeft = Math.max(0, left)
        val newTop = Math.max(0, top)
        val newRight = Math.min(srcBitmap.width, right)
        val newBottom = Math.min(srcBitmap.height, bottom)
        val width = newRight - newLeft
        val height = newBottom - newTop
        return Bitmap.createBitmap(srcBitmap, newLeft, newTop, width, height)
    }

    /**
     * Bitmap设置大小
     */
    fun bitmapSize(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
//        val matrix = Matrix()
//        matrix.postScale(newWidth.toFloat() / bitmap.width, newHeight.toFloat() / bitmap.height)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 矢量图 转 Bitmap
     */
    fun getVectorBitmap(drawableId: Int): Bitmap? {
        if (mContext == null) return null
        val drawable = ContextCompat.getDrawable(mContext!!, drawableId) as Drawable
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 图片数据 转 Base64
     */
    fun imageUrlToBase64(imageUrl: String): String {
        val bitmap = BitmapFactory.decodeFile(imageUrl)
        val byteArray = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray)
        return Base64.encodeToString(byteArray.toByteArray(), Base64.DEFAULT)
    }

    /**
     * bitmap 转 base64
     */
    fun bitmapToBase64(bitmap: Bitmap?): String {
        var base64Str = ""
        if (bitmap == null) return base64Str
        var byteArrayOutputStream: ByteArrayOutputStream? = null
        try {
            byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            base64Str = Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                byteArrayOutputStream?.flush()
                byteArrayOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return base64Str.replace("\n", "").trim()
    }

    /**
     * NV21 转 bitmap
     */
    fun nv21ToBitmap(nv21: ByteArray?, width: Int, height: Int): Bitmap? {
        if (nv21 == null) return null
        var bitmap: Bitmap? = null
        try {
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val byteArrayOutputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, byteArrayOutputStream)
            bitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size())
            byteArrayOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }

    fun nv21ToBitmap2(nv21: ByteArray, width: Int, height: Int): Bitmap {
        yuvType = Type.Builder(rs, Element.U8(rs)).setX(nv21.size)
        ing = Allocation.createTyped(rs, yuvType?.create(), Allocation.USAGE_SCRIPT)
        rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        outing = Allocation.createTyped(rs, rgbaType?.create(), Allocation.USAGE_SCRIPT)
        ing?.copyFrom(nv21)
        yuvToRgbIntrinsic?.setInput(ing)
        yuvToRgbIntrinsic?.forEach(outing)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outing?.copyTo(bitmap)
        return bitmap
    }

    /**
     * Bitmap 转 N21
     */
    fun bitmapToNv21(bitmap: Bitmap): ByteArray {
        val argb = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return argbToNv21(argb, bitmap.width, bitmap.height)
    }

    private fun argbToNv21(argb: IntArray, width: Int, height: Int): ByteArray {
        var yIndex = 0
        var uvIndex = width * height
        var index = 0
        val nv21 = ByteArray(width * height * 3 / 2)
        for (j in 0 until height) {
            for (i in 0 until width) {
                val R = argb[index] and 0xFF0000 shr 16
                val G = argb[index] and 0x00FF00 shr 8
                val B = argb[index] and 0x0000FF
                val Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                val U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                val V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
                nv21[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.size - 2) {
                    nv21[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    nv21[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                }
                ++index
            }
        }
        return nv21
    }
}