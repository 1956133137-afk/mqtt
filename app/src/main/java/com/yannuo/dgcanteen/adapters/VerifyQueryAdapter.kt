package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.databinding.ItemVerifyQueryBinding
import com.yannuo.dgcanteen.model.Verify

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/12/17 9:40
 **/
class VerifyQueryAdapter(context: Context) : BaseAdapter<Verify, ItemVerifyQueryBinding>() {
    private val mContext = context

    private val adapterList: MutableList<VerifyQueryDishAdapter> = mutableListOf()

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemVerifyQueryBinding {
        return ItemVerifyQueryBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val verify = getData(position)
        holder.binding.verifyMeal.text = "${verify.mealName} 待核销菜品:"
        /*配置适配器*/
        if (adapterList.size <= position) adapterList.add(VerifyQueryDishAdapter()) else adapterList[position] = VerifyQueryDishAdapter()
        holder.binding.dishListView.layoutManager = LinearLayoutManager(mContext)
        holder.binding.dishListView.adapter = adapterList[position]
        holder.binding.dishListView.isNestedScrollingEnabled = false
        adapterList[position].data = verify.dishesList
    }
}