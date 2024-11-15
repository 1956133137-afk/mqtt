package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemOrderRecordBinding
import com.yannuo.dgcanteen.model.Order
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/15 16:52
 **/
class OrderRecordAdapter(context: Context) : BaseAdapter<Order, ItemOrderRecordBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderRecordBinding {
        return ItemOrderRecordBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.businessName.text = bean.businessName
        holder.binding.mealDate.text = bean.mealDate
        holder.binding.weekName.text = getWeekDay(bean.mealDate)
        holder.binding.mealName.text = bean.mealId
        holder.binding.orderType.text = if (bean.orderType == "1") "配送" else "自提"
        holder.binding.timeName.text = if (bean.orderType == "1") "配送时间: " else "用餐时间: "
        holder.binding.useMealTime.text = "${bean.startTime} - ${bean.endTime}"
        holder.binding.orderPayment.text = "${String.format("%.02f", bean.actualPayment.toDouble())}元"
        holder.binding.orderTime.text = bean.orderTime
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    private fun getWeekDay(date: String?): String {
        if (date == null || date.isEmpty() || !isValidDate(date)) return ""
        val split = date.split("-".toRegex())
        val calendar = Calendar.getInstance()
        calendar.set(split[0].toInt(), split[1].toInt() - 1, split[2].toInt())
        return when (calendar[Calendar.DAY_OF_WEEK]) {
            Calendar.SUNDAY -> "星期天"
            Calendar.MONDAY -> "星期一"
            Calendar.TUESDAY -> "星期二"
            Calendar.WEDNESDAY -> "星期三"
            Calendar.THURSDAY -> "星期四"
            Calendar.FRIDAY -> "星期五"
            Calendar.SATURDAY -> "星期六"
            else -> "未知"
        }
    }

    private fun isValidDate(dateFormat: String): Boolean {
        val regex = Regex("""^\d{4}-\d{2}-\d{2}$""", RegexOption.IGNORE_CASE)
        return regex.matches(dateFormat)
    }
}