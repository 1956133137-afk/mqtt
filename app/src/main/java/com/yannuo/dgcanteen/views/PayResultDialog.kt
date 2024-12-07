package com.yannuo.dgcanteen.views

import android.content.Context
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import java.util.concurrent.TimeUnit

class PayResultDialog(context :Context) :BaseDialog(context, R.layout.dialog_pay_result) {
    private val TAG = javaClass.simpleName
    private var time = 3L
    private val kv by lazy { MMKV.defaultMMKV() }
    private lateinit var tv_close : ImageButton
    private lateinit var tv_count  :TextView
    private lateinit var tvPayResult: TextView
    private var countDown : CountDownTimer ?= null

    override fun initViewAndEvent() {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        time = kv.decodeLong(Constant.PAY_RESULT_DIALOG_TIME, 3L)
        tv_close = findViewById(R.id.ib_close)
        tv_count = findViewById(R.id.tv_count_down)
        tvPayResult = findViewById(R.id.pay_result)

        tv_close.setOnClickListener(this)

    }

    fun show(isSuccess: Boolean, text: String) {
        super.show()
        tvPayResult.text = text
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time)+200,1000) {
            override fun onTick(mil: Long) {
                tv_count.text = "返回倒计时 ${TimeUnit.MILLISECONDS.toSeconds(mil)} s"
//                LogUtil.i(TAG,"${tv_count.text}")
            }

            override fun onFinish() {
                dismiss()
            }
        }
        countDown?.start()
    }

    override fun dismiss() {
        countDown?.cancel()
        super.dismiss()
    }

    override fun cancel() {
        countDown?.cancel()
        super.cancel()
    }


    override fun onClick(v: View) {
        when(v.id){
            R.id.ib_close ->{
                dismiss()
            }
        }

    }


}