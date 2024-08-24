package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ItemOfflineOrderBinding
import com.yannuo.dgcanteen.dialogView.ShowDishesDialog
import com.yannuo.dgcanteen.greendao.entity.OfflineOrderTable
import com.yannuo.dgcanteen.model.Dish
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/23 18:31
 **/
class OfflineOrderAdapter(context: Context) : BaseAdapter<OfflineOrderTable, ItemOfflineOrderBinding>() {
    private val showDishesDialog by lazy { ShowDishesDialog(context) }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOfflineOrderBinding {
        return ItemOfflineOrderBinding.inflate(inflater, parent, false)
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
            holder.binding.payType.text = when (payType) {
                "1" -> "刷脸"
                "2" -> "扫码"
                "3" -> "刷卡"
                else -> "未知"
            }
            holder.binding.payment.text = payment
            holder.binding.payTime.text = TimeUtil.dateFormat(signTime)
            holder.binding.sessionId.text = sessionId
        }
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.checkDish.setOnClickListener {
            val position = holder.adapterPosition
            val dishList: MutableList<Dish> = mutableListOf()
            mData?.get(position)?.paymentDishes?.forEach {
                val dish = Gson().fromJson(Gson().toJson(it), Dish::class.java)
                dishList.add(dish)
            }
            showDishesDialog.showWithDishDetails(dishList)
            showDishesDialog.show()
        }
    }
}