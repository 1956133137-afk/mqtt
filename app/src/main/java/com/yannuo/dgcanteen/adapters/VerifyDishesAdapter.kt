package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.dao.VerifyDishes
import com.yannuo.dgcanteen.databinding.ItemVerifyDishesBinding

class VerifyDishesAdapter: BaseAdapter<VerifyDishes, ItemVerifyDishesBinding>() {
    override fun bindHolder(holder: Holder?, position: Int) {
        val data = mData[position]
        holder?.binding?.verifyTime?.text = data.time
        holder?.binding?.verifyDishes?.text = data.dish.replace("|", "\n")
        holder?.binding?.verifyUnDish?.text = data.unDish.replace("|", "\n")
        holder?.binding?.verifyWindow?.text = data.window.replace("|", "\n")
    }

    override fun bindHolder(holder: Holder?, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun getB(inflater: LayoutInflater?, parent: ViewGroup?): ItemVerifyDishesBinding {
        return ItemVerifyDishesBinding.inflate(inflater!!, parent, false)
    }
    
}