package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.dao.VerifyDishes
import com.yannuo.dgcanteen.databinding.ItemVerifyDishesBinding

class VerifyDishesAdapter: BaseAdapter<VerifyDishes, ItemVerifyDishesBinding>() {
    override fun bindHolder(holder: Holder?, position: Int) {
        val data = mData[position]
        holder?.binding?.verifyTime?.text = data.time
        holder?.binding?.verifyInfo?.text = "${data.personName}已取餐"
    }

    override fun bindHolder(holder: Holder?, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun getB(inflater: LayoutInflater?, parent: ViewGroup?): ItemVerifyDishesBinding {
        return ItemVerifyDishesBinding.inflate(inflater!!, parent, false)
    }

    // 分割并重新组合字符串，确保最后一项不换行
    private fun formatString(str: String): String {
        return str.split("|").joinToString(prefix = "", postfix = "", separator = "\n") { item ->
            // 不需要额外添加换行符，因为 joinToString 已经处理了
            item
        }.trimEnd('\n')
    }
    
}