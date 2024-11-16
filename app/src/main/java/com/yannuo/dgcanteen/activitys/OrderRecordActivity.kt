package com.yannuo.dgcanteen.activitys

import android.os.Handler
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.viewModel.OrderRecordVM
import com.yannuo.dgcanteen.adapters.OrderRecordAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityOrderRecordBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.util.LogUtil

class OrderRecordActivity : BaseActivity<ActivityOrderRecordBinding>() {
    private val orderRecordVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[OrderRecordVM::class.java] }
    private val orderRecordAdapter by lazy { OrderRecordAdapter(this) }
    private val handler: Handler = Handler(MyApplication.applicationContext.mainLooper)
    private var awaitingDialog: AwaitingDialog? = null
    private var confirmDialog: ConfirmDialog? = null

    override fun bindLayout() {
        binding = ActivityOrderRecordBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        orderRecordVM.setUserId(intent.extras?.getString("ccbToken") ?: "", intent.extras?.getString("custId") ?: "")
        initObject()
        initEvent()
    }

    private fun initObject() {
        binding.orderListView.layoutManager = GridLayoutManager(this, 3)
        binding.orderListView.adapter = orderRecordAdapter

        orderRecordVM.queryOrderList(1, 100) { type, orderList ->
            handler.post {
//                LogUtil.d(TAG, Gson().toJson(orderList))
                orderRecordAdapter.data = orderList
            }
        }

        orderRecordAdapter.setItemListener(object : OrderRecordAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                showConfirmDialog(position)
            }
        })
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun showConfirmDialog(position: Int) {
        if (confirmDialog == null) confirmDialog = ConfirmDialog(this)
        confirmDialog?.show()
        confirmDialog?.setTextMsg("您是否确认退款?")
        confirmDialog?.setListener(object : ConfirmDialog.OnConfirmCallback {
            override fun confirmCallback(flag: Boolean) {
                if (flag) {
                    showAwaitDialog("退款中•••")
                    orderRecordVM.orderRefund(orderRecordAdapter.data[position]) {
                        handler.post {
                            awaitingDialog?.dismiss()
                            if (it) orderRecordAdapter.removeData(position)
                        }
                    }
                }
            }
        })
    }

    private fun showAwaitDialog(str: String) {
        if (awaitingDialog == null) awaitingDialog = AwaitingDialog(this)
        if (awaitingDialog?.isShowing == false) awaitingDialog?.show()
        awaitingDialog?.updateText(str)
    }

    override fun onDestroy() {
        orderRecordAdapter.release()
        awaitingDialog?.cancel()
        confirmDialog?.cancel()
        super.onDestroy()
    }
}