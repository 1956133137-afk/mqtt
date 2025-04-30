package com.yannuo.dgcanteen.adapters

import android.os.Handler
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.viewModel.MealPreparationVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ItemMealPreparationBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.MealPreparationDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.DcOrderDishes
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.Order
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.model.OrderStatusBean
import com.yannuo.dgcanteen.model.WindowBean
import com.yannuo.dgcanteen.printer.USBPrinterHelper

class MealPreparationAdapter(private val requireContext: FragmentActivity) : BaseAdapter<Order,ItemMealPreparationBinding>() {
    private var awaitingDialog: AwaitingDialog? = null
    private var windowList: MutableList<WindowBean> = mutableListOf()
    private val mealPreparationDialog by lazy { MealPreparationDialog(requireContext) }
    private val handler: Handler = Handler(MyApplication.applicationContext.mainLooper)
    private val mealPreparationVM by lazy { ViewModelProvider(requireContext, ViewModelProvider.AndroidViewModelFactory(requireContext.application))[MealPreparationVM::class.java] }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.businessName.text = bean.orderId
        val money = String.format("%.02f", bean.actualPayment.ifEmpty { "0.00" }.toDouble() - bean.refundPayment.ifEmpty { "0.00" }.toDouble())
        val fromHtml = Html.fromHtml("<s>${String.format("%.02f", bean.payment.toDouble())}元</s> <font color='#FF0000'>${money}元</font>")
        holder.binding.orderPayment.text = if (money.toDouble() != bean.payment.toDouble()) fromHtml else "${money}元"
        holder.binding.mealName.text = bean.personName
        val mealId = MyApplication.mealMap[bean.mealId]
        holder.binding.mealId.text = if(mealId.isNullOrEmpty()) "未知" else MyApplication.mealMap[bean.mealId]
        holder.binding.orderType.text = if(bean.orderType == "1") "配送" else "自提"
        holder.binding.orderTime.text = bean.payTime
        holder.binding.useMealTime.text = bean.mealDate
        holder.binding.windows.text = getWindows(bean.dcOrderDishesList)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemMealPreparationBinding {
        return ItemMealPreparationBinding.inflate(inflater, parent, false)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.btnDetails.setOnClickListener {
            val position = holder.adapterPosition
            val data = getData(position)
            if (!mealPreparationDialog.isShowing) {
                mealPreparationDialog.show()
                mealPreparationDialog.setOrderDishDetails(data.dcOrderDishesList, data.packagingFee ?: "0.00", data.deliveryFee ?: "0.00", data.remark ?: "")
            }
        }
        //完成备餐
        holder.binding.btnPrinter.setOnClickListener {
            if (awaitingDialog == null) awaitingDialog = AwaitingDialog(requireContext)
            val position = holder.adapterPosition
            if(position == -1) return@setOnClickListener
            awaitingDialog?.show()
            val myData = data
            //修改订单状态
            val bean = OrderStatusBean().apply {
                this.businessId = data[position].businessId
                this.campusId = data[position].campusId
                this.orderId = data[position].orderId
                this.orderStatus = "1"
            }
            //监听修改结果
            mealPreparationVM.setMealListener(object : MealPreparationVM.OnMealListener{
                override fun onMeal(type: Int, data: Any) {
                    if(type == 1){
                        handler.post {
                            printer(myData[position])
                            notifyItemRemoved(position)
                            myData.removeAt(position)
                            mealPreparationVM.setInfoBeanList(getData())
                        }
                    }
                    if(awaitingDialog != null) awaitingDialog?.dismiss()
                }
            })
            //发起状态变更
            mealPreparationVM.modifyTheOrderStatus(bean)
        }
    }

    /**
     * 打印订单
     */
    private fun printer(order: Order) {
        val person = DishesDBHelper.getInstance().queryPersonToCustId(order.custId)
        val orderForUI = Gson().fromJson(Gson().toJson(order), OrderForUI::class.java)
        orderForUI.custName = if (person != null) person.personName else ""
        orderForUI.orderDate = order.mealDate
        orderForUI.distribute = order.orderType
        orderForUI.deliveryFee = if(order.deliveryFee.isNullOrEmpty()) "" else order.deliveryFee
        orderForUI.packagingFee = if(order.packagingFee.isNullOrEmpty()) "" else order.packagingFee
        if(!order.verificationCode.isNullOrEmpty()){
            orderForUI.verificationCode = order.verificationCode
        }
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

    private fun isValidDate(dateFormat: String): Boolean {
        val regex = Regex("""^\d{4}-\d{2}-\d{2}$""", RegexOption.IGNORE_CASE)
        return regex.matches(dateFormat)
    }

    fun release() {
        mealPreparationDialog.cancel()
    }
}