package com.yannuo.dgcanteen.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.view.animation.CycleInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.scale
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.util.LogUtil

class CardDrawable(context: Context) : Drawable() {
    private val TAG = javaClass.simpleName
    private var mAlpha = 0xff
    private var cnt = context
    private var mWidth = 100f
    private var mHeight = 100f
    private var fixedW : Int = 0
    private var fixedH : Int = 0
    private lateinit var fixedDrawable :Bitmap
    private lateinit var downMarkDrawable :Bitmap
    private var mCurrentValue = 0
    private lateinit var mTextPaint :Paint
    private var mScalex = 1f
    private var mScaley = 1f
    private var valueAnimator : ValueAnimator ? = null

    init {
        initSize()
        initPaint()
    }

    private fun initSize() {
        val fixed = cnt.resources.getDrawable(R.drawable.ic_machine,null)

        fixedDrawable = Bitmap.createBitmap(fixed.intrinsicWidth,fixed.intrinsicHeight,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fixedDrawable)
        fixed.setBounds(0,0,canvas.width,canvas.height)
        fixed.draw(canvas)

//        fixedDrawable = BitmapFactory.decodeResource(cnt.resources,R.drawable.ic_machine)
        fixedW = fixedDrawable.width
        fixedH = fixedDrawable.height

        val down = cnt.resources.getDrawable(R.drawable.ic_down_card,null)
        downMarkDrawable = Bitmap.createBitmap(down.intrinsicWidth,down.intrinsicHeight,Bitmap.Config.ARGB_8888)
        val canvas1 = Canvas(downMarkDrawable)
        down.setBounds(0,0,canvas1.width,canvas1.height)
        down.draw(canvas1)
//        downMarkDrawable = BitmapFactory.decodeResource(cnt.resources,R.drawable.ic_down_mark)
        val downW = downMarkDrawable.width
        val downH = downMarkDrawable.height
        LogUtil.d(TAG,"fixedW:$fixedW fixedH:$fixedH downW:$downW downH:$downH")
        mWidth = fixedW * 2f
        mHeight = fixedH * 2f
        setBounds(0,0, mWidth.toInt(), mHeight.toInt())
    }

    private fun initPaint() {
        mTextPaint = Paint()
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.textSize = 30f
        mTextPaint.isAntiAlias = true
        mTextPaint.style = Paint.Style.STROKE


        valueAnimator = ValueAnimator.ofInt(0,
            (mHeight - fixedH/2  - downMarkDrawable.height).toInt()
        )
        valueAnimator?.repeatCount = ValueAnimator.INFINITE
        valueAnimator?.repeatMode = ValueAnimator.RESTART
        valueAnimator?.duration = 3000
        valueAnimator?.interpolator = BounceInterpolator()
        valueAnimator?.addUpdateListener {
            mCurrentValue = it.animatedValue as Int
            invalidateSelf()
//            LogUtil.d(TAG,"value: $mCurrentValue")
        }
        valueAnimator?.start()

    }


    override fun setAlpha(alpha: Int) {
        mAlpha = alpha

    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("Not yet implemented")
    }

    override fun getOpacity(): Int {
        return when(mAlpha){
            0 -> PixelFormat.TRANSPARENT
            0xff -> PixelFormat.OPAQUE
            else -> PixelFormat.TRANSLUCENT
        }
    }

//    override fun getIntrinsicHeight(): Int {
//        return mHeight
//    }
//
//    override fun getIntrinsicWidth(): Int {
//        return mWidth
//    }

    override fun draw(canvas: Canvas) {
        canvas.scale(mScalex,mScaley)
        val mn = mCurrentValue % 100
        mTextPaint.setARGB(0xff-mn,0xff-mn,0x80+mn,mn)
        canvas.drawText("请在感应区刷卡",mWidth / 2f,100f,mTextPaint)
        canvas.drawText("请在感应区刷卡",mWidth / 2f,100f,mTextPaint)
        val nidu = mCurrentValue / (0f + mHeight - fixedH/2  - downMarkDrawable.height)
        val left =  mWidth  - downMarkDrawable.width - nidu * ( mWidth / 2  - downMarkDrawable.width)
//        canvas.drawBitmap(downMarkDrawable,mWidth / 2 - downMarkDrawable.width / 2 , mCurrentValue + 0f ,null)
        canvas.drawBitmap(downMarkDrawable, left, mCurrentValue + 0f ,null)
        canvas.drawBitmap(fixedDrawable,mWidth / 2 - fixedDrawable.width / 2 , mHeight - fixedDrawable.height + 0f ,null)

    }


    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)

        mScalex = (bottom - top ) / mWidth
        mScaley = (right - left) / mHeight

//        LogUtil.d(TAG,"left: $left top: $top right: $right bottom: $bottom")
    }

    fun release(){
        valueAnimator?.cancel()
        if (fixedDrawable.isRecycled.not()) {
            fixedDrawable.recycle()
        }
        if (downMarkDrawable.isRecycled.not())
        {
            downMarkDrawable.recycle()
        }
    }


    





}