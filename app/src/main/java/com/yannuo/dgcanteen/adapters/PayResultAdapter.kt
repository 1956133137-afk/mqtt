package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemPayResultDishsBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.Dish
import com.yannuo.dgcanteen.util.PictureUtil

class PayResultAdapter(context: Context) : BaseAdapter<Dish, ItemPayResultDishsBinding>() {
    private var TAG = javaClass.simpleName

    //    var textsize = -1f  //45
    private var cnt = context

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemPayResultDishsBinding {
        return ItemPayResultDishsBinding.inflate(inflater, parent, false)
    }


    override fun bindHolder(holder: Holder, position: Int) {
        val dat = data.get(position);
//        if (textsize > 0){
//            if (holder.binding.tvDishName.textSize != textsize) {
//                holder.binding.tvDishName.textSize = textsize
//                holder.binding.tvDishCount.textSize = textsize
//            }
//        }
        val dishList = DishesDBHelper.getInstance().queryDishById(dat.dishesId)
        val dishImgUrl = if (dishList != null && dishList.size > 0) dishList[0].imgUrl else ""

        holder.binding.tvDishName.text = dat.dishesName
        holder.binding.tvDishCount.text = "x${dat.dishesNumber} 份"
        cnt.let {
            Glide.with(it)
                .load(PictureUtil.getPictureName(dishImgUrl, cnt))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.no_picture)
                .transform(CenterCrop(), GranularRoundedCorners(10f, 10f, 10f, 10f))
                .into(holder.binding.ivShopPic)
        }

    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }


}