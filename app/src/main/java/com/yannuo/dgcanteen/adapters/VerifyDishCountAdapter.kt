package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemVerifyCountBinding
import com.yannuo.dgcanteen.model.Dishes

class VerifyDishCountAdapter: BaseAdapter<Dishes, ItemVerifyCountBinding>() {
    override fun bindHolder(holder: Holder?, position: Int) {
        val data = mData[position]
        holder?.binding?.tvDishName?.text = "${data.dishesName}订餐数:"
        holder?.binding?.tvDishOrder?.text = data.totalDishesNum.toString()
        holder?.binding?.tvDishCount?.text = "${data.dishesName}核销数:"
        holder?.binding?.tvDishVerify?.text = data.verifyDishesNum.toString()
    }

    override fun bindHolder(holder: Holder?, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun getB(inflater: LayoutInflater?, parent: ViewGroup?): ItemVerifyCountBinding {
        return ItemVerifyCountBinding.inflate(inflater!!, parent, false)
    }
}