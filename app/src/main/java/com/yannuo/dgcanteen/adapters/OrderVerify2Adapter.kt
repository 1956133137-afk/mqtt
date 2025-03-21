package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemOrderVerify2Binding
import com.yannuo.dgcanteen.model.VerifyDish

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/20 17:16
 **/
class OrderVerify2Adapter : BaseAdapter<VerifyDish, ItemOrderVerify2Binding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderVerify2Binding {
        return ItemOrderVerify2Binding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.verifyDishName.text = bean.dishesName
        holder.binding.verifyDishCount.text = "${bean.dishesNum}${bean.unit}"
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

}