package com.yannuo.dgcanteen.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemSelectMealBinding
import com.yannuo.dgcanteen.model.OrderMeal

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/24 16:16
 **/
class SelectMealAdapter : BaseAdapter<OrderMeal, ItemSelectMealBinding>() {
    var selectPos = 0
    private var listener: SelectMealListener? = null

    fun setMealListener(listener: SelectMealListener) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemSelectMealBinding {
        return ItemSelectMealBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.tvMealName.text = bean.mealName
        holder.binding.tvMealName.setBackgroundResource(if (position == selectPos) R.drawable.shape_bg_blue_1 else R.drawable.shape_bg_white_1)
        holder.binding.tvMealName.setTextColor(Color.parseColor(if (position == selectPos) "#FFFFFF" else "#4F4F4F"))
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.tvMealName.setOnClickListener {
            val position = holder.adapterPosition
            selectPos = position
            listener?.onSelectMeal(getData(position))
            notifyDataSetChanged()
        }
    }

    interface SelectMealListener {
        fun onSelectMeal(bean: OrderMeal)
    }
}