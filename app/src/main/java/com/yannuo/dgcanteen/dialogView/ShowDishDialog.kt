package com.yannuo.dgcanteen.dialogView

import android.content.Context
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.DialogShowDishBinding

class ShowDishDialog(context: Context): BaseDialog<DialogShowDishBinding>(context) {
    private var dishes: String? = null
    override fun initDialogView() {
        binding = DialogShowDishBinding.inflate(layoutInflater)
    }

    override fun initOperation() {
        var stringBuilder = StringBuilder()
        val dishesVerification = Gson().fromJson(dishes, Array<String>::class.java)
        for (s in dishesVerification) {
            stringBuilder.append("$s\n")
        }
        binding.tvDishes.text = stringBuilder
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    fun showDishes(dish: String) {
        dishes = dish
    }

}