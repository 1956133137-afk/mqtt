package com.yannuo.dgcanteen.adapters

import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.dao.ProductsTable
import com.yannuo.dgcanteen.databinding.ProductAddItemBinding
import com.yannuo.dgcanteen.util.LogUtil


class ProductAddAdapter(path :String) : BaseAdapter<ProductsTable, ProductAddItemBinding> (){

    private var listener: WorkListener?= null
    private var picPath = path


    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ProductAddItemBinding {
        return ProductAddItemBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = mData.get(position)
        holder.binding.tvPNumber.text = data.barCode
        holder.binding.tvName.text = data.pName
        holder.binding.tvType.text = data.type
        holder.binding.tvPrice.text = data.pMoney

        Glide.with(holder.itemView.context)
            .load("$picPath${data.pictureName}")
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .placeholder(R.drawable.no_picture)
            .into(holder.binding.ivPic)
    }



    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }


    fun insertOrUpdate(da :ProductsTable){
        var position = -1
        var result = false

        for(index in data.indices){
            result = data[index].barCode.equals(da.barCode)
            if (result) {
                position = index
                break
            }
        }

        if (position != -1){
            data.removeAt(position)
            notifyItemRemoved(position)
        }
        data.add(0,da)
        notifyItemInserted(0)

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun addEventListener(holder: Holder) {
        holder.itemView.setOnLongClickListener {
            val popup = PopupMenu(it.context,it)
            popup.gravity = Gravity.RIGHT
            val inflater =  popup.menuInflater
            inflater.inflate(R.menu.exter,popup.menu)
            popup.setOnMenuItemClickListener {
                return@setOnMenuItemClickListener when (it.itemId){
                    R.id.edit ->{
                        val position = holder.adapterPosition
                        if (position == RecyclerView.NO_POSITION) return@setOnMenuItemClickListener true
                        LogUtil.d(TAG,"编辑")
                        listener?.onEventClick(0,mData.get(position) as Object)
                        true
                    }
                    R.id.del -> {
                        val position = holder.adapterPosition
                        if (position == RecyclerView.NO_POSITION) return@setOnMenuItemClickListener true
                        LogUtil.d(TAG,"删除")
                        val bean = mData.get(position)
                        mData.removeAt(position)
                        notifyItemRemoved(position)
                        listener?.onEventClick(1,bean as Object)
                        true
                    }
                    else ->{
                        false
                    }
                }
            }
            popup.show()
            return@setOnLongClickListener true
        }
    }


    //编辑监听
    fun setListener(listen : WorkListener?){
        this.listener = listen
    }

    interface WorkListener{
        fun onEventClick(event :Int ,position :Object?)
    }
}