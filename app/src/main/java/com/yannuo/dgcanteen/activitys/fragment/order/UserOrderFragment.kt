package com.yannuo.dgcanteen.activitys.fragment.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.databinding.FragmentUserOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.util.LogUtil

class UserOrderFragment : BaseFragment<FragmentUserOrderBinding>() {
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private val dbHelper = DishesDBHelper.getInstance()
    private var orderForUI: OrderForUI = OrderForUI()

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentUserOrderBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        val directions: UserOrderFragmentArgs by navArgs()
        orderForUI = directions.orderForUI
        initData()
        initEvent()
    }

    private fun initData() {
        if (orderForUI.custName.isEmpty()) {
            val person = dbHelper.queryPersonToCustId(orderForUI.custId)
            orderForUI.custName = if (person != null) person.personName else "未知"
        }
        orderMealVM.getUserName().value = orderForUI.custName
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            orderMealVM.getUserName().value = ""
            if (findNavController().previousBackStackEntry != null) findNavController().popBackStack()
        }
        binding.btnConfirm.setOnClickListener {
            findNavController().navigate(R.id.userOrder_to_orderSettle)
        }
    }

}