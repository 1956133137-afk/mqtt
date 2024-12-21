package com.yannuo.dgcanteen.dialogView

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.ShowDishAdapter
import com.yannuo.dgcanteen.databinding.DialogDishesBinding
import com.yannuo.dgcanteen.databinding.DialogMealRuleDetailBinding
import com.yannuo.dgcanteen.model.Dish
import com.yannuo.dgcanteen.model.SwForUI

class ShowRuleDetailDialog(context: Context) : BaseDialog<DialogMealRuleDetailBinding>(context) {

    override fun initDialogView() {
        binding = DialogMealRuleDetailBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.btnClose.setOnClickListener { cancel() }
    }

    @SuppressLint("SetTextI18n")
    fun setRuleDetail(swForUI: SwForUI) {
        binding.ruleName.text = swForUI.useMealRuleName
        binding.ruleUseCount.text = "${swForUI.everyUseTime} 次"
        binding.ruleDayRestTime.text = if (swForUI.everyUseTime == "0") {
            "无数 次"
        } else "${swForUI.restTime} 次"
        binding.rulePrice.text = "${swForUI.rulePrice} 元"
        binding.ruleAllowance.text = "${swForUI.subsidy} 元"
    }

}