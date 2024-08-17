package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ItemGridLayoutBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.PictureUtil
import com.yannuo.dgcanteen.util.ToastShowUtil


class DishesManageAdapter(context :Context) : BaseAdapter<DishesInfo,ItemGridLayoutBinding> (){
    private var listener: WorkListener?= null
    private var cnt = context

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemGridLayoutBinding {
        return ItemGridLayoutBinding.inflate(inflater,parent,false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = mData.get(position)

        holder.binding.tvName.text = data.dishesName
//        val spanStr = SpannableString(cnt.getString(R.string.money_format,data.price.toString()))
//        spanStr.setSpan(AbsoluteSizeSpan(28),1,spanStr.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
//        holder.binding.tvPrice.text = spanStr
        holder.binding.tvPrice.text = "￥${data.price}"

        Glide.with(cnt).load(PictureUtil.getPictureName(data.imgUrl, cnt))
            .diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.no_picture)
            .transform(CenterCrop(), GranularRoundedCorners(20f,20f,0f,0f))
            .into(holder.binding.igImg)

        updateData(holder,position)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()){
            LogUtil.d(TAG,"update payload:  ${payloads.get(0)}")
        }
        else{
            bindHolder(holder, position)
        }
    }

    override fun addEventListener(holder :Holder) {
        holder.binding.btnStatus.setOnClickListener {
            val position = holder.adapterPosition
            var data = mData[position]
            if (data.status == 0) {
                data.status = 1
                DishesDBHelper.getInstance().updateDishes( data.dishesId, data.mealId, 1)
                ToastShowUtil.show( cnt, "菜品已经上架")
            } else {
                data.status = 0
                DishesDBHelper.getInstance().updateDishes( data.dishesId, data.mealId, 0)
                ToastShowUtil.show( cnt, "菜品已经下架")
            }
            updateData(holder, position)
            listener?.onEventClick(position)
        }
    }

    private fun updateData(holder: Holder, position: Int) {
        if (mData[position].status == 1) {
            holder.binding.btnStatus.setText(R.string.under_shelf)
            holder.binding.btnStatus.setTextColor(cnt.getColor(R.color.under_black))
            holder.binding.btnStatus.background = cnt.getDrawable(R.drawable.shape_btn_one)
            holder.binding.dropdownIc.visibility = View.INVISIBLE
            holder.binding.dropdownBg.visibility = View.INVISIBLE
        } else {
            holder.binding.btnStatus.setText(R.string.shelf)
            holder.binding.btnStatus.setTextColor(cnt.getColor(R.color.white))
            holder.binding.btnStatus.background = cnt.getDrawable(R.drawable.shape_btn_two)
            holder.binding.dropdownIc.visibility = View.VISIBLE
            holder.binding.dropdownBg.visibility = View.VISIBLE
        }
    }

    fun setListener(listener : WorkListener?){
        this.listener = listener
    }

    interface WorkListener{
        fun onEventClick(position :Int)
    }
}