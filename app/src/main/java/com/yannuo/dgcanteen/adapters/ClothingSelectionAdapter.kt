package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemClothingSelectionBinding
import com.yannuo.dgcanteen.model.ClothingItem

class ClothingSelectionAdapter : BaseAdapter<ClothingItem, ItemClothingSelectionBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemClothingSelectionBinding {
        return ItemClothingSelectionBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val item = getData(position)
        holder.binding.tvClothingName.text = item.name
        holder.binding.cbClothingItem.isChecked = item.isSelected
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.cbClothingItem.setOnCheckedChangeListener { _, isChecked ->
            val position = holder.layoutPosition
            if (position != -1) { // Check for valid position
                val item = getData(position)
                item.isSelected = isChecked
            }
        }
    }

    // Helper function to get all selected items
    fun getSelectedItems(): List<ClothingItem> {
        return mData.filter { it.isSelected }
    }
}
