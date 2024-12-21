package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemMealTimePayOrderBinding
import com.yannuo.dgcanteen.dialogView.ShowDishesDialog
import com.yannuo.dgcanteen.dialogView.ShowRuleDetailDialog
import com.yannuo.dgcanteen.greendao.entity.SwPayOrderTable
import com.yannuo.dgcanteen.model.SwForUI
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/23 18:31
 **/
class MealTimePayOrderAdapter(context: Context) :
    BaseAdapter<SwPayOrderTable, ItemMealTimePayOrderBinding>() {
    private val showRuleDetailDialog by lazy { ShowRuleDetailDialog(context) }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemMealTimePayOrderBinding {
        return ItemMealTimePayOrderBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()) {
            LogUtil.d(TAG, "update payload:  ${payloads.get(0)}")
        } else {
            bindHolder(holder, position)
        }
    }

    override fun bindHolder(holder: Holder, position: Int) {
        mData?.get(position)?.apply {
            holder.binding.userName.text = username
            holder.binding.payTime.text = payTime
            holder.binding.payment.text = payment
            holder.binding.actualPayment.text = actualPayment
            holder.binding.payType.text = when (payType) {
                "1" -> "刷脸"
                "2" -> "扫码"
                "3" -> "刷卡"
                else -> "未知"
            }
            holder.binding.mealName.text = actualMealName
            holder.binding.ruleName.text = standardName
            holder.binding.orderId.text = orderId
        }
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.checkRule.setOnClickListener {
            val position = holder.adapterPosition
            if (position >= 0) {
                val data = mData?.get(position)
                if (data != null) {
                    val swForUI = SwForUI().apply {
                        useMealRuleName = data.standardName
                        everyUseTime = data.standardNum
                        restTime = data.ruleRestTime
                        rulePrice = data.rulePrice
                        subsidy = data.subsidyMoney
                    }
                    showRuleDetailDialog.show()
                    showRuleDetailDialog.setRuleDetail(swForUI)
                }
            }
        }
    }
}