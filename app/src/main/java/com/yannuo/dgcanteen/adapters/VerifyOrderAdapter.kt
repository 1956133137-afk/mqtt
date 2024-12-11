package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemVerifyOrderBinding
import com.yannuo.dgcanteen.greendao.entity.VerifyDishes

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/12/11 18:01
 **/
class VerifyOrderAdapter : BaseAdapter<VerifyDishes, ItemVerifyOrderBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemVerifyOrderBinding {
        return ItemVerifyOrderBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = getData(position)
        holder.binding.userName.text = data.personName
        holder.binding.verifyTime.text = data.time
        holder.binding.verifyDetails.text = data.dish
    }
}