package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridView
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.DifferentCatrgoryBinding
import com.yannuo.dgcanteen.greendao.entity.CategoryTable
import okhttp3.internal.notify

class DifferentCategoryAdapter(context: Context): BaseAdapter<CategoryTable,DifferentCatrgoryBinding>() {
    private val mContext = context
    private var listener: CategoryListener?= null
    private var popupWindow: PopupWindow? = null
    private var btnA: Button? = null
    private var btnB: Button? = null

    private var win1: Int = 0
    private var win2: Int = 1

    override fun bindHolder(holder: Holder, position: Int) {

    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        holder.binding.ivCategory.text = data[position].categoryName
        holder.binding.ivCategory.background =  when(position){
            win1 -> AppCompatResources.getDrawable(mContext,R.drawable.shape_press_bt_bg_green)
            win2 -> AppCompatResources.getDrawable(mContext,R.drawable.shape_press_bt_bg_orange)
            else -> AppCompatResources.getDrawable(mContext,R.drawable.common_bt_bg)
        }
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): DifferentCatrgoryBinding {
        return DifferentCatrgoryBinding.inflate(inflater, parent, false)
    }

    override fun addEventListener(holder: Holder) {
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position == -1) return@setOnClickListener
            if(position == win1 || position == win2) return@setOnClickListener
            pop(it)
            btnA?.setOnClickListener {
                win1 = position
                listener?.onCategoryClick(0,data[position].categoryName)
                popupWindow?.dismiss()
                notifyDataSetChanged()
            }
            btnB?.setOnClickListener {
                win2 = position
                listener?.onCategoryClick(1,data[position].categoryName)
                popupWindow?.dismiss()
                notifyDataSetChanged()
            }
        }
    }

    private fun pop(v: View){
        val inflate = LayoutInflater.from(mContext).inflate(R.layout.popwindown_category_item, null,false)
        popupWindow = PopupWindow(inflate,ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,true)
        popupWindow?.elevation = 100f
        popupWindow?.showAsDropDown(v,135,-85)
        btnA = inflate.findViewById(R.id.bt_window_a)
        btnB = inflate.findViewById(R.id.bt_window_b)
    }

    fun setListener(listener : CategoryListener){
        this.listener = listener
    }

    interface CategoryListener{
        fun onCategoryClick(position :Int,name: String)
    }
}