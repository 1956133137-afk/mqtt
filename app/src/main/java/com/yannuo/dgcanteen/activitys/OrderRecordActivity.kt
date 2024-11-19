package com.yannuo.dgcanteen.activitys

import android.os.Handler
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.yannuo.dgcanteen.activitys.viewModel.OrderRecordVM
import com.yannuo.dgcanteen.adapters.OrderRecordAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityOrderRecordBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.util.ToastShowUtil

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
        orderRecordVM.setListener(object : OrderRecordVM.OnOrderListener {
            override fun onOrder(type: Int, data: Any) {
                handler.post {
                    if (type == 0) awaitingDialog?.dismiss()
                }
            }
        })
        binding.orderListView.layoutManager = GridLayoutManager(this, 3)
        binding.orderListView.adapter = orderRecordAdapter
        orderRecordAdapter.setItemListener(object : OrderRecordAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                showConfirmDialog(position)
            }
        })
        synOrderRecord()
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun synOrderRecord() {
        showAwaitDialog("同步订餐记录中•••")
        orderRecordVM.queryOrderList(1, 100) { type, orderList ->
            handler.post {
                awaitingDialog?.dismiss()
                orderRecordAdapter.data = orderList
            }
        }
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
                            ToastShowUtil.show(if (it) "退餐成功" else "退餐失败")
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