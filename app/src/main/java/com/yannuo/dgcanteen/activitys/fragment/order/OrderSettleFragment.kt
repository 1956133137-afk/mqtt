package com.yannuo.dgcanteen.activitys.fragment.order

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.adapters.InfoAdapter
import com.yannuo.dgcanteen.adapters.ListDishAdapter
import com.yannuo.dgcanteen.databinding.FragmentOrderSettleBinding
import com.yannuo.dgcanteen.model.InfoBean
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil

class OrderSettleFragment : BaseFragment<FragmentOrderSettleBinding>() {
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private val listDishAdapter by lazy { ListDishAdapter() }
    private val infoAdapter by lazy { InfoAdapter() }
    private var orderForUI: OrderForUI = OrderForUI()

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOrderSettleBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        val directions: OrderSettleFragmentArgs by navArgs()
        orderForUI = directions.orderForUI
        showPayResult(orderForUI)

        binding.dishListView.layoutManager = LinearLayoutManager(requireContext())
        binding.dishListView.adapter = listDishAdapter
        listDishAdapter.data = orderForUI.dishList
        totalMoneyCompute()

        binding.payResView.layoutManager = LinearLayoutManager(requireContext())
        binding.payResView.adapter = infoAdapter

        initEvent()
    }

    private fun initEvent() {
        binding.radioGroup.setOnCheckedChangeListener { radioGroup, checkId ->
            when (checkId) {
                R.id.btn_one -> orderForUI.distribute = "1"
                R.id.btn_two -> orderForUI.distribute = "2"
            }
        }

        //确定支付
        binding.btnConfirm.setOnClickListener {
            if (orderForUI.distribute == "0") {
                ToastShowUtil.show("请先选择配送方式")
                return@setOnClickListener
            }
            if (orderForUI.distribute == "1") {
                val inputPhone = binding.inputPhone.text.toString()
                val inputAddress = binding.inputAddress.text.toString()
                val inputRemark = binding.inputRemark.text.toString()
                if (inputPhone.length != 11) {
                    ToastShowUtil.show("联系电话位数不足")
                    return@setOnClickListener
                }
                if (inputAddress.isEmpty()) {
                    ToastShowUtil.show("联系电话位数不足")
                    return@setOnClickListener
                }
                orderForUI.phone = inputPhone
                orderForUI.address = inputAddress
                orderForUI.remark = inputRemark
            }
            LogUtil.d(TAG, Gson().toJson(orderForUI))
        }
        //继续订餐
        binding.btnReorder.setOnClickListener { if (findNavController().previousBackStackEntry != null) findNavController().popBackStack() }
        //取消订餐
        binding.btnCancel.setOnClickListener {
            orderMealVM.getUserName().value = ""
            findNavController().navigate(R.id.orderSettle_to_verifyUser)
        }
    }

    private fun totalMoneyCompute() {
        var totalCount: Int = 0
        var totalMoney: Double = 0.0
        orderForUI.dishList.forEach {
            totalCount += it.dishCount
            totalMoney += it.dishPrice.toDouble() * it.dishCount
        }
        orderForUI.payment = String.format("%.02f", totalMoney)
        orderForUI.actualPayment = String.format("%.02f", totalMoney)
        binding.tvTotalCount.text = "$totalCount"
        binding.tvTotalMoney.text = String.format("%.02f元", totalMoney)
    }

    // 绿:#82D582 红:#FF5252
    private fun showPayResult(orderForUI: OrderForUI, resultStr: String = "等待支付", colorStr: String = "#4F4F4F") {
        binding.payResult.text = resultStr
        binding.payResult.setTextColor(Color.parseColor(colorStr))
        val infoList = mutableListOf<InfoBean>()
        when (resultStr) {
            "等待支付" -> infoAdapter.clear()
            "支付成功" -> {
                infoList.add(InfoBean("订单编号: ", orderForUI.orderId))
                infoList.add(InfoBean("实付金额: ", orderForUI.actualPayment))
                infoList.add(InfoBean("支付时间: ", orderForUI.payTime))
            }
            else -> {
                infoList.add(InfoBean("错误代码: ", orderForUI.errCode))
                infoList.add(InfoBean("错误信息: ", orderForUI.errMsg))
            }
        }
        infoAdapter.data = infoList
    }
}