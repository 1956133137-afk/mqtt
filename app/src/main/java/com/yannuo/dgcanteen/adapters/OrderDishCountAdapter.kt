package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemVerifyCountBinding
import com.yannuo.dgcanteen.model.Dishes
import com.yannuo.dgcanteen.model.DishesCounts

class OrderDishCountAdapter: BaseAdapter<DishesCounts, ItemVerifyCountBinding>() {
    override fun bindHolder(holder: Holder?, position: Int) {
        val data = mData[position]
        when (data.flag) {
            0 -> {
                holder?.binding?.tvDishCount?.text = "${data.dishesName}订餐数:"
                holder?.binding?.tvDishVerify?.text = data.dishesNum.toString()
            }
            1 -> {
                holder?.binding?.tvDishCount?.text = "${data.dishesName}核销数:"
                holder?.binding?.tvDishVerify?.text = data.dishesNum.toString()
            }
            2 -> {
                holder?.binding?.tvDishCount?.text = "${data.dishesName}未核销数:"
                holder?.binding?.tvDishVerify?.text = data.dishesNum.toString()
            }
        }

    }

    override fun bindHolder(holder: Holder?, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun getB(inflater: LayoutInflater?, parent: ViewGroup?): ItemVerifyCountBinding {
        return ItemVerifyCountBinding.inflate(inflater!!, parent, false)
    }
}