package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yannuo.dgcanteen.databinding.ItemCartBinding
import com.yannuo.dgcanteen.model.DishesInfo

class CartAdapter(
    private val increaseClickListener: (DishesInfo) -> Unit,
    private val decreaseClickListener: (DishesInfo) -> Unit,
    private val deleteClickListener: (DishesInfo) -> Unit
) : ListAdapter<DishesInfo, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DishesInfo) {
            binding.tvClothingName.text = item.dishesName
            binding.tvClothingCount.text = item.count.toString()
            binding.tvClothingPrice.text = "件"
            binding.btnIncrease.setOnClickListener { increaseClickListener(item) }
            binding.btnDecrease.setOnClickListener { decreaseClickListener(item) }
            binding.btnDelete.setOnClickListener { deleteClickListener(item) }
        }
    }

    private class CartDiffCallback : DiffUtil.ItemCallback<DishesInfo>() {
        override fun areItemsTheSame(oldItem: DishesInfo, newItem: DishesInfo): Boolean {
            return oldItem.dishesId == newItem.dishesId
        }

        override fun areContentsTheSame(oldItem: DishesInfo, newItem: DishesInfo): Boolean {
            return oldItem == newItem
        }
    }
}
