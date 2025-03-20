package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yannuo.dgcanteen.databinding.ItemInitModeBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/18 9:34
 **/
class InitModeAdapter : BaseAdapter<String, ItemInitModeBinding>() {
    private var currentTime: Long = 0
    private var listener: OnItemClickListener? = null

    fun setItemListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemInitModeBinding {
        return ItemInitModeBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        holder.binding.initMode.text = getData(position)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.initMode.setOnClickListener {
            val position = holder.adapterPosition
            if (!judgeReClick() || position == RecyclerView.NO_POSITION) return@setOnClickListener
            listener?.onItemClick(getData(position))
        }
    }

    private fun judgeReClick(): Boolean {
        if (System.currentTimeMillis() - currentTime < 1000) return false
        currentTime = System.currentTimeMillis()
        return true
    }

    interface OnItemClickListener {

        fun onItemClick(modeName: String)
    }
}