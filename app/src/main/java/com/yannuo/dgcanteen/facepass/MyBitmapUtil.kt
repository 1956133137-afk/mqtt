package com.yannuo.dgcanteen.facepass

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.*

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
}