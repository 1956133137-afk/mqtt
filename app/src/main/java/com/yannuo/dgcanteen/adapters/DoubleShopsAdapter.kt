package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemPayListDoubleBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.LogUtil
import okhttp3.internal.http2.Http2Connection

class DoubleShopsAdapter(context : Context)  : BaseAdapter<DishesInfo, ItemPayListDoubleBinding> () {
    private var cnt = context
    private var listener: DishListener? = null

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemPayListDoubleBinding {
        return   ItemPayListDoubleBinding.inflate(inflater, parent, false)
    }


    override fun bindHolder(holder: Holder, position: Int) {
        val dat = data[position]
        holder.binding.tvItemName.text =dat.dishesName
        holder.binding.tvCount.text = "${dat.count} 份"
        holder.binding.tvMoney.text = "￥${dat.price}"
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.addView.setOnClickListener{
            val position = holder.adapterPosition
            val dishesInfo = data[position]
            listener?.onModifyDish(dishesInfo,true)
        }
        holder.binding.subView.setOnClickListener{
            val position = holder.adapterPosition
            val dishesInfo = data[position]
            listener?.onModifyDish(dishesInfo,false)
        }
    }

    fun setListener(listener: DishListener){
        this.listener = listener
    }


    private fun calculate(data: DishesInfo):String{
        return try {
            data.price.toBigDecimal().multiply(data.count.toBigDecimal()).toString()
        }catch (e :Exception){
            LogUtil.e(TAG,e.message)
            "0.0"
        }
    }

    interface DishListener{

        fun onModifyDish(bean : DishesInfo, type: Boolean)

    }
}