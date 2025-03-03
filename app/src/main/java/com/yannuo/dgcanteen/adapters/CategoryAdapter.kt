package com.yannuo.dgcanteen.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.CatrgoryShowBinding
import com.yannuo.dgcanteen.greendao.entity.CategoryTable
import com.yannuo.dgcanteen.util.ToastShowUtil

class CategoryAdapter : BaseAdapter<CategoryTable, CatrgoryShowBinding>() {
    private var listener: WorkListener?= null

    fun setListener(listener : WorkListener?){
        this.listener = listener
    }


    override fun bindHolder(holder: Holder, position: Int) {
        holder.binding.ivCategory.text = getData(position).categoryName
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        super.addEventListener(holder)
        holder.binding.ivCategory.setOnClickListener {
            Log.d(TAG, "addEventListener: 序号：${holder.adapterPosition}")
            listener?.onEventClickCategory(getData(holder.adapterPosition))
        }
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): CatrgoryShowBinding {
        return CatrgoryShowBinding.inflate(inflater, parent, false)
    }

    interface WorkListener{
        fun onEventClickCategory(category: CategoryTable)
    }
}