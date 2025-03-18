package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemOrderDishBinding
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.PictureUtil
import com.yannuo.dgcanteen.util.ToastShowUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 11:02
 **/
class OrderDishAdapter(val context: Context) : BaseAdapter<DishBean, ItemOrderDishBinding>() {
    private var listener: OrderDishListener? = null
    private val mmkv = MMKV.defaultMMKV()

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
            .load(if (bean.imgUrl.isNotEmpty()) PictureUtil.getPictureName(bean.imgUrl, context) else "")
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
        holder.binding.rlDescription.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            listener?.onOrderDishDescription("菜品描述:\n${(mData[position].description ?: "").replace("(\n|\r|\n\r|\r\n)".toRegex(), "")}")
        }
        holder.binding.btnAdd.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            if (!judgeMealLimit()) {
                ToastShowUtil.show("餐别订餐份数已达上限！")
                return@setOnClickListener
            }
            mData[position].dishCount++
            notifyItemChanged(position, "dishCount")
            listener?.onOrderDish(mData[position])
        }
        holder.binding.btnMinus.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            if (mData[position].dishCount > 0) {
                mData[position].dishCount--
                notifyItemChanged(position, "dishCount")
                listener?.onOrderDish(mData[position])
            }
        }
    }

    private fun judgeMealLimit(): Boolean {
        /*是否限购*/
        if (!mmkv.decodeBool(Constant.ORDER_MEAL_LIMIT_SWITCH, false)) return true
        /*是否已达餐别订餐份数上限*/
        var dishTotalCount = 0
        mData.forEach { dishTotalCount += it.dishCount }
        return mmkv.decodeInt(Constant.ORDER_MEAL_SIZE) + dishTotalCount < mmkv.decodeInt(Constant.ORDER_MEAL_LIMIT_SIZE, 1)
    }

    fun updateDishCount(dishBean: DishBean) {
        mData.forEachIndexed { position, dish ->
            if (dish.dishId == dishBean.dishId) {
                dish.dishCount = dishBean.dishCount
                notifyItemChanged(position, "dishCount")
            }
        }
    }

    interface OrderDishListener {
        fun onOrderDish(bean: DishBean)
        fun onOrderDishDescription(description: String)
    }
}