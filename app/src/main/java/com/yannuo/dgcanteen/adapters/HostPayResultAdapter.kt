package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.HostItemPayResultDishsBinding
import com.yannuo.dgcanteen.databinding.ItemPayResultDishsBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.PictureUtil

class HostPayResultAdapter(context : Context)  : BaseAdapter<DishesInfo, HostItemPayResultDishsBinding> (){
    private var TAG = javaClass.simpleName
    private var cnt = context


    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): HostItemPayResultDishsBinding {
        return  HostItemPayResultDishsBinding.inflate(inflater, parent, false)
    }


    override fun bindHolder(holder: Holder, position: Int) {
        val dat = data.get(position);

        holder.binding.tvDishName.text =dat.dishesName
        holder.binding.tvDishCount.text = "x${dat.count} 份"
        cnt.let { Glide.with(it).load(PictureUtil.getPictureName(dat.imgUrl, cnt)).diskCacheStrategy(
            DiskCacheStrategy.NONE).placeholder(
            R.drawable.no_picture)
            .transform(CenterCrop(), GranularRoundedCorners(10f,10f,10f,10f)).into(holder.binding.ivShopPic) }

    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }



}