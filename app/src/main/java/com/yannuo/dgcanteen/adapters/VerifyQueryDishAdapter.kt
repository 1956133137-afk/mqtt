package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemVerifyQueryDishBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/12/17 9:40
 **/
class VerifyQueryDishAdapter : BaseAdapter<String, ItemVerifyQueryDishBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemVerifyQueryDishBinding {
        return ItemVerifyQueryDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        holder.binding.verifyDish.text = getData(position)
    }
}