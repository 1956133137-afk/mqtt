package com.yannuo.dgcanteen.activitys

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import com.yannuo.dgcanteen.util.DisplayUtils
import com.yannuo.dgcanteen.util.LogUtil

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/8/19 15:42
 * @Version 1.0
 */
open class BaseDisplay(private val context: Context, display: Display): Presentation(context, display) {

    private var isCancel: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (context is Activity) {
            DisplayUtils.setCustomDensity(this, context, context.application)
        } else {
            LogUtil.i(this::class.simpleName, "Display's context is not activity!!!")
        }
        super.onCreate(savedInstanceState)
    }

    override fun cancel() {
        if (this.isCancel) {
            super.cancel()
        }
    }

    /**
     * 防止：Presentation is being dismissed because the display metrics have changed since it was created.
     *
     * @param isCancel 是否允许取消
     */
    fun setCancelLock(isCancel: Boolean) {
        this.isCancel = isCancel
    }

    fun safeCancel() {
        setCancelLock(true)
        cancel()
        setCancelLock(false)
    }
}