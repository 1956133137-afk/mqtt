package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ItemDateMenuBinding
import com.yannuo.dgcanteen.model.DateMenu
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 17:02
 **/
class DateMenuAdapter(val context: Context) : BaseAdapter<DateMenu, ItemDateMenuBinding>() {
    private val adapterList: MutableList<MealMenuAdapter> = mutableListOf()
    private var listener: SelectDateListener? = null

    fun setDateListener(listener: SelectDateListener?) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemDateMenuBinding {
        return ItemDateMenuBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.tvDate.text = "${bean.date} ( ${getWeekName(bean.value)} )"
        if (adapterList.size <= position) adapterList.add(MealMenuAdapter(context)) else adapterList[position] = MealMenuAdapter(context)
        holder.binding.mealMenuView.layoutManager = LinearLayoutManager(context)
        holder.binding.mealMenuView.itemAnimator = null
        holder.binding.mealMenuView.adapter = adapterList[position]
        adapterList[position].data = bean.mealList
        adapterList[position].setMealListener(object : MealMenuAdapter.SelectMealListener {
            override fun onSelectMeal(mealId: String, dishBean: DishBean) {
                val date = getData(position).date
                updateDish(position)
                listener?.onSelectDate(date, mealId, dishBean)
            }
        })
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    private fun updateDish(position: Int) {
        var mealIndex = -1
        val mData = getData(position)
        mData?.mealList?.forEachIndexed { index, mealMenu -> if (mealMenu.dishList.size < 1) mealIndex = index }
        if (mealIndex != -1) mData.mealList.removeAt(mealIndex)
        if (mData != null && mData.mealList.size < 1) {
            data.removeAt(position)
            notifyDataSetChanged()
        }
    }

    private fun getWeekName(value: String): String {
        return when (value) {
            "7" -> "星期日"
            "1" -> "星期一"
            "2" -> "星期二"
            "3" -> "星期三"
            "4" -> "星期四"
            "5" -> "星期五"
            "6" -> "星期六"
            else -> "未知"
        }
    }

    interface SelectDateListener {
        fun onSelectDate(date: String, mealId: String, dishBean: DishBean)
    }
}