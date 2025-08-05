package com.yannuo.dgcanteen.activitys

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.util.DisplayUtils
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.adaptScreenUtil

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/8/19 15:42
 * @Version 1.0
 */
open class BaseDisplay(context: Context, display: Display) : Presentation(context, display) {

    private var isCancel: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY - 1)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        else window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT or WindowManager.LayoutParams.TYPE_PHONE)
        adaptScreenUtil.instance.adaptScreen(this.context,1920)
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