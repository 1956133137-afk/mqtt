package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ItemPayOrderBinding
import com.yannuo.dgcanteen.dialogView.ShowDishesDialog
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.model.Dish
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/23 18:31
 **/
class PayOrderAdapter(context: Context) : BaseAdapter<PayOrderTable, ItemPayOrderBinding>() {
    private val showDishesDialog by lazy { ShowDishesDialog(context) }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemPayOrderBinding {
        return ItemPayOrderBinding.inflate(inflater, parent, false)
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
            holder.binding.payState.text = when (offline) {
                "0" -> "在线"
                "1" -> "离线"
                else -> "未知"
            }
            holder.binding.uploadState.text = when (flag) {
                1 -> "已上传"
                else -> "未上传"
            }
            holder.binding.orderId.text = orderId
        }
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.checkDish.setOnClickListener {
            val position = holder.adapterPosition
            val dishList: MutableList<Dish> = mutableListOf()
            mData?.get(position)?.paymentDishesList?.forEach {
                val dish = Dish().apply {
                    dishesId = it.dishesId
                    dishesName = it.dishesName
                    dishesNumber = it.dishesNumber
                    dishesPrice = it.dishesPrice
                }
                dishList.add(dish)
            }
            showDishesDialog.showWithDishDetails(dishList)
            showDishesDialog.show()
        }
    }
}