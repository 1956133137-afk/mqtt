package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.ItemSelectDishBinding
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 17:02
 **/
class SelectDishAdapter : BaseAdapter<DishBean, ItemSelectDishBinding>() {
    private val repeatMap: HashMap<String, Int> = hashMapOf()
    private var listener: SelectDishListener? = null
    private val mmkv = MMKV.defaultMMKV()

    fun setDishListener(listener: SelectDishListener?) {
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
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            if (!judgeDishLimit()) {
                ToastShowUtil.show("餐别订餐份数已达上限！")
                return@setOnClickListener
            }
            val dishBean = mData[position]
            mData[position].dishCount++
            notifyItemChanged(position, "dishCount")
            listener?.onSelectDish(dishBean)
        }
        holder.binding.ivBtnMinus.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val dishBean = mData[position]
            if (mData[position].dishCount > 0) {
                mData[position].dishCount--
                if (mData[position].dishCount == 0) removeData(position) else notifyItemChanged(position, "dishCount")
                listener?.onSelectDish(dishBean)
            }
        }
    }

    private fun judgeDishLimit(): Boolean {
        /*是否限购*/
        if (!mmkv.decodeBool(Constant.ORDER_MEAL_LIMIT_SWITCH, false)) return true
        /*是否已达餐别订餐份数上限*/
        var dishTotalCount = 0
        mData.forEach { dishTotalCount += it.dishCount }
        return mmkv.decodeInt(Constant.ORDER_MEAL_SIZE) + dishTotalCount < mmkv.decodeInt(Constant.ORDER_MEAL_LIMIT_SIZE, 1)
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
        fun onSelectDish(dishBean: DishBean)
    }
}