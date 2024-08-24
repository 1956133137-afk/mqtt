package com.yannuo.dgcanteen.dialogView

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.ShowDishAdapter
import com.yannuo.dgcanteen.databinding.DialogDishesBinding
import com.yannuo.dgcanteen.model.Dish

class ShowDishesDialog(context: Context) : BaseDialog<DialogDishesBinding>(context) {
    private val adapter: ShowDishAdapter by lazy { ShowDishAdapter() }
    private val handler = Handler(context.mainLooper)

    override fun initDialogView() {
        binding = DialogDishesBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        binding.rvDishes.layoutManager = LinearLayoutManager(context)
        binding.rvDishes.adapter = adapter
    }

    fun showWithDishDetails(dishList: MutableList<Dish>) {
        adapter.data = dishList
        var totalPrice = 0.0F
        dishList.forEach { totalPrice += String.format("%.2f", it.dishesPrice.toDouble() * it.dishesNumber.toDouble()).toFloat() }
        handler.post {
            binding.dishesTotal.text = "￥${totalPrice}"
            binding.btnClose.setOnClickListener { cancel() }
        }
    }

}