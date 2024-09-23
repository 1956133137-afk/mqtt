package com.yannuo.dgcanteen.views

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.interfaces.CloseEvent

class HintDialog(context: Context) : BaseDialog(context, R.layout.dialog_waitfor_pay) {
    private val TAG = javaClass.simpleName
    private lateinit var tv_close: ImageButton
    private lateinit var tv_count: TextView
    private lateinit var tv_content: TextView
    private var listener: CloseEvent? = null

    override fun initViewAndEvent() {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        tv_close = findViewById(R.id.ib_close)
        tv_count = findViewById(R.id.tv_count_down)
        tv_content = findViewById(R.id.textView)
        tv_close.setOnClickListener(this)
        tv_content.text = "正在等待用户支付..."
    }

    fun setListener(lis: CloseEvent): HintDialog {
        listener = lis
        return this
    }

    fun setTipsText(str: String) {
        tv_content.text = str
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ib_close -> {
                dismiss()
                listener?.onEvent(0, "取消交易")
            }
        }
    }
}