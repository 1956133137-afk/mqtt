package com.yannuo.dgcanteen.activitys

import android.os.CountDownTimer
import android.os.Handler
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.OrderRecordVM
import com.yannuo.dgcanteen.adapters.OrderRecordAdapter
import com.yannuo.dgcanteen.adapters.SelectDateAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityOrderRecordBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.model.SelectDateBean
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.concurrent.TimeUnit

class OrderRecordActivity : BaseActivity<ActivityOrderRecordBinding>() {
    private val mmkv = MMKV.defaultMMKV()
    private val orderRecordVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[OrderRecordVM::class.java] }
    private val orderRecordAdapter by lazy { OrderRecordAdapter(this) }
    private val handler: Handler = Handler(MyApplication.applicationContext.mainLooper)
    private var awaitingDialog: AwaitingDialog? = null
    private var confirmDialog: ConfirmDialog? = null

    //    private var windowStr = StringBuilder()
    private val selectDateAdapter by lazy { SelectDateAdapter() }

    private var countDown: CountDownTimer? = null

    override fun bindLayout() {
        binding = ActivityOrderRecordBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        orderRecordVM.setUserId(intent.extras?.getString("ccbToken") ?: "", intent.extras?.getString("custId") ?: "")
        initObject()
        initEvent()
    }

    private fun initObject() {
        /*显示日期*/
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.dateWeekView.layoutManager = layoutManager
        binding.dateWeekView.adapter = selectDateAdapter
        selectDateAdapter.data = orderRecordVM.getDateWeek()
        selectDateAdapter.setDateListener(object : SelectDateAdapter.SelectDateListener {
            override fun onSelectDate(bean: SelectDateBean) {
                LogUtil.d(TAG, Gson().toJson(bean))
                orderRecordVM.getOrderDate(bean.date)
            }
        })
        /*显示订餐记录*/
        binding.orderListView.layoutManager = GridLayoutManager(this, 3)
        binding.orderListView.adapter = orderRecordAdapter
        orderRecordAdapter.setItemListener(orderRecordVM.getWindowList(), object : OrderRecordAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                showConfirmDialog(position)
            }

//            override fun onItemWindow(windowList: MutableList<String>) {
//                handler.post {
//                    windowStr.clear()
//                    windowStr.append("核销窗口:\n")
//                    windowList.forEach { windowId ->
//                        orderRecordVM.getWindowList().forEach { if (windowId == it.windowId) windowStr.append("${it.windowName}，") }
//                    }
//                    if (windowStr.isNotEmpty()) windowStr.deleteCharAt(windowStr.length - 1)
//                    binding.mvControl.text = windowStr.toString()
//                }
//            }
        })
        /*选择日志显示的记录*/
        orderRecordVM.getOrderList().observe(this) { orderRecordAdapter.data = it }

        orderRecordVM.setListener(object : OrderRecordVM.OnOrderListener {
            override fun onOrder(type: Int, data: Any) {
                handler.post {
                    if (type == 0) awaitingDialog?.dismiss()
                }
            }
        })

        synOrderRecord()
        if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) onCountDownTimer(binding.btnBack, 30L)
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
                    orderRecordVM.DCRefundIsOverTime(orderRecordAdapter.data[position]){
                        if(!it){
                            handler.post {
                                awaitingDialog?.dismiss()
                                ToastShowUtil.show("超过了退款时间")
                            }
                            return@DCRefundIsOverTime
                        }else{
                            orderRecordVM.orderRefund(orderRecordAdapter.data[position]) { type,str ->
                                handler.post {
                                    awaitingDialog?.dismiss()
                                    if (type) orderRecordAdapter.removeData(position)
                                    ToastShowUtil.show(if (type) "退餐成功" else str)
                                }
                            }
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

    private fun onCountDownTimer(btnBack: Button?, time: Long) {
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time), 1000) {
            override fun onTick(mil: Long) {
                btnBack?.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
                finish()
            }
        }
        countDown?.start()
    }

    override fun onDestroy() {
//        binding.mvControl.stopAnima()
        orderRecordAdapter.release()
        awaitingDialog?.cancel()
        confirmDialog?.cancel()
        countDown?.cancel()
        super.onDestroy()
    }
}