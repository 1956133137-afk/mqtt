package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.LaundryRecordItemBinding
import com.yannuo.dgcanteen.model.LaundryRecord

class LaundryRecordAdapter : BaseAdapter<LaundryRecord, LaundryRecordItemBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): LaundryRecordItemBinding {
        return LaundryRecordItemBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val record = mData[position]
        holder.binding.laundryOrderIndex.text = record.index.toString()
        holder.binding.laundryOrderTime.text = record.orderTime
        holder.binding.laundryPickupTime.text = record.pickupTime
        holder.binding.laundryClothingType.text = record.clothingType
    }
}
