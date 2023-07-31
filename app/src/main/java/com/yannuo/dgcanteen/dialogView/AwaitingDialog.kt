package com.yannuo.dgcanteen.dialogView

import android.content.Context
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
        binding.tvState.setOnLongClickListener {
            dismiss()
            return@setOnLongClickListener true
        }
    }

    fun updateText(str: String) {
        val strText = "$str•••"
        mScope.launch {
            var value = 0
            while (isActive) {
                delay(200)
                value++
                binding.tvState.text = strText.substring(0, strText.length - 3 + value % 4)
            }
        }
    }
}