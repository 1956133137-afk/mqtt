package com.yannuo.dgcanteen.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.ItemOrderRecordBinding
import com.yannuo.dgcanteen.dialogView.DishDetailsDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import com.yannuo.dgcanteen.util.Constant
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/15 16:52
 **/
class OrderRecordAdapter(context: Context) : BaseAdapter<Order, ItemOrderRecordBinding>() {
    private val mmkv = MMKV.defaultMMKV()
    private val dishDetailsDialog by lazy { DishDetailsDialog(context) }
    private var listener: OnItemClickListener? = null
    private var currentTime: Long = 0
    private var windowList: MutableList<WindowBean> = mutableListOf()

    fun setItemListener(windowList: MutableList<WindowBean>, listener: OnItemClickListener?) {
        this.windowList = windowList
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemOrderRecordBinding {
        return ItemOrderRecordBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("SetTextI18n")
    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.businessName.text = bean.businessName
        holder.binding.mealDate.text = "${bean.mealDate}(${getWeekDay(bean.mealDate)})"
        holder.binding.mealName.text = bean.mealName
        holder.binding.windows.text = getWindows(bean.dcOrderDishesList)
        holder.binding.orderType.text = if (bean.orderType == "1") "配送" else "自提"
        holder.binding.timeName.text = if (bean.orderType == "1") "配送时间: " else "用餐时间: "
        holder.binding.useMealTime.text = "${bean.startTime} - ${bean.endTime}"
        val money = String.format("%.02f", bean.actualPayment.ifEmpty { "0.00" }.toDouble() - bean.refundPayment.ifEmpty { "0.00" }.toDouble())
        val fromHtml = Html.fromHtml("<s>${String.format("%.02f", bean.payment.toDouble())}元</s> <font color='#FF0000'>${money}元</font>")
        holder.binding.orderPayment.text = if (money.toDouble() != bean.payment.toDouble()) fromHtml else "${money}元"
        holder.binding.orderTime.text = bean.orderTime
        if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) {
            holder.binding.btnRefund.visibility = View.GONE
//            holder.binding.btnPrinter.visibility = View.GONE
        }
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
//        holder.binding.llWindows.setOnClickListener {
//            val position = holder.adapterPosition
//            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
//            val windowIdList: MutableList<String> = mutableListOf()
//            getData(position).dcOrderDishesList.forEach { dishes ->
//                dishes.windowIdList.split(",").forEach { if (it.isNotEmpty()) windowIdList.add(it) }
//            }
//            listener?.onItemWindow(windowIdList.distinct().toMutableList())
//        }
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

    private fun getWindows(dcOrderDishesList: MutableList<DcOrderDishes>): String {
        val windowIdList: MutableList<String> = mutableListOf()
        val windowStr = StringBuilder()
        dcOrderDishesList.forEach { dishes ->
            if (dishes.windowIdList.isNullOrBlank()) return@forEach
            dishes.windowIdList.split(",").forEach { if (it.isNotEmpty() && !windowIdList.contains(it)) windowIdList.add(it) }
        }
        windowIdList.forEach { windowId ->
            windowList.forEach { if (windowId == it.windowId) windowStr.append("${it.windowName}，") }
        }
        if (windowStr.isNotEmpty()) windowStr.deleteCharAt(windowStr.length - 1)
        return windowStr.toString()
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

//        fun onItemWindow(windowList: MutableList<String>)
    }
}