package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yannuo.dgcanteen.databinding.ItemKeyboardBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/12 14:22
 **/
class KeyboardAdapter : BaseAdapter<String, ItemKeyboardBinding>() {
    private var listener: OnFigureCallback? = null

    fun setListener(listener: OnFigureCallback) {
        this.listener = listener
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemKeyboardBinding {
        return ItemKeyboardBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        holder.binding.figure.text = getData(position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.figure.setOnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            listener?.onFigure(getData(position))
        }
    }

    interface OnFigureCallback {
        fun onFigure(figure: String)
    }
}