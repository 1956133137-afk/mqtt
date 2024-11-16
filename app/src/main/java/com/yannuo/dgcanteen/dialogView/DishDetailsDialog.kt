package com.yannuo.dgcanteen.dialogView

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.DishDetailsAdapter
import com.yannuo.dgcanteen.databinding.DialogDishDetailsBinding
import com.yannuo.dgcanteen.model.DcOrderDishes

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/16 9:33
 **/
class DishDetailsDialog(context: Context) : BaseDialog<DialogDishDetailsBinding>(context) {
    private val mContext = context
    private val dishDetailsAdapter by lazy { DishDetailsAdapter(mContext) }

    override fun initDialogView() {
        binding = DialogDishDetailsBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.dishDetailsView.layoutManager = LinearLayoutManager(context)
        binding.dishDetailsView.adapter = dishDetailsAdapter

        binding.btnClose.setOnClickListener { dismiss() }
    }

    fun setOrderDishDetails(dishList: MutableList<DcOrderDishes>) {
        dishDetailsAdapter.data = dishList
        var totalCount = 0
        var totalPayment = 0.0
        dishList.forEach {
            totalCount += it.dishesNum.toInt()
            totalPayment += it.dishesNum.toInt() * it.dishesPrice.toDouble()
        }
        binding.totalCount.text = "$totalCount"
        binding.totalPayment.text = "${String.format("%.02f", totalPayment)}"
    }
}