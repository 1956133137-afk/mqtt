package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.DishDetailsAdapter
import com.yannuo.dgcanteen.databinding.DialogDishDetailsBinding
import com.yannuo.dgcanteen.model.DcOrderDishes
import com.yannuo.dgcanteen.util.ToastShowUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/16 9:33
 **/
class DishDetailsDialog(context: Context) : BaseDialog<DialogDishDetailsBinding>(context) {
    private val mContext = context
    private val handler = Handler(mContext.mainLooper)
    private val dishDetailsAdapter by lazy { DishDetailsAdapter(mContext) }

    override fun initDialogView() {
        binding = DialogDishDetailsBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.dishDetailsView.layoutManager = LinearLayoutManager(context)
        binding.dishDetailsView.adapter = dishDetailsAdapter
        dishDetailsAdapter.setItemListener(object : DishDetailsAdapter.OnItemClickListener {
            override fun onItemClick(description: String) {
                handler.post { if (description.isNotEmpty()) binding.mvControl.text = description else ToastShowUtil.show("没有菜品描述") }
            }
        })

        binding.btnClose.setOnClickListener { dismiss() }
    }

    fun setOrderDishDetails(dishList: MutableList<DcOrderDishes>, packagingFee: String = "0.00", deliveryFee: String = "0.00") {
        val list: MutableList<DcOrderDishes> = mutableListOf()
        var totalCount = 0
        var totalPayment = 0.0
        dishList.forEach {
            val count = it.dishesNum.toInt() - it.dishesRefundNum.toInt()
            if (count > 0) {
                list.add(it)
                totalCount += count
                totalPayment += count * it.dishesPrice.toDouble()
            }
        }
        totalPayment += packagingFee.toDouble() + deliveryFee.toDouble()
        dishDetailsAdapter.data = list
        binding.tvPackageFee.text = String.format("%.02f", packagingFee.toDouble())
        binding.tvDeliveryFee.text = String.format("%.02f", deliveryFee.toDouble())
        binding.totalCount.text = "$totalCount"
        binding.totalPayment.text = "${String.format("%.02f", totalPayment)}"
    }
}