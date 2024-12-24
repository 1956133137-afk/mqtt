package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ItemOrderRecordBinding
import com.yannuo.dgcanteen.dialogView.DishDetailsDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.Order
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/15 16:52
 **/
class OrderRecordAdapter(context: Context) : BaseAdapter<Order, ItemOrderRecordBinding>() {
    private val dishDetailsDialog by lazy { DishDetailsDialog(context) }
    private var listener: OnItemClickListener? = null
    private var currentTime: Long = 0

    fun setItemListener(listener: OnItemClickListener?) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderRecordBinding {
        return ItemOrderRecordBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.businessName.text = bean.businessName
        holder.binding.mealDate.text = bean.mealDate
        holder.binding.weekName.text = getWeekDay(bean.mealDate)
        holder.binding.mealName.text = bean.mealName
        holder.binding.orderType.text = if (bean.orderType == "1") "配送" else "自提"
        holder.binding.timeName.text = if (bean.orderType == "1") "配送时间: " else "用餐时间: "
        holder.binding.useMealTime.text = "${bean.startTime} - ${bean.endTime}"
        holder.binding.orderPayment.text =
            "${String.format("%.02f", bean.actualPayment.ifEmpty { "0.00" }.toDouble() - bean.refundPayment.ifEmpty { "0.00" }.toDouble())}元"
        holder.binding.orderTime.text = bean.orderTime
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.btnRefund.setOnClickListener {
            val position = holder.adapterPosition
            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
            listener?.onItemClick(position)
        }
        holder.binding.btnPrinter.setOnClickListener {
            val position = holder.adapterPosition
            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
            printer(getData(position))
        }
        holder.binding.btnDetails.setOnClickListener {
            val position = holder.adapterPosition
            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
            val data = getData(position)
            if (!dishDetailsDialog.isShowing) {
                dishDetailsDialog.show()
                dishDetailsDialog.setOrderDishDetails(data.dcOrderDishesList, data.packagingFee ?: "0.00", data.deliveryFee ?: "0.00")
            }
        }
    }

    private fun judgeReClick(): Boolean {
        if (System.currentTimeMillis() - currentTime < 1000) return false
        currentTime = System.currentTimeMillis()
        return true
    }

    private fun printer(order: Order) {
        val person = DishesDBHelper.getInstance().queryPersonToCustId(order.custId)
        val orderForUI = Gson().fromJson(Gson().toJson(order), OrderForUI::class.java)
        orderForUI.custName = if (person != null) person.personName else ""
        orderForUI.orderDate = order.mealDate
        orderForUI.distribute = order.orderType
        order.dcOrderDishesList.forEach {
            val dishBean = DishBean().apply {
                dishId = it.dishesId
                dishName = it.dishesName
                dishPrice = it.dishesPrice
                dishCount = it.dishesNum.toInt()
                dishUnit = it.unit
            }
            orderForUI.dishList.add(dishBean)
        }
        USBPrinterHelper.instance.printTicket("2", orderForUI)
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

    fun release() {
        dishDetailsDialog.cancel()
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}