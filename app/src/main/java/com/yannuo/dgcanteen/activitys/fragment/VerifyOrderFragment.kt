package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.VerifyOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentVerifyOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.util.TimeUtil

class VerifyOrderFragment : BaseFragment<FragmentVerifyOrderBinding>() {
    private val currentDate = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
    private val verifyAdapter by lazy { VerifyOrderAdapter() }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentVerifyOrderBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = verifyAdapter
        val verifyList = DishesDBHelper.getInstance().queryVerifyUserToAll(currentDate, "")
        if (verifyList != null) {
            binding.verifyTotalCount.text = "${verifyList.size}"
            verifyAdapter.data = verifyList
        } else {
            binding.verifyTotalCount.text = "0"
            verifyAdapter.clear()
        }
        initEvent()
    }

    private fun initEvent() {
        binding.queryOrder.setOnClickListener {
            val username = binding.inputUserName.text.toString().trim().replace(" ", "")
            verifyAdapter.data = DishesDBHelper.getInstance().queryVerifyUserToAll(currentDate, username)
        }
    }
}