package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.WindowManager
import com.yannuo.dgcanteen.databinding.DialogPasswordBinding
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.util.ToastShowUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/4 14:00
 **/
class PasswordDialog(context: Context) : BaseDialog<DialogPasswordBinding>(context) {
    private lateinit var listener: CloseEvent

    fun setListener(listener: CloseEvent) {
        this.listener = listener
    }

    override fun initDialogView() {
        binding = DialogPasswordBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.inputPassword.addTextChangedListener(HideTextWatcher(4))
        binding.btnCancel.setOnClickListener {
            binding.inputPassword.text = null
            dismiss()
        }
    }

    private inner class HideTextWatcher(private val mMaxLength: Int) : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(editable: Editable) {
            if (editable.length == mMaxLength) { //在布局中也要设置长度
                if (editable.toString() == getCurrentTime()) {
                    binding.inputPassword.text = null
                    if (this@PasswordDialog::listener.isInitialized)
                        listener.onEvent(1, null)
                    dismiss()
                } else {
                    binding.inputPassword.text = null
                    ToastShowUtil.show("密码错误，请重新输入")
                }
            }
        }
    }

    private fun getCurrentTime(): String {
        return DateFormat.format("MMdd", System.currentTimeMillis()).toString()
    }
}