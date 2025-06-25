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
class DrawCrossView : View {

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
    private var circleRate = 3
    //画线的速率
    private var lineRate = 2

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        progress += circleRate

        /**
         * 开始绘制圆弧
         **/
        val paint = Paint()
        //设置画笔颜色
        paint.setColor(resources.getColor(R.color.arc_red))
        //设置圆弧的宽度
        paint.strokeWidth = 4f
        //设置圆弧为空心
        paint.style = Paint.Style.STROKE
        //消除锯齿
        paint.isAntiAlias = true

        //获取圆心的x坐标
        val center = width / 2f
        //空心区域半径
        val radius = width / 2f - paint.strokeWidth

        //定义的圆弧的形状和大小的界限
        val rectF = RectF(center - radius - 1f, center - radius - 1f, center + radius + 1f, center + radius + 1f)

        //根据进度画圆弧
        canvas?.drawArc(rectF, 235f, -360f * progress / 100f, false, paint)

        /**
         * 开始绘制打叉
         */
        //先等圆弧画完，才画对勾
        if (progress >= 100) {
            path.reset()

            if (line1_x < radius * 2 / 3) {
                line1_x += lineRate
                line1_y += lineRate
            }
            //画第一根线
            path.moveTo(center - radius / 3, center - radius / 3)
            path.lineTo(center - radius / 3 + line1_x - 1, center - radius / 3 + line1_y - 1)
            canvas?.drawPath(path, paint)

            if (line1_x >= radius * 2 / 3 && line2_x == 0f && line2_y == 0f) {
                line2_x++
                line2_y++
            }
            if (line1_x >= radius * 2 / 3 && (line2_x != 0f || line2_y != 0f)) {
                //画第二根线
                if (radius * 2 / 3 in line2_x..line1_x) {
                    line2_x += lineRate
                    line2_y += lineRate
                }
                path.moveTo(center + radius / 3, center - radius / 3)
                path.lineTo(center + radius / 3 - line2_x, center - radius / 3 + line2_y)
                canvas?.drawPath(path, paint)
            }
        }

        //每隔1毫秒界面刷新
        if (line1_x < radius / 3 || line2_x <= radius)
            postInvalidateDelayed(1)
    }

    fun setCircleRate(rate: Int) {
        this.circleRate = rate
    }

    fun setLineRate(rate: Int) {
        this.lineRate = rate
    }

    fun clearData() {
        progress = 0
        line1_x = 0f
        line1_y = 0f
        line2_x = 0f
        line2_y = 0f
    }
}