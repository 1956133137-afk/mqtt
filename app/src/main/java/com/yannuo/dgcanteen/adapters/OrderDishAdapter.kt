package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemOrderDishBinding
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.OrderDish
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 11:02
 **/
class OrderDishAdapter : BaseAdapter<DishBean, ItemOrderDishBinding>() {
    private var listener: OrderDishListener? = null

    fun setDishListener(listener: OrderDishListener) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemOrderDishBinding {
        return ItemOrderDishBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
        holder.binding.price.text = "￥${bean.dishPrice}/${bean.dishUnit}"
        holder.binding.dishName.text = bean.dishName
        holder.binding.tvCount.text = bean.dishCount.toString()
        Glide.with(holder.binding.imgPic)
            .load(bean.imgUrl)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()
            .placeholder(R.drawable.load_failure)
            .fallback(R.drawable.load_failure)
            .into(holder.binding.imgPic)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()) holder.binding.tvCount.text = getData(position).dishCount.toString()
        else bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.btnAdd.setOnClickListener {
            val position = holder.adapterPosition
            mData[position].dishCount++
            notifyItemChanged(position, "dishCount")
            listener?.onOrderDish(mData[position])
        }
        holder.binding.btnMinus.setOnClickListener {
            val position = holder.adapterPosition
            if (mData[position].dishCount > 0) {
                mData[position].dishCount--
                notifyItemChanged(position, "dishCount")
                listener?.onOrderDish(mData[position])
            }
        }
    }

    fun updateDishCount(dishId: String) {
        mData.forEachIndexed { position, dish ->
            if (dish.dishId == dishId) notifyItemChanged(position, "dishCount")
        }
    }

    interface OrderDishListener {
        fun onOrderDish(bean: DishBean)
    }
}