package com.yannuo.dgcanteen.activitys.fragment.laundry

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.activitys.fragment.order.BaseFragment
import com.yannuo.dgcanteen.activitys.viewModel.LaundryModeVM
import com.yannuo.dgcanteen.adapters.CartAdapter
import com.yannuo.dgcanteen.adapters.InfoAdapter
import com.yannuo.dgcanteen.adapters.LaundryItemsAdapter
import com.yannuo.dgcanteen.databinding.FragmentPlaceLaundryOrderBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.InfoBean
import com.yannuo.dgcanteen.model.OrderForUI
import java.text.DecimalFormat

class PlaceLaundryOrderFragment : BaseFragment<FragmentPlaceLaundryOrderBinding>() {

    private val laundryModeVM by lazy { ViewModelProvider(requireActivity())[LaundryModeVM::class.java] }
    private lateinit var laundryItemsAdapter: LaundryItemsAdapter
    private lateinit var cartAdapter: CartAdapter
    private val infoAdapter by lazy { InfoAdapter() }
    private var awaitingDialog: AwaitingDialog? = null
    private var orderForUI: OrderForUI = OrderForUI()

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentPlaceLaundryOrderBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        initRecyclerView()
        observeViewModel()
        initEvent()
        laundryModeVM.getClothingTypes()
    }

    private fun initRecyclerView() {
        laundryItemsAdapter = LaundryItemsAdapter(requireContext()).apply {
            setListener(object : LaundryItemsAdapter.WorkListener {
                override fun onEventClick(position: Int) {
                    val selectedItem = laundryItemsAdapter.getData(position)
                    laundryModeVM.addToCart(selectedItem)
                }
            })
        }
        binding.rvClothingItems.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = laundryItemsAdapter
        }

        cartAdapter = CartAdapter(
            increaseClickListener = { laundryModeVM.increaseCartItem(it) },
            decreaseClickListener = { laundryModeVM.decreaseCartItem(it) },
            deleteClickListener = { laundryModeVM.removeCartItem(it) }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }

        binding.payResView.layoutManager = LinearLayoutManager(requireContext())
        binding.payResView.adapter = infoAdapter
    }

    private fun initEvent() {
        binding.btnConfirmPayment.setOnClickListener {
            if (laundryModeVM.cartItems.value.isNullOrEmpty()) {
                return@setOnClickListener
            }
            laundryModeVM.placeLaundryOrder()
        }

        binding.btnCancelPayment.setOnClickListener {
            // Reset to initial state
            laundryModeVM.cartItems.value = emptyList()
            binding.rvCartItems.visibility = View.VISIBLE
            binding.layoutPaymentResult.visibility = View.GONE
            binding.tvHeaderCart.text = "已选衣物"
            binding.btnCancelPayment.text = "清空"
            binding.btnConfirmPayment.isEnabled = true
        }
    }

    private fun observeViewModel() {
        laundryModeVM.clothingItems.observe(viewLifecycleOwner) { dishes ->
            laundryItemsAdapter.setData(dishes)
        }

        laundryModeVM.cartItems.observe(viewLifecycleOwner) { cartItems ->
            cartAdapter.submitList(cartItems)
            val totalCount = cartItems.sumOf { it.count }
            binding.tvTotalCount.text = "$totalCount 件"
            // 在洗衣房登录成功后，禁用副屏点击功能
            // 获取副屏实例并禁用点击功能
//            val dishDisplay = (requireActivity() as? com.yannuo.dgcanteen.activitys.LaundryActivity)?.getDishDisplay()
//            dishDisplay?.disableClickFunction()
        }

        laundryModeVM.setOrderListener(object : LaundryModeVM.OrderMealListener {
            override fun onOrderResult(type: Int, any: Any) {
                handler.post {
                    if (awaitingDialog == null) awaitingDialog = AwaitingDialog(requireActivity())
                    when (type) {
                        2 -> {
                            if (awaitingDialog?.isShowing == false) awaitingDialog?.show()
                            awaitingDialog?.updateText(any.toString())
                        }
                        4 -> {
                            awaitingDialog?.dismiss()
                            if (any is LaundryOrderResult) {
                                val orderResult = any
                                if (orderResult.orderForUI.result == "Y") {
                                    showPayResult(orderResult.orderForUI, orderResult.totalCount, "下单成功", "#82D582")
                                } else {
                                    showPayResult(orderResult.orderForUI, 0, "下单失败", "#FF5252")
                                }
                            } else if (any is OrderForUI) {
                                // Fallback for other order types that don't have totalCount
                                if (any.result == "Y") {
                                    showPayResult(any, 0, "下单成功", "#82D582")
                                } else {
                                    showPayResult(any, 0, "下单失败", "#FF5252")
                                }
                            }
                        }
                        else -> awaitingDialog?.dismiss()
                    }
                }
            }
        })
    }

    private fun showPayResult(orderForUI: OrderForUI, totalCount: Int, resultStr: String, colorStr: String) {
        binding.tvHeaderCart.text = "下单结果"
        binding.rvCartItems.visibility = View.GONE
        binding.layoutPaymentResult.visibility = View.VISIBLE
        binding.payResultStatusText.text = resultStr
        binding.payResultStatusText.setTextColor(Color.parseColor(colorStr))

        binding.btnCancelPayment.text = "完成"
        binding.btnConfirmPayment.isEnabled = false

        val infoList = mutableListOf<InfoBean>()
        when (resultStr) {
            "下单成功" -> {
                infoList.add(InfoBean("订单编号: ", orderForUI.orderId))
                if (totalCount > 0) {
                    infoList.add(InfoBean("衣物件数: ", "$totalCount 件"))
                }
            }
            else -> {
                infoList.add(InfoBean("错误代码: ", orderForUI.errCode))
                infoList.add(InfoBean("错误信息: ", orderForUI.errMsg))
            }
        }
        infoAdapter.data = infoList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitingDialog?.dismiss()
    }
}

/**
 * A data class to hold the result of a laundry order, separating concerns from the shared OrderForUI class.
 * This class is internal to the module, ensuring it doesn't leak into other parts of the app like the dining module.
 */
internal data class LaundryOrderResult(val orderForUI: OrderForUI, val totalCount: Int)
