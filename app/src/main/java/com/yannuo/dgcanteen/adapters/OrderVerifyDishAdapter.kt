package com.yannuo.dgcanteen.adapters

import android.graphics.Color
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
    private var colorStr: String = "#4F4F4F"

    fun setColorStr(colorStr: String) {
        this.colorStr = colorStr
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderVerifyDishBinding {
        return ItemOrderVerifyDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        /*字体颜色*/
        holder.binding.verifyDishName.setTextColor(Color.parseColor(colorStr))
        holder.binding.verifyDishPrice.setTextColor(Color.parseColor(colorStr))
        holder.binding.verifyDishCount.setTextColor(Color.parseColor(colorStr))
        /*数据*/
        holder.binding.verifyDishName.text = bean.dishesName
        holder.binding.verifyDishPrice.text = "¥${bean.price}"
        holder.binding.verifyDishCount.text = "${bean.dishesNum}/${bean.unit}"
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }
}