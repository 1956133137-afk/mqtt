package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.OfflineOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentOfflineOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper

class OfflineOrderFragment : BaseFragment<FragmentOfflineOrderBinding>() {
    private val offlineOrderAdapter by lazy { OfflineOrderAdapter(requireContext()) }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOfflineOrderBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = offlineOrderAdapter
        offlineOrderAdapter.data = DishesDBHelper.getInstance().queryOfflineOrderByUsername("")
        initEvent()
    }

    private fun initEvent() {
        binding.queryOrder.setOnClickListener {
            val username = binding.inputUserName.text.toString().trim().replace(" ", "")
            offlineOrderAdapter.data = DishesDBHelper.getInstance().queryOfflineOrderByUsername(username)
        }
    }
}