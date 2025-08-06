package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import com.yannuo.dgcanteen.databinding.DialogConfirmBinding
import java.util.concurrent.TimeUnit

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/4 17:27
 **/
class ConfirmDialog(context: Context) : BaseDialog<DialogConfirmBinding>(context) {
    private lateinit var mListener: OnConfirmCallback
    private var countDown: CountDownTimer? = null

    fun setListener(listener: OnConfirmCallback) {
        mListener = listener
    }

    override fun initDialogView() {
        binding = DialogConfirmBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
//        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)

        binding.cancel.setOnClickListener { //取消
            mListener.confirmCallback(false)
            dismiss()
        }

        binding.confirm.setOnClickListener { //确定
            mListener.confirmCallback(true)
            dismiss()
        }
    }

    fun setTextMsg(str: String) {
        binding.tvText.text = str
    }

    fun setBackTime(msg: String, time: Long) {
        binding.llBtn.visibility = View.GONE
        binding.tvBackTime.visibility = View.VISIBLE
        binding.tvText.text = msg

        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
            override fun onTick(mil: Long) {
                binding.tvBackTime.text = "自动关闭 ${TimeUnit.MILLISECONDS.toSeconds(mil)}s"
            }

            override fun onFinish() {
                dismiss()
            }
        }
        countDown?.start()
    }

    override fun show() {
        super.show()
        binding.llBtn.visibility = View.VISIBLE
        binding.tvBackTime.visibility = View.GONE
    }

    override fun dismiss() {
        super.dismiss()
        countDown?.cancel()
    }

    override fun cancel() {
        super.cancel()
        countDown?.cancel()
    }

    interface OnConfirmCallback {
        fun confirmCallback(flag: Boolean)
    }
}