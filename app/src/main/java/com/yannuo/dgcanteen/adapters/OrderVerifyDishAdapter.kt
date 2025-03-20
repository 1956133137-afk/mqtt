package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemOrderVerifyDishBinding
import com.yannuo.dgcanteen.model.VerifyDish

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/20 18:10
 **/
class OrderVerifyDishAdapter : BaseAdapter<VerifyDish, ItemOrderVerifyDishBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderVerifyDishBinding {
        return ItemOrderVerifyDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.verifyDishName.text = bean.dishesName
        holder.binding.verifyDishPrice.text = bean.price
        holder.binding.verifyDishCount.text = "${bean.dishesNum}/${bean.unit}"
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }
}