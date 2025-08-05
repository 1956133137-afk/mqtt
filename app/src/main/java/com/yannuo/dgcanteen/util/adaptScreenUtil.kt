package com.yannuo.dgcanteen.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

/**
 * 动态适配屏幕分辨率工具类
 */
class adaptScreenUtil {

    companion object {
        val instance: adaptScreenUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(adaptScreenUtil::class.java) { adaptScreenUtil() }
        }
    }

    /**
     * 适配分辨率
     */
    fun adaptScreen(activity: Context) {
        val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.width()
        } else {
            val p = Point()
            wm.defaultDisplay.getSize(p)
            p.x
        }
        val displayMetrics = activity.resources.displayMetrics
        val targetDensity = displayMetrics.widthPixels / width.toFloat()
        val targetDensityDpi = (160 * targetDensity).toInt()
        val fontScale = displayMetrics.scaledDensity / displayMetrics.density // 保留用户字体缩放

        displayMetrics.density = targetDensity
        displayMetrics.scaledDensity = targetDensity * fontScale // 应用字体缩放
        displayMetrics.densityDpi = targetDensityDpi
    }

    fun adaptScreen(activity: Context,width: Int) {
        val displayMetrics = activity.resources.displayMetrics
        val targetDensity = displayMetrics.widthPixels / width.toFloat()
        val targetDensityDpi = (160 * targetDensity).toInt()
        val fontScale = displayMetrics.scaledDensity / displayMetrics.density // 保留用户字体缩放

        displayMetrics.density = targetDensity
        displayMetrics.scaledDensity = targetDensity * fontScale // 应用字体缩放
        displayMetrics.densityDpi = targetDensityDpi
    }
}