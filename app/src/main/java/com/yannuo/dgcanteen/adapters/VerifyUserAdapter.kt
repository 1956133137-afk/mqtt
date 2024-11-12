package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemVerifyUserBinding
import com.yannuo.dgcanteen.greendao.entity.VerifyDishes

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/12 15:27
 **/
class VerifyUserAdapter : BaseAdapter<VerifyDishes, ItemVerifyUserBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemVerifyUserBinding {
        return ItemVerifyUserBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = mData[position]
        holder.binding.verifyMsg.text = "${data.time}\n${data.personName}已取餐"
    }
}