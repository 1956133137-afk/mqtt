package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemSuccessListBinding
import com.yannuo.dgcanteen.model.DishesInfo

class SucShopsAdapter(context : Context)  : BaseAdapter<DishesInfo, ItemSuccessListBinding> () {

    private var TAG = javaClass.simpleName

    private var cnt = context

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemSuccessListBinding {
        return   ItemSuccessListBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val dat = data.get(position);
        holder.binding.tvDishName.text =dat.dishesName
        holder.binding.tvDishCount.text = "${dat.count} 份"
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

}