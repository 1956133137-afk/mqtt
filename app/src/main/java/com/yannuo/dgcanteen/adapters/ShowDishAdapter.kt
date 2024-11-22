package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemShowDishBinding
import com.yannuo.dgcanteen.model.Dish
import com.yannuo.dgcanteen.util.LogUtil

/**
 * 消费记录中菜品信息
 */
class ShowDishAdapter : BaseAdapter<Dish, ItemShowDishBinding>() {
    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemShowDishBinding {
        return ItemShowDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()) {
            LogUtil.d(TAG, "update payload:  ${payloads.get(0)}")
        } else {
            bindHolder(holder, position)
        }
    }

    override fun bindHolder(holder: Holder, position: Int) {
        getData(position).apply {
            holder.binding.dishName.text = dishesName
            holder.binding.dishCount.text = dishesNumber
            holder.binding.dishPrice.text = String.format("%.02f", dishesPrice.toDouble())
        }
    }
}