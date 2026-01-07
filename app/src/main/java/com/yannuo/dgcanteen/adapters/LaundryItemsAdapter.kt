package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemFoodsBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.PictureUtil

class LaundryItemsAdapter(context: Context?) : BaseAdapter<DishesInfo, ItemFoodsBinding>() {

    private var listener: WorkListener? = null
    private var cnt: Context? = context

    fun update(da: DishesInfo) {
        for (index in data.indices) {
            if (data[index].dishesId == da.dishesId) {
                data[index].count = da.count
                notifyItemChanged(index, "count")
                break
            }
        }
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemFoodsBinding {
        return ItemFoodsBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val item = mData[position]
        with(holder.binding) {
            tvClothingName.text = item.dishesName
            tvClothingPrice.text = "洗衣项目"

            cnt?.let {
                Glide.with(it)
                    .load(PictureUtil.getPictureName(item.imgUrl, cnt))
                    .placeholder(R.drawable.ic_clothing_default)
                    .error(R.drawable.ic_clothing_default)
                    .into(ivClothingImage)
            }

            if (item.count > 0) {
                llQuantity.visibility = View.VISIBLE
                tvClothingCount.text = item.count.toString()
            } else {
                llQuantity.visibility = View.GONE
            }
        }
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val item = mData[position]
            if (item.count > 0) {
                holder.binding.llQuantity.visibility = View.VISIBLE
                holder.binding.tvClothingCount.text = item.count.toString()
            } else {
                holder.binding.llQuantity.visibility = View.GONE
            }
        } else {
            bindHolder(holder, position)
        }
    }

    override fun addEventListener(holder: Holder) {
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener?.onEventClick(position)
            }
        }
    }

    fun setListener(listener: WorkListener?) {
        this.listener = listener
    }

    interface WorkListener {
        fun onEventClick(position: Int)
    }
}
