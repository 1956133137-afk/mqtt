package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemPayListBinding
import com.yannuo.dgcanteen.databinding.ItemPayListVariantBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.LogUtil

class ShopsAdapter(context :Context)  : BaseAdapter<DishesInfo, ItemPayListVariantBinding> (){
    private var TAG = javaClass.simpleName

    private var cnt = context


    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemPayListVariantBinding {
        return   ItemPayListVariantBinding.inflate(inflater, parent, false)
    }


    override fun bindHolder(holder: Holder, position: Int) {
        val dat = data.get(position);
        holder.binding.tvItemName.text =dat.dishesName
        holder.binding.tvCount.text = "${dat.count}"
        holder.binding.tvMoney.text = calculate(dat)
        cnt.let { Glide.with(it).load(dat.imgUrl).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(
            R.drawable.no_picture)
            .transform(CenterCrop(), GranularRoundedCorners(10f,10f,0f,0f)).into(holder.binding.ivShopPic) }

    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }


    private fun calculate(data: DishesInfo):String{
        return try {
            data.price.toBigDecimal().multiply(data.count.toBigDecimal()).toString()
        }catch (e :Exception){
            LogUtil.e(TAG,e.message)
            "0.0"
        }
    }
}