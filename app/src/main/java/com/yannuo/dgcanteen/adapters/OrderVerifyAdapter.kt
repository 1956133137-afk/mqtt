package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.R
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
        /*配置适配器*/
        if (adapterList.size <= position) adapterList.add(OrderVerifyDishAdapter()) else adapterList[position] = OrderVerifyDishAdapter()
        holder.binding.orderVerifyDish.layoutManager = LinearLayoutManager(mContext)
        holder.binding.orderVerifyDish.adapter = adapterList[position]
        holder.binding.orderVerifyDish.isNestedScrollingEnabled = false

        changeUI(bean.verifyType, holder, position)

        if (bean.verifyMsg.isNotEmpty()) {
            holder.binding.verifyMsg.visibility = View.VISIBLE
            holder.binding.verifyMsg.text = "描述：${bean.verifyMsg}"
        }

        adapterList[position].data = bean.verifyDishList
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    private fun changeUI(verifyType: Int, holder: Holder, position: Int) {
        when (verifyType) {
            0 -> {
                holder.binding.verifyStatus.text = "核销成功"
                holder.binding.ll1.setBackgroundResource(R.drawable.shape_bg_green_10)
            }
            1 -> {
                holder.binding.verifyStatus.text = "核销失败"
                holder.binding.ll1.setBackgroundResource(R.drawable.shape_bg_red_10)
            }
            else -> {
                holder.binding.verifyStatus.text = "等待核销"
                holder.binding.ll1.setBackgroundResource(R.drawable.shape_bg_white_10)
            }
        }
        when (verifyType) {
            0, 1 -> {
                holder.binding.verifyStatus.setTextColor(Color.parseColor("#FFFFFF"))
                holder.binding.verifyLine.setBackgroundColor(Color.parseColor("#FFFFFF"))
                adapterList[position].setColorStr("#FFFFFF")
            }
            else -> {
                holder.binding.verifyStatus.setTextColor(Color.parseColor("#4F4F4F"))
                holder.binding.verifyLine.setBackgroundColor(Color.parseColor("#4F4F4F"))
                adapterList[position].setColorStr("#4F4F4F")
            }
        }

    }

}