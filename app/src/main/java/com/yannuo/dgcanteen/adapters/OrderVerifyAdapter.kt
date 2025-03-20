package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.databinding.ItemOrderVerifyBinding
import com.yannuo.dgcanteen.model.OrderVerify

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/20 17:16
 **/
class OrderVerifyAdapter(context: Context) : BaseAdapter<OrderVerify, ItemOrderVerifyBinding>() {
    private val mContext = context
    private val adapterList: MutableList<OrderVerifyDishAdapter> = mutableListOf()

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderVerifyBinding {
        return ItemOrderVerifyBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.verifyStatus.text = when (bean.verifyType) {
            0 -> "核销成功"
            1 -> "核销失败"
            else -> "等待核销"
        }
        if (bean.verifyMsg.isNotEmpty()) {
            holder.binding.verifyMsg.visibility = View.VISIBLE
            holder.binding.verifyMsg.text = "描述：${bean.verifyMsg}"
        }

        if (adapterList.size <= position) adapterList.add(OrderVerifyDishAdapter()) else adapterList[position] = OrderVerifyDishAdapter()
        holder.binding.orderVerifyDish.layoutManager = LinearLayoutManager(mContext)
        holder.binding.orderVerifyDish.adapter = adapterList[position]
        adapterList[position].data = bean.verifyDishList
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

}