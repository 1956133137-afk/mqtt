package com.yannuo.dgcanteen.activitys.fragment.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.databinding.FragmentOrderSettleBinding

class OrderSettleFragment : BaseFragment<FragmentOrderSettleBinding>() {
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOrderSettleBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        binding.btnBack.setOnClickListener { if (findNavController().previousBackStackEntry != null) findNavController().popBackStack() }
        binding.btnCancel.setOnClickListener {
            orderMealVM.getUserName().value = ""
            findNavController().navigate(R.id.orderSettle_to_verifyUser)
        }
    }

}