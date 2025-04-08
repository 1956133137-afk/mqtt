package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemDishVerifyCountBinding
import com.yannuo.dgcanteen.model.InfoBean

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/26 17:23
 **/
class DishVerifyCountAdapter : BaseAdapter<InfoBean, ItemDishVerifyCountBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemDishVerifyCountBinding {
        return ItemDishVerifyCountBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.describe.text = bean.describe
        holder.binding.content.text = bean.content
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {

    }
}