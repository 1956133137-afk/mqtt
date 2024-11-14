package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.adapters.PayOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentPayOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil

class PayOrderFragment : BaseFragment<FragmentPayOrderBinding>() {

    private val payOrderAdapter by lazy { PayOrderAdapter(requireContext()) }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentPayOrderBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = payOrderAdapter
        val payOrderList = DishesDBHelper.getInstance().queryPayOrder("", "")
        payOrderAdapter.data = payOrderList
        initData(payOrderList)
        initEvent()
    }

    private fun initData(payOrderList: MutableList<PayOrderTable>) {
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        var tranCount = 0
        var tranAmount = 0.0
        var uploadCount = 0
        var notCount = 0
        payOrderList.forEach {
            tranCount++
            tranAmount += it.actualPayment.ifEmpty { it.payment }.toDouble()
            if (it.flag == 1) uploadCount++ else notCount++
        }
        binding.dateMsg.text = "$date\n消费汇总统计"
        binding.tranCount.text = "$tranCount"
        binding.tranAmount.text = String.format("%.02f", tranAmount)
        binding.alreadyUploadCount.text = "$uploadCount"
        binding.notUploadCount.text = "$notCount"
    }

    private fun initEvent() {
        binding.queryOrder.setOnClickListener {
            val username = binding.inputUserName.text.toString().trim().replace(" ", "")
            val orderId = binding.inputOrderId.text.toString().trim().replace(" ", "")
            payOrderAdapter.data = DishesDBHelper.getInstance().queryPayOrder(username, orderId)
        }
    }
}