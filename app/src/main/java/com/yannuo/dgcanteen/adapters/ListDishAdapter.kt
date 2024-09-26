package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemListDishBinding
import com.yannuo.dgcanteen.model.DishBean

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/26 11:37
 **/
class ListDishAdapter : BaseAdapter<DishBean, ItemListDishBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemListDishBinding {
        return ItemListDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.dishName.text = bean.dishName
        holder.binding.dishPrice.text = "${bean.dishPrice}/${bean.dishUnit}"
        holder.binding.dishCount.text = "${bean.dishCount}"
        val totalMoney = bean.dishPrice.toDouble() * bean.dishCount
        holder.binding.dishTotalPrice.text = String.format("%.02f", totalMoney)
        Glide.with(holder.binding.dishImg)
            .load(bean.imgUrl)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()
            .placeholder(R.drawable.load_failure)
            .fallback(R.drawable.load_failure)
            .into(holder.binding.dishImg)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {

    }
}