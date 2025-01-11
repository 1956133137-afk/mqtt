package com.yannuo.dgcanteen.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*


/**
 * Author: filowl
 * Description: ***
 * Date: 2025/1/9 18:30
 **/
class PickerView : View {
    private val TAG = javaClass.simpleName
    private var MARGIN_ALPHA = 2.8F //text之间间距和minTextSize之比
    private var SPEED = 2   //自动回滚到中间的速度
    private val mDataList: MutableList<String> = mutableListOf()
    private var mCurrentSelected = 0
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mMaxTextSize = 80F
    private var mMinTextSize = 40F
    private var mMaxTextAlpha = 255F
    private var mMinTextAlpha = 120F
    private var mColorText = 0x333333

    private var mViewWidth = 0
    private var mViewHeight = 0

    private var mLastDownY = 0F
    private var mMoveLen = 0F   //滑动距离
    private var isInit = false
    private var mSelectListener: OnSelectListener? = null
    private var mTimer: Timer = Timer()
    private var myTimerTask: MyTimerTask? = null

    @SuppressLint("HandlerLeak")
    private var updateHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (Math.abs(mMoveLen) < SPEED) {
                mMoveLen = 0F
                if (myTimerTask != null) {
                    myTimerTask?.cancel()
                    myTimerTask == null
                    mSelectListener?.onSelect(mDataList[mCurrentSelected])
                }
            } else mMoveLen -= mMoveLen / Math.abs(mMoveLen) * SPEED
            invalidate()
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init()
    }

    private fun init() {
        mPaint.style = Paint.Style.FILL
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.color = mColorText
    }

    fun setOnSelectListener(listener: OnSelectListener) {
        mSelectListener = listener
    }

    fun setData(dataList: MutableList<String>, data: String) {
        mDataList.clear()
        mDataList.addAll(dataList)
        // 设置默认显示
        mCurrentSelected = 0
        mDataList.forEachIndexed { index, s ->
            if (data == s) {
                mCurrentSelected = index
                mSelectListener?.onSelect(mDataList[mCurrentSelected])
            }
        }
        invalidate()
    }

    fun setSelected(selected: Int) {
        mCurrentSelected = selected
    }

    //往下滑超过离开距离
    fun moveHeadToTail() {
        if (mDataList.size > 0) {
            val headPosition = mDataList[0]
            mDataList.removeAt(0)
            mDataList.add(headPosition)
        }
    }

    //往上滑超过离开距离
    fun moveTailToHead() {
        if (mDataList.size > 0) {
            val tailPosition = mDataList[mDataList.size - 1]
            mDataList.removeAt(mDataList.size - 1)
            mDataList.add(0, tailPosition)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mViewWidth = measuredWidth
        mViewHeight = measuredHeight
        // 按照View的高度计算字体大小
        mMaxTextSize = mViewHeight / 2.0f
        mMinTextSize = mMaxTextSize / 2f
        isInit = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isInit) drawData(canvas)
    }

    private fun drawData(canvas: Canvas) {
        // 先绘制选中的text再往上往下绘制其余的text
        val scale: Float = parabola(mViewHeight / 4.0f, mMoveLen)
        val size = (mMaxTextSize - mMinTextSize) * scale + mMinTextSize
        mPaint.textSize = size
        mPaint.alpha = ((mMaxTextAlpha - mMinTextAlpha) * scale + mMinTextAlpha).toInt()
        // text居中绘制，注意baseline的计算才能达到居中，y值是text中心坐标
        val x = (mViewWidth / 2.0).toFloat()
        val y = (mViewHeight / 2.0 + mMoveLen).toFloat()
        val fmi = mPaint.fontMetricsInt
        val baseline = (y - (fmi.bottom / 2.0 + fmi.top / 2.0)).toFloat()

        canvas.drawText(mDataList[mCurrentSelected], x, baseline, mPaint)
        // 绘制上方data
        var i = 1
        while (mCurrentSelected - i >= 0) {
            drawOtherText(canvas, i, -1)
            i++
        }
        // 绘制下方data
        i = 1
        while (mCurrentSelected + i < mDataList.size) {
            drawOtherText(canvas, i, 1)
            i++
        }
    }

    /**
     * @param canvas
     * @param position 距离mCurrentSelected的差值
     * @param type 1表示向下绘制，-1表示向上绘制
     */
    private fun drawOtherText(canvas: Canvas, position: Int, type: Int) {
        val d = (MARGIN_ALPHA * mMinTextSize * position + type * mMoveLen)
        val scale = parabola(mViewHeight / 4.0f, d)
        val size = (mMaxTextSize - mMinTextSize) * scale + mMinTextSize
        mPaint.textSize = size
        mPaint.alpha = ((mMaxTextAlpha - mMinTextAlpha) * scale + mMinTextAlpha).toInt()
        val y = (mViewHeight / 2.0 + type * d).toFloat()
        val fmi = mPaint.fontMetricsInt
        val baseline = (y - (fmi.bottom / 2.0 + fmi.top / 2.0)).toFloat()
        canvas.drawText(mDataList[mCurrentSelected + type * position], (mViewWidth / 2.0).toFloat(), baseline, mPaint)
    }

    /**
     * 抛物线
     * @param zero 零点坐标
     * @param x 偏移量
     * @return scale
     */
    private fun parabola(zero: Float, x: Float): Float {
        val f = (1 - Math.pow((x / zero).toDouble(), 2.0)).toFloat()
        return if (f < 0) 0F else f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> doDown(event)
            MotionEvent.ACTION_MOVE -> doMove(event)
            MotionEvent.ACTION_UP -> doUp(event)
        }
        return true
    }

    private fun doDown(event: MotionEvent) {
        if (myTimerTask != null) {
            myTimerTask?.cancel()
            myTimerTask = null
        }
        mLastDownY = event.y
    }

    private fun doMove(event: MotionEvent) {
        mMoveLen += event.y - mLastDownY
        if (mMoveLen > MARGIN_ALPHA * mMinTextSize / 2) {
            moveTailToHead()
            mMoveLen -= MARGIN_ALPHA * mMinTextSize
        } else if (mMoveLen < -(MARGIN_ALPHA * mMinTextSize / 2)) {
            moveHeadToTail()
            mMoveLen += MARGIN_ALPHA * mMinTextSize
        }
        mLastDownY = event.y
        invalidate()
    }

    private fun doUp(event: MotionEvent) {
        if (Math.abs(mMoveLen) < 0.0001) {
            mMoveLen = 0F
            return
        }
        if (myTimerTask != null) {
            myTimerTask?.cancel()
            myTimerTask = null
        }
        myTimerTask = MyTimerTask(updateHandler)
        mTimer.schedule(myTimerTask, 0, 10)
    }

    class MyTimerTask(handler: Handler) : TimerTask() {
        private val mHandler: Handler = handler

        override fun run() {
            mHandler.sendMessage(mHandler.obtainMessage())
        }
    }

    interface OnSelectListener {
        fun onSelect(text: String)
    }
}