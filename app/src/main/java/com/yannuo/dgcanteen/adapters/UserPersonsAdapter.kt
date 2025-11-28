package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemUserPersonsBinding
import com.yannuo.dgcanteen.greendao.entity.Persons

class UserPersonsAdapter: BaseAdapter<Persons,ItemUserPersonsBinding>() {
    private var listener: OnClickListener? = null
    override fun bindHolder(holder: Holder, position: Int) {

    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        val data = getData(position)
        holder.binding.name.text = data.personName
        holder.binding.phone.text = data.phone
    }

    override fun getB(inflater: LayoutInflater, parent: ViewGroup?): ItemUserPersonsBinding {
        return ItemUserPersonsBinding.inflate(inflater, parent, false)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.root.setOnClickListener {
            val layoutPosition = holder.layoutPosition
            val data = getData(layoutPosition)
            listener?.clickUserPersons(data)
        }
    }

    fun setListener(listener : OnClickListener){
        this.listener = listener
    }

    interface OnClickListener{
        fun clickUserPersons(persons: Persons)
    }
}