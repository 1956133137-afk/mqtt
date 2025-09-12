package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ProductOnShowBinding
import com.yannuo.dgcanteen.databinding.ProductShowBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.PictureUtil


class ProductsOnAdapter(context :Context?,type: Int) : BaseAdapter<DishesInfo,ProductOnShowBinding> (){
    private var listener: WorkListener ?= null
    private var cnt = context
    private var type = type
    private var wh: GridLayoutManager? = null
    private var sizeWH = 200
    private var firstCaclulate = true
    private var holdWidth = 0

    fun update(da :DishesInfo){
        var result = false
        for(index in data.indices){
            result = data[index].dishesId == da.dishesId
            if (result) {
                data[index].count = da.count
                notifyItemChanged(index, "count")
                break
            }
        }
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ProductOnShowBinding {
        holdWidth = parent.width
        return ProductOnShowBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = mData[position]
        holder.binding.tvName.text = data.dishesName
        holder.binding.tvNumber.text = "￥${data.price}"
        holder.binding.countText.text = data.count.toString()

        if (firstCaclulate) {
            wh?.let {
               val long = holdWidth - it.paddingLeft - it.paddingRight -  (holder.itemView.marginLeft + holder.itemView.marginRight +
                       holder.itemView.paddingLeft +  holder.itemView.paddingRight
                       ) * it.spanCount

                sizeWH = (long / it.spanCount)
                firstCaclulate = false
            }
        }
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()){
            holder.binding.countText.text = data[position].count.toString()
        }
        else{
          bindHolder(holder, position)
        }
    }

    override fun addEventListener(holder :Holder) {
        holder.binding.cvCountAdd.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION)return@setOnClickListener
            mData[position].count += 1
            holder.binding.countText.text  = mData[position].count.toString()
            notifyItemChanged(position,"count")
            listener?.onEventClickAdd(position,type)
        }
        holder.binding.cvCountSubtract.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION)return@setOnClickListener
            if(mData[position].count > 0){
                mData[position].count -= 1
                holder.binding.countText.text  = mData[position].count.toString()
                notifyItemChanged(position,"count")
                listener?.onEventClickSub(position,type)
            }
        }
    }

    fun setListener(listener : WorkListener?){
        this.listener = listener
    }

    fun setImgSize(manager: GridLayoutManager) {
        wh =manager
    }

    interface WorkListener{
        fun onEventClickAdd(position :Int,type: Int)
        fun onEventClickSub(position :Int,type: Int)
    }
}