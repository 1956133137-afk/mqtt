package com.yannuo.dgcanteen.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ItemMealPreparationFinishBinding
import com.yannuo.dgcanteen.dialogView.MealPreparationDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import java.util.*

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/15 16:52
 **/
class MealPreparationFinishAdapter(private val context: Context) : BaseAdapter<Order, ItemMealPreparationFinishBinding>() {
    private val mealPreparationDialog by lazy { MealPreparationDialog(context) }
    private var currentTime: Long = 0
    private val mealMap = MyApplication.mealMap

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemMealPreparationFinishBinding {
        return ItemMealPreparationFinishBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("SetTextI18n")
    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.businessName.text = bean.orderId
        val money = String.format("%.02f", bean.actualPayment.ifEmpty { "0.00" }.toDouble() - bean.refundPayment.ifEmpty { "0.00" }.toDouble())
        val fromHtml = Html.fromHtml("<s>${String.format("%.02f", bean.payment.toDouble())}元</s> <font color='#FF0000'>${money}元</font>")
        holder.binding.orderPayment.text = if (money.toDouble() != bean.payment.toDouble()) fromHtml else "${money}元"
        holder.binding.mealName.text = bean.personName
        val containsKey = mealMap[bean.mealId]
        holder.binding.windows.text = containsKey ?: "未知"
        holder.binding.orderType.text = if(bean.orderType == "1") "配送" else "自提"
        holder.binding.orderTime.text = bean.payTime
        holder.binding.useMealTime.text = bean.mealDate
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.btnPrinter.setOnClickListener {
            val position = holder.adapterPosition
            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
            printer(getData(position))
        }
        holder.binding.btnDetails.setOnClickListener {
            val position = holder.adapterPosition
            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
            val data = getData(position)
            if (!mealPreparationDialog.isShowing) {
                mealPreparationDialog.show()
                mealPreparationDialog.setOrderDishDetails(data.dcOrderDishesList, data.packagingFee ?: "0.00", data.deliveryFee ?: "0.00", data.remark ?: "")
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
        USBPrinterHelper.instance.printTicket("3", orderForUI)
    }

    fun release() {
        mealPreparationDialog.cancel()
    }
}