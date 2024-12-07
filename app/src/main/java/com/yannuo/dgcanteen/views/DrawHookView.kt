package com.yannuo.dgcanteen.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.util.LogUtil

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/26 17:17
 * @Version 1.0
 */
class DrawHookView : View {

    //绘制圆弧的进度值
    private var progress = 0
    //线1的x轴
    private var line1_x = 0f
    //线1的y轴
    private var line1_y = 0f
    //线2的x轴
    private var line2_x = 0f
    //线2的y轴
    private var line2_y = 0f
    //画圆速率
    private var rate = 3

    private val path = Path()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        progress += 3

        /**
         * 开始绘制圆弧
         **/
        val paint = Paint()
        //设置画笔颜色
        paint.setColor(resources.getColor(R.color.arc_blue))
        //设置圆弧的宽度
        paint.strokeWidth = 4f
        //设置圆弧为空心
        paint.style = Paint.Style.STROKE
        //消除锯齿
        paint.isAntiAlias = true

        //获取圆心的x坐标
        val center = width / 2f
        val center1 = center - width / paint.strokeWidth
        //圆弧半径
        val radius = width / 2f - paint.strokeWidth

        //定义的圆弧的形状和大小的界限
        val rectF = RectF(center - radius - 1f, center - radius - 1f, center + radius + 1f, center + radius + 1f)

        //根据进度画圆弧
        canvas?.drawArc(rectF, 235f, -360f * progress / 100f, false, paint)

        /**
         * 开始绘制对勾
         */
        //先等圆弧画完，才画对勾
        if (progress >= 100) {
            path.reset()

            if (line1_x < radius / 3) {
                line1_x++
                line1_y++
            }
            //画第一根线
            path.moveTo(center1, center)
            path.lineTo(center1 + line1_x, center + line1_y)
            canvas?.drawPath(path, paint)


            if (line1_x >= radius / 3 && line2_x == 0f && line2_y == 0f) {
                line2_x = line1_x
                line2_y = line1_y
                line1_x++
                line1_y++
            }
            if (line1_x >= radius / 3 && (line2_x != 0f || line2_y != 0f)) {
                //画第二根线
                if (line1_x >= radius / 3 && line2_x <= radius) {
                    line2_x++
                    line2_y--
                }
                path.moveTo(center1 + line1_x - 1, center + line1_y + 2)
                path.lineTo(center1 + line2_x, center + line2_y)
                canvas?.drawPath(path, paint)
            }
        }

        //每隔1毫秒界面刷新
        if (line1_x < radius / 3 || line2_x <= radius)
            postInvalidateDelayed(5)
    }

    fun setRate(rate: Int) {
        this.rate = rate
    }

    fun clearData() {
        progress = 0
        line1_x = 0f
        line1_y = 0f
        line2_x = 0f
        line2_y = 0f
    }
}