package com.yannuo.dgcanteen.views

import android.content.Context
import android.view.WindowManager
import android.widget.TextView
import com.yannuo.dgcanteen.R

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/21 18:36
 **/
class AwaitingDialog(context: Context) : BaseDialog(context, R.layout.dialog_await) {
    private lateinit var tvState: TextView

    override fun initViewAndEvent() {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        tvState = findViewById(R.id.tv_state)
        tvState.setOnLongClickListener {
            dismiss()
            return@setOnLongClickListener true
        }
    }

    fun updateText(str: String) {
        tvState.text = "${str}•••"
    }
}