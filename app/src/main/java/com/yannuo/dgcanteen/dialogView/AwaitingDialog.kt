package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.view.WindowManager
import com.yannuo.dgcanteen.databinding.DialogAwaitBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/6/8
 * Time: 16:35
 **/
class AwaitingDialog(context: Context) : BaseDialog<DialogAwaitBinding>(context) {
    override fun initDialogView() {
        binding = DialogAwaitBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
//        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        binding.tvState.setOnLongClickListener {
            dismiss()
            return@setOnLongClickListener true
        }
    }

    fun updateText(str: String) {
//        var strText = "•••"
//        var lin = ""
//        mScope.launch {
//            var value = 0
//            while (isActive) {
//                delay(200)
//                value++
//                lin = binding.tvState.text.toString().replace("•","")
//                 if (lin.isEmpty())lin = str
//                binding.tvState.text = lin +strText.substring(0, value % 4)
//            }
//        }
        binding.tvState.text = "${str}•••"
    }

    fun setText(str: String) {
        binding.tvState.text = str
    }
}