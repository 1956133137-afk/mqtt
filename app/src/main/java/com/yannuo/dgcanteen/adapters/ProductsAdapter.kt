package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
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
import com.yannuo.dgcanteen.databinding.ProductShowBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.ProductInfo
import com.yannuo.dgcanteen.util.LogUtil


class ProductsAdapter(title :String?,context :Context?) : BaseAdapter<DishesInfo,ProductShowBinding> (){
    private var listener: WorkListener ?= null
    private var cnt = context
    private var wh: GridLayoutManager? = null
    private var sizeWH = 200
    private var firstCaclulate = true

    init {
        TAG = "ProductsAdapter-$title"
    }

    fun update(da :DishesInfo){
        var result = false
        for(index in data.indices){
            result = data[index].dishesId.equals(da.dishesId)
            if (result) {
                data[index].count = da.count
                notifyItemChanged(index, "count")
                break
            }
        }
    }


    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ProductShowBinding {
        return ProductShowBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
//        LogUtil.d(TAG,"bindHolder(holder: Holder, $position: Int)")
        val data = mData.get(position)
        holder.binding.tvName.text = data.dishesName
        cnt?.also {
            val spanStr = SpannableString(it.getString(R.string.money_format,data.price.toString()))
            spanStr.setSpan(AbsoluteSizeSpan(28),1,spanStr.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            holder.binding.tvNumber.text = spanStr
        }
//        holder.binding.tvIndex.text = "${(position+1)}"
        holder.binding.cvCountAdd.updateValue(data.count)
        val layoutParams = holder.binding.ivPic.layoutParams

        if (firstCaclulate) {
            wh?.let {
               val long = it.width - it.paddingLeft - it.paddingRight -  (holder.itemView.marginLeft + holder.itemView.marginRight +
                       holder.itemView.paddingLeft +  holder.itemView.paddingRight
                       ) * it.spanCount

                sizeWH = (long / it.spanCount)
                firstCaclulate = false
            }
        }

        layoutParams.height = sizeWH
        layoutParams.width = sizeWH

        holder.binding.ivPic.layoutParams = layoutParams
//        if (data.filename.isNullOrEmpty().not()) {
        cnt?.let { Glide.with(it).load(data.imgUrl).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.no_picture)
            .transform(CenterCrop(), GranularRoundedCorners(10f,10f,0f,0f)).into(holder.binding.ivPic) }
//        }

    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()){
            holder.binding.cvCountAdd.updateValue(data[position].count)
            LogUtil.d(TAG,"update payload:  ${payloads.get(0)}")
        }
        else{
          bindHolder(holder, position)
        }
    }



    override fun addEventListener(holder :Holder) {

//        holder.binding.cvCountAdd.setOnClickListener {
//            val position = holder.adapterPosition
//            LogUtil.d(TAG,"添加 $position")
//            if (position == RecyclerView.NO_POSITION)return@setOnClickListener
//            mData.get(position).count +=1
//            notifyItemChanged(position,"count")
//            listener?.onEventClick(position)
//        }

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            LogUtil.d(TAG,"添加 $position")
            if (position == RecyclerView.NO_POSITION)return@setOnClickListener
            mData.get(position).count +=1
            notifyItemChanged(position,"count")
            listener?.onEventClick(position)
        }


    }

    fun setListener(listener : WorkListener?){
        this.listener = listener
    }

    fun setImgSize(manager: GridLayoutManager) {
//        wh = manager.width -  (manager.paddingLeft + manager.paddingRight)
        wh =manager


    }

    interface WorkListener{
        fun onEventClick(position :Int)
    }
}