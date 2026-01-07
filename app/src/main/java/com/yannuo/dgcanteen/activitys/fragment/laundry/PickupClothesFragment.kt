package com.yannuo.dgcanteen.activitys.fragment.laundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.yannuo.dgcanteen.activitys.fragment.order.BaseFragment
import com.yannuo.dgcanteen.adapters.ClothingSelectionAdapter
import com.yannuo.dgcanteen.databinding.FragmentPickupClothesBinding
import com.yannuo.dgcanteen.model.ClothingItem

class PickupClothesFragment : BaseFragment<FragmentPickupClothesBinding>() {

    private lateinit var clothingSelectionAdapter: ClothingSelectionAdapter

    companion object {
        private const val ARG_ORDER_ID = "order_id"

        /**
         * Creates a new instance of the fragment for a specific laundry order.
         * @param orderId The unique ID of the laundry order to be picked up.
         */
        fun newInstance(orderId: Int): PickupClothesFragment {
            val fragment = PickupClothesFragment()
            val args = Bundle()
            args.putInt(ARG_ORDER_ID, orderId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentPickupClothesBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        clothingSelectionAdapter = ClothingSelectionAdapter()
        binding.rvClothingList.adapter = clothingSelectionAdapter

        // Get the order ID from arguments, default to a sample ID for demonstration
        val orderId = arguments?.getInt(ARG_ORDER_ID) ?: 101 // Default to order 101

        // Load clothes based on the specific order ID
        val clothingItems = getClothesForOrder(orderId)
        clothingSelectionAdapter.setData(clothingItems)

        // Set up the submit button
        binding.btnSubmitSelection.setOnClickListener {
            val selectedItems = clothingSelectionAdapter.getSelectedItems()
            if (selectedItems.isEmpty()) {
                Toast.makeText(context, "请至少选择一件衣物", Toast.LENGTH_SHORT).show()
            } else {
                val selectedNames = selectedItems.joinToString(separator = ", ") { it.name }
                Toast.makeText(context, "订单 $orderId 已提交: $selectedNames", Toast.LENGTH_LONG).show()
                // TODO: Add your actual submission logic here, using the orderId
            }
        }
    }

    /**
     * Simulates fetching a specific order's clothes from a database.
     * This is a mock data source and should be replaced with a real API call.
     * @param orderId The unique ID of the order.
     * @return A list of clothing items for the given order.
     */
    private fun getClothesForOrder(orderId: Int): List<ClothingItem> {
        // Mock database of orders
        val ordersDatabase = mapOf(
            101 to listOf(
                ClothingItem(1, "白色衬衫"),
                ClothingItem(2, "蓝色牛仔裤"),
                ClothingItem(3, "T恤")
            ),
            102 to listOf(
                ClothingItem(4, "被套"),
                ClothingItem(5, "床单"),
                ClothingItem(6, "枕巾")
            ),
            103 to listOf(
                ClothingItem(7, "西装"),
                ClothingItem(8, "领带"),
                ClothingItem(9, "羊毛衫")
            )
        )

        return ordersDatabase[orderId] ?: emptyList() // Return clothes for the ID, or empty list if not found
    }
}
