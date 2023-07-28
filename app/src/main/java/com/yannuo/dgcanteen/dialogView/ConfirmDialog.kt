package com.yannuo.dgcanteen.dialogView

import android.content.Context
import com.yannuo.dgcanteen.databinding.DialogConfirmBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/4 17:27
 **/
class ConfirmDialog(context: Context) : BaseDialog<DialogConfirmBinding>(context) {
    private lateinit var mListener: OnConfirmCallback

    fun setListener(listener: OnConfirmCallback) {
        mListener = listener
    }

    override fun initDialogView() {
        binding = DialogConfirmBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.cancel.setOnClickListener { //取消
            mListener.confirmCallback(false)
            dismiss()
        }

        binding.confirm.setOnClickListener { //确定
            mListener.confirmCallback(true)
            dismiss()
        }
    }

    interface OnConfirmCallback {
        fun confirmCallback(flag: Boolean)
    }
}