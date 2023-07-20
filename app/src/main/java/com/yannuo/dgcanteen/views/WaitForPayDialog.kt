package com.yannuo.dgcanteen.views

import android.content.Context
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.util.LogUtil
import java.util.concurrent.TimeUnit

class WaitForPayDialog(context :Context) :BaseDialog(context, R.layout.dialog_waitfor_pay) {
    private val TAG = javaClass.simpleName
    private val time = 60L
    private lateinit var tv_close : ImageButton
    private lateinit var tv_count  :TextView
    private var countDown : CountDownTimer ?= null
    private var listener : CloseEvent?= null


    override fun initViewAndEvent() {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        tv_close = findViewById(R.id.ib_close)
        tv_count = findViewById(R.id.tv_count_down)

        tv_close.setOnClickListener(this)

    }

    override fun show() {
        super.show()
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time)+200,1000) {
            override fun onTick(mil: Long) {
                tv_count.text = "请在有效时间内完成支付 ${TimeUnit.MILLISECONDS.toSeconds(mil)} s"
                LogUtil.i(TAG,"${tv_count.text}")
            }

            override fun onFinish() {
                dismiss()
                listener?.onEvent(1,"超时取消")
            }
        }
        countDown?.start()
    }

    override fun dismiss() {
        countDown?.cancel()
        super.dismiss()
    }

    fun conclude(){
        dismiss()
        listener?.onEvent(0,"取消交易")
    }

    override fun cancel() {
        countDown?.cancel()
        super.cancel()
    }

    fun setListener(lis : CloseEvent){
        listener = lis
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.ib_close ->{
                dismiss()
                listener?.onEvent(0,"取消交易")
            }
        }

    }


}