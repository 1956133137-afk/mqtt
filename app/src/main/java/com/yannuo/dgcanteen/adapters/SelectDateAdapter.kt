package com.yannuo.dgcanteen.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemSelectDateBinding
import com.yannuo.dgcanteen.model.SelectDateBean
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/24 16:16
 **/
class SelectDateAdapter : BaseAdapter<SelectDateBean, ItemSelectDateBinding>() {
    private var selectPos = 0
    private var listener: SelectDateListener? = null

    fun setDateListener(listener: SelectDateListener) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemSelectDateBinding {
        return ItemSelectDateBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.tvDate.text = bean.date
        holder.binding.tvWeek.text = getWeekName(bean.value)
        if (position == selectPos) {
            holder.binding.llView.setBackgroundResource(R.drawable.shape_bg_blue_1)
            holder.binding.tvDate.setTextColor(Color.parseColor("#FFFFFF"))
            holder.binding.tvWeek.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            holder.binding.llView.setBackgroundResource(R.drawable.shape_bg_white_1)
            holder.binding.tvDate.setTextColor(Color.parseColor("#4F4F4F"))
            holder.binding.tvWeek.setTextColor(Color.parseColor("#4F4F4F"))
        }
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.llView.setOnClickListener {
            val position = holder.adapterPosition
            selectPos = position
            listener?.onSelectDate(getData(position))
            notifyDataSetChanged()
        }
    }

    private fun getWeekName(value: String): String {
        return when (value) {
            "7" -> "星期日"
            "1" -> "星期一"
            "2" -> "星期二"
            "3" -> "星期三"
            "4" -> "星期四"
            "5" -> "星期五"
            "6" -> "星期六"
            else -> "未知"
        }
    }

    interface SelectDateListener {
        fun onSelectDate(bean: SelectDateBean)
    }
}