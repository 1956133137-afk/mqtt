package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemDishDetailsBinding
import com.yannuo.dgcanteen.model.DcOrderDishes
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.PictureUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/25 11:02
 **/
class DishDetailsAdapter(val context: Context) : BaseAdapter<DcOrderDishes, ItemDishDetailsBinding>() {
    private val mmkv = MMKV.defaultMMKV()
    private var listener: OnItemClickListener? = null

    fun setItemListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemDishDetailsBinding {
        return ItemDishDetailsBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val bean = getData(position)
//        if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) holder.binding.tvName.isSingleLine = false
        holder.binding.tvName.text = bean.dishesName
        holder.binding.tvPrice.text = "￥${String.format("%.02f", bean.dishesPrice.toDouble())}"
        val count = bean.dishesNum.toInt() - bean.dishesRefundNum.toInt()
        val totalPayment = count * bean.dishesPrice.toDouble()
        holder.binding.tvCount.text = count.toString()
        holder.binding.tvSubtotal.text = "${String.format("%.02f", totalPayment)}元"
        Glide.with(holder.binding.imgPic)
            .load(if (bean.imgUrl != null && bean.imgUrl.isNotEmpty()) PictureUtil.getPictureName(bean.imgUrl, context) else "")
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()
            .placeholder(R.drawable.load_failure)
            .fallback(R.drawable.load_failure)
            .into(holder.binding.imgPic)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.tvName.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val description = getData(position).description ?: ""
            listener?.onItemClick(description.replace("(<p>|</p>|\n|\r|\n\r|\r\n)".toRegex(), ""))
        }
    }

    interface OnItemClickListener {

        fun onItemClick(description: String)
    }
}