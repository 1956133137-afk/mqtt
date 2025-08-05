package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yannuo.dgcanteen.databinding.ItemPayListBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.LogUtil


class PayForAdapter : BaseAdapter<DishesInfo,ItemPayListBinding> (){
    private var listener: WorkListener ?= null
    init {
        map = HashMap()
    }




    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemPayListBinding {
        return ItemPayListBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {

        holder.binding.tvItemName.text = data.get(position).dishesName
        holder.binding.tvThisMoney.text = "￥${data.get(position).price}"
        holder.binding.adAddSubtract.setCount(data[position].count)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        map[data[position].dishesId] = position //维护数据位置
        if (payloads.isEmpty().not()){
            holder.binding.tvThisMoney.text = "￥${data[position].price}"
            if(data[position].count > 0) {
                holder.binding.adAddSubtract.setCount(data[position].count)
            }
            else{
                map.remove(data[position].dishesId)
                mData.removeAt(position)
            }
        }
        else{
            bindHolder(holder, position)
        }
    }

    private fun calculate(data: DishesInfo):String{
        return try {
            data.price.toBigDecimal().multiply(data.count.toBigDecimal()).toString()
        }catch (e :Exception){
            LogUtil.e(TAG,e.message)
            "0.0"
        }
    }

    //操作同一个商品对象，手选择商品不进行累加，直接就可以更新界面；扫码选择商品的时候，需要进行数量累加操作
    fun insertedData(data: DishesInfo?,accumulation :Boolean) {
        data?.apply {
            val position = map[this.dishesId]
            if (position == null) {
                if (accumulation) this.count +=1
                mData.add(this)
                notifyItemInserted(itemCount -1)
            }else{
                if (accumulation) mData.get(position).count +=1
                notifyItemChanged(position,"count")
            }
        }
    }


    fun getSpecifyBarcode(dishesId :String?):DishesInfo?{
        var bean :DishesInfo ? = null
        try {
            for(index in data.indices){
                if (data[index].dishesId.equals(dishesId)) {
                    bean = data[index]
                    break
                }
            }
        }catch (e :Exception){e.printStackTrace()}
        return bean
    }


    override fun addEventListener(holder :Holder) {
        holder.binding.adAddSubtract.setMode(1)
        holder.binding.adAddSubtract.setListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setListener
            LogUtil.d(TAG,"添加 $position")
            val data = mData[position]
            data.count = it
            when(it){
                0-> {
                    mData.removeAt(position)
                    map.remove(data.dishesId)
                    notifyItemRemoved(position)
                }
                else ->{
                    notifyItemChanged(position,"count")
                }
            }
            listener?.onEventClick(data)
        }
    }

    fun setListener(listener : WorkListener?){
        this.listener = listener
    }

    interface WorkListener{
        fun onEventClick(position :DishesInfo)
    }
}