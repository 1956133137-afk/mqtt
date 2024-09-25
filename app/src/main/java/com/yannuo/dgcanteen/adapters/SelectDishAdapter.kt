package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yannuo.dgcanteen.databinding.ItemSelectDishBinding
import com.yannuo.dgcanteen.model.DishBean

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 17:02
 **/
class SelectDishAdapter : BaseAdapter<DishBean, ItemSelectDishBinding>() {
    private val repeatMap: HashMap<String, Int> = hashMapOf()
    private var listener: SelectDishListener? = null

    fun setDishListener(listener: SelectDishListener) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemSelectDishBinding {
        return ItemSelectDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.dishName.text = bean.dishName
        holder.binding.dishPrice.text = "￥${bean.dishPrice}/${bean.dishUnit}"
        holder.binding.dishCount.text = bean.dishCount.toString()
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()) holder.binding.dishCount.text = getData(position).dishCount.toString()
        else bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.ivBtnAdd.setOnClickListener {
            val position = holder.adapterPosition
            val dishId = mData[position].dishId
            mData[position].dishCount++
            notifyItemChanged(position, "dishCount")
            listener?.onSelectDish(dishId)
        }
        holder.binding.ivBtnMinus.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val dishId = mData[position].dishId
            if (mData[position].dishCount > 0) {
                mData[position].dishCount--
                if (mData[position].dishCount == 0) {
                    removeData(position)
                    repeatMap.remove(dishId)
                } else notifyItemChanged(position, "dishCount")
                listener?.onSelectDish(dishId)
            }
        }
    }

    fun insertedData(bean: DishBean) {
        repeatMap.clear()
        mData.forEachIndexed { index, dish -> repeatMap[dish.dishId] = index }
        val position = repeatMap[bean.dishId]
        if (position == null) {
            repeatMap[bean.dishId] = itemCount
            mData.add(bean)
            notifyItemChanged(itemCount - 1)
        } else {
//            mData[position].dishCount = bean.dishCount
            if (mData[position].dishCount == 0) removeData(position)
            else notifyItemChanged(position, "dishCount")
        }
    }

    interface SelectDishListener {
        fun onSelectDish(dishId: String)
    }
}