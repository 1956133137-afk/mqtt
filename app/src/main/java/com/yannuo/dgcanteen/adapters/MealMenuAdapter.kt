package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ItemMealMenuBinding
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.MealMenu
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 17:02
 **/
class MealMenuAdapter(val context: Context) : BaseAdapter<MealMenu, ItemMealMenuBinding>() {
    private val adapterList: MutableList<SelectDishAdapter> = mutableListOf()
    private var listener: SelectMealListener? = null

    fun setMealListener(listener: SelectMealListener?) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemMealMenuBinding {
        return ItemMealMenuBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.tvMealName.text = bean.mealName
        if (adapterList.size <= position) adapterList.add(SelectDishAdapter()) else adapterList[position] = SelectDishAdapter()
        holder.binding.dishListView.layoutManager = LinearLayoutManager(context)
        holder.binding.dishListView.itemAnimator = null
        holder.binding.dishListView.adapter = adapterList[position]
        adapterList[position].data = bean.dishList
        adapterList[position].setDishListener(bean.isLimit, bean.limitSize, bean.size, object : SelectDishAdapter.SelectDishListener {
            override fun onSelectDish(dishBean: DishBean) {
                val mealId = getData(position).mealId
                updateDish(position)
                listener?.onSelectMeal(mealId, dishBean)
            }
        })
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    private fun updateDish(position: Int) {
        var dishIndex = -1
        val mData = getData(position)
        mData?.dishList?.forEachIndexed { index, dish -> if (dish.dishCount == 0) dishIndex = index }
        if (dishIndex != -1) mData.dishList.removeAt(dishIndex)
        if (mData != null && mData.dishList.size < 1) {
            data.removeAt(position)
            notifyDataSetChanged()
        }
    }

    interface SelectMealListener {
        fun onSelectMeal(mealId: String, dishBean: DishBean)
    }
}