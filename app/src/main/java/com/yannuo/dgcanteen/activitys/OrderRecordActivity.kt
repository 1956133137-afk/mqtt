package com.yannuo.dgcanteen.activitys

import android.os.Handler
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.viewModel.OrderRecordVM
import com.yannuo.dgcanteen.adapters.OrderRecordAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityOrderRecordBinding
import com.yannuo.dgcanteen.util.LogUtil

class OrderRecordActivity : BaseActivity<ActivityOrderRecordBinding>() {
    private val orderRecordVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[OrderRecordVM::class.java] }
    private val orderRecordAdapter by lazy { OrderRecordAdapter(this) }
    private val handler: Handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout() {
        binding = ActivityOrderRecordBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        orderRecordVM.setUserId(intent.extras?.getString("ccbToken") ?: "", intent.extras?.getString("custId") ?: "")

        binding.orderListView.layoutManager = GridLayoutManager(this, 3)
        binding.orderListView.adapter = orderRecordAdapter

        orderRecordVM.queryOrderList(1, 100) { type, orderList ->
            handler.post {
                LogUtil.d(TAG, Gson().toJson(orderList))
                orderRecordAdapter.data = orderList
            }
        }

        binding.btnBack.setOnClickListener { finish() }
    }
}