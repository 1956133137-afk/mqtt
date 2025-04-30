package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.DialogTextBinding

class ShowTextDailog(context: Context): BaseDialog<DialogTextBinding>(context) {
    override fun initDialogView() {
        binding = DialogTextBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        binding.showText.setOnClickListener {
            dismiss()
        }
    }

    fun setBack(type: Int){
        binding.showText.background = when(type){
            1 -> { ResourcesCompat.getDrawable(context.resources,R.drawable.shape_bg_green_1,null) }
            2 -> { ResourcesCompat.getDrawable(context.resources,R.drawable.shape_bg_red_10,null) }
            else -> { ResourcesCompat.getDrawable(context.resources,R.drawable.click_button_blue,null) }
        }
    }

    fun showText(text: String){
        binding.text.text = text
    }

}