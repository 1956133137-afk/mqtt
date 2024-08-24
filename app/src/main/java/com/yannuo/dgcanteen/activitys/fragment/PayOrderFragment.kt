package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.adapters.PayOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentPayOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.util.LogUtil

class PayOrderFragment : BaseFragment<FragmentPayOrderBinding>() {

    private val payOrderAdapter by lazy { PayOrderAdapter(requireContext()) }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentPayOrderBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = payOrderAdapter
        payOrderAdapter.data = DishesDBHelper.getInstance().queryPayOrder("", "")
        initEvent()
    }

    private fun initEvent() {
        binding.queryOrder.setOnClickListener {
            val username = binding.inputUserName.text.toString().trim().replace(" ", "")
            val orderId = binding.inputOrderId.text.toString().trim().replace(" ", "")
            payOrderAdapter.data = DishesDBHelper.getInstance().queryPayOrder(username, orderId)
        }
    }
}