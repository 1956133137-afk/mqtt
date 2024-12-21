package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.view.View
import com.yannuo.dgcanteen.databinding.DialogMealAmountDetailBinding
import com.yannuo.dgcanteen.model.AmountDetail
import com.yannuo.dgcanteen.model.SwForUI

/**
 * 餐别金额详细
 */
class ShowAmountDetailDialog(context: Context) : BaseDialog<DialogMealAmountDetailBinding>(context) {

    override fun initDialogView() {
        binding = DialogMealAmountDetailBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.btnClose.setOnClickListener { cancel() }
    }

    fun setAmountDetail(map: HashMap<String, AmountDetail>) {
        if (map["meal01"] != null) {
            binding.meal01Info.visibility = View.VISIBLE
            binding.meal01Name.text = "${map["meal01"]!!.mealName}:"
            binding.meal01Amount.text = "${map["meal01"]!!.payment} 元 / ${map["meal01"]!!.actualPayment} 元"
        } else binding.meal01Info.visibility = View.INVISIBLE

        if (map["meal02"] != null) {
            binding.meal02Info.visibility = View.VISIBLE
            binding.meal02Name.text = "${map["meal02"]!!.mealName}:"
            binding.meal02Amount.text = "${map["meal02"]!!.payment} 元 / ${map["meal02"]!!.actualPayment} 元"
        } else binding.meal02Info.visibility = View.INVISIBLE

        if (map["meal03"] != null) {
            binding.meal03Info.visibility = View.VISIBLE
            binding.meal03Name.text = "${map["meal03"]!!.mealName}:"
            binding.meal03Amount.text = "${map["meal03"]!!.payment} 元 / ${map["meal03"]!!.actualPayment} 元"
        } else binding.meal03Info.visibility = View.INVISIBLE

        if (map["meal04"] != null) {
            binding.meal04Info.visibility = View.VISIBLE
            binding.meal04Name.text = "${map["meal04"]!!.mealName}:"
            binding.meal04Amount.text = "${map["meal04"]!!.payment} 元 / ${map["meal04"]!!.actualPayment} 元"
        } else binding.meal04Info.visibility = View.INVISIBLE
    }

}