package com.yannuo.dgcanteen.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.PopupWindow
import com.yannuo.dgcanteen.R

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/12/21 10:33
 * @Version 1.0
 */
class TipPopupWindow(context: Context): PopupWindow(
    LayoutInflater.from(context).inflate(R.layout.popwindow_time_tip, null),
    LayoutParams.WRAP_CONTENT,
    LayoutParams.WRAP_CONTENT,
    true
) {

    init {
        /**
         * onCreate 在这里写
         */

        // 设置点击外部关闭
        isOutsideTouchable = true
        isTouchable = true
    }

    fun showAtRight(anchorView: View) {
        if (!isShowing) {
            val offsetX = anchorView.width
            val offsetY = anchorView.height
            showAsDropDown(anchorView, offsetX, - offsetY)
        }
    }

    fun dismissAndRelease() {
        if (isShowing) {
            release()
            dismiss()
        }
    }

    private fun release() {

    }
}