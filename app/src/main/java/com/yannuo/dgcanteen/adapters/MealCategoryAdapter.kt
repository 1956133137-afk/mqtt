package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemSelectMealBinding
import com.yannuo.dgcanteen.model.CategoryBean

class MealCategoryAdapter : BaseAdapter<CategoryBean, ItemSelectMealBinding>()  {
    private var listener: MealCategoryListener? = null

    fun setCategoryListener(listener: MealCategoryListener) {
        this.listener = listener
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.tvMealName.text = bean.categoryName.ifEmpty { "未知" }
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemSelectMealBinding {
        return ItemSelectMealBinding.inflate(inflater, parent, false)
    }

    override fun addEventListener(holder: Holder) {
        super.addEventListener(holder)
        holder.binding.tvMealName.setOnClickListener{
            val position = holder.adapterPosition
            listener?.onMealCategory(getData(position))
        }
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        super.bindHolder(holder, position, payloads)
        val bean = getData(position)
        holder.binding.tvMealName.text = bean.categoryName.ifEmpty { "未知" }
    }

    interface MealCategoryListener {
        fun onMealCategory(bean: CategoryBean)
    }
}