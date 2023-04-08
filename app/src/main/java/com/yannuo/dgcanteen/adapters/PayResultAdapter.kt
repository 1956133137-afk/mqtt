package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemPayResultDishsBinding
import com.yannuo.dgcanteen.model.DishesInfo

class PayResultAdapter  : BaseAdapter<DishesInfo, ItemPayResultDishsBinding> (){
    private var TAG = javaClass.simpleName



    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemPayResultDishsBinding {
        return  ItemPayResultDishsBinding.inflate(inflater, parent, false)
    }


    override fun bindHolder(holder: Holder, position: Int) {
        val dat = data.get(position);
        holder.binding.tvDishName.text =dat.dishesName
        holder.binding.tvDishCount.text = "x${dat.count} 份"
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }



}