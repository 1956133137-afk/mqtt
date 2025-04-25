package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.view.WindowManager
import com.yannuo.dgcanteen.databinding.DialogTextBinding

class ShowTextDailog(context: Context): BaseDialog<DialogTextBinding>(context) {
    override fun initDialogView() {
        binding = DialogTextBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        binding.showText.setOnLongClickListener {
            dismiss()
            return@setOnLongClickListener true
        }
    }

    fun showText(text: String){
        binding.text.text = text
    }

}