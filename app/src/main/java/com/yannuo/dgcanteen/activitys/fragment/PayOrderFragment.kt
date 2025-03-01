package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.adapters.PayOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentPayOrderBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.model.ACCLIST
import com.yannuo.dgcanteen.model.Dish
import com.yannuo.dgcanteen.model.SynConsumeRecordBean
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PayOrderFragment : BaseFragment<FragmentPayOrderBinding>() {
    private val payOrderAdapter by lazy { PayOrderAdapter(requireContext()) }
    private val awaitingDialog by lazy { AwaitingDialog(requireContext()) }
    private val payRepositoryOfPay by lazy { PayRepositoryOfPay() }
    private var currentTime = 0L

    // 放在重复调用
    private val mutex = Mutex()
    private var isUpdateStatus = false

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        if (awaitingDialog.isShowing) awaitingDialog.dismiss()
        isUpdateStatus = false
    }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentPayOrderBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = payOrderAdapter
        initData(DishesDBHelper.getInstance().queryPayOrder("", ""))
        initEvent()
    }

    private fun initData(payOrderList: MutableList<PayOrderTable>?) {
        if (payOrderList == null) payOrderAdapter.clear() else payOrderAdapter.data = payOrderList
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        var tranCount = 0
        var tranAmount = 0.0
        var uploadCount = 0
        var notCount = 0
        payOrderList?.forEach {
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
        binding.btnUpdateOrder.setOnClickListener {
            if (System.currentTimeMillis() - currentTime < 1000) return@setOnClickListener
            currentTime = System.currentTimeMillis()
            synFacePayOrder()
        }
    }

    private fun synFacePayOrder() {
        if (isUpdateStatus) return
        lifecycleScope.launch(Dispatchers.IO + mHandler) {
            if (NetworkStateManager.getInstance().isOnline(requireContext()) && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) { //有网并且不为离线状态
                mutex.withLock { isUpdateStatus = true }
                withContext(Dispatchers.Main) { if (!awaitingDialog.isShowing) awaitingDialog.show() }
                LogUtil.i(TAG, "手动上传刷脸开始...")
                var updateFailureCount = 0
                val payOrderToAll = DishesDBHelper.getInstance().queryPayOrderToAll()
                payOrderToAll?.forEachIndexed { index, order ->
                    val bean = SynConsumeRecordBean().apply {
                        deviceSerialNumber = order.deviceId
                        businessId = order.businessId
                        campusId = order.campusId
                        counterId = order.vposId
                        consumptionType = order.payType
                        RESULT = order.result
                        CUST_ID = order.custId
                        PAYMENT = order.payment
                        ACTUAL_PAYMENT = order.actualPayment ?: "0.0"
                        ACC_NO = order.accNo
                        ACC_BAL = order.accBal
                        ACC_TYPE = order.accType
                        TRACEID = order.traceId
                        ORDER_ID = order.orderId
                        TRAN_RESULT = order.tranResult
                        OFFLINE = order.offline
                        ERRCODE = order.errCode
                        ERRMSG = order.errMsg
                        order.accList.forEach {
                            val acclist = ACCLIST().apply {
                                ACC_NO = it.acC_NO
                                ACC_BAL = it.acC_BAL
                                ACC_TYPE = it.acC_TYPE
                                TRAN_ID = it.traN_ID
                                PAYMENT = it.payment
                            }
                            ACC_LIST.add(acclist)
                        }
                        PAYTIME = order.payTime
                        BUSINESS_NAME = order.businessName
                    }
                    order.paymentDishesList.forEach { dish ->
                        bean.paymentDishesList.add(Gson().fromJson(Gson().toJson(dish), Dish::class.java))
                    }
                    order.accList.forEach {
                        bean.ACC_LIST.add(Gson().fromJson(Gson().toJson(it), ACCLIST::class.java))
                    }
                    val res = payRepositoryOfPay.synCsRecord(bean)
                    if (res.code == "200") {
                        order.flag = 1
                        DishesDBHelper.getInstance().updatePayOrder(order)
                        LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
                    } else {
                        updateFailureCount++
                        LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                    }
                    withContext(Dispatchers.Main) { if (awaitingDialog.isShowing) awaitingDialog.updateText("上传进度: 成功${index + 1 - updateFailureCount}, 失败$updateFailureCount") }
                }
                LogUtil.i(TAG, "手动上传刷脸结束...")
                withContext(Dispatchers.Main) {
                    initData(DishesDBHelper.getInstance().queryPayOrder("", ""))
                    if (awaitingDialog.isShowing) awaitingDialog.dismiss()
                }
                mutex.withLock { isUpdateStatus = false }
            } else {
                withContext(Dispatchers.Main) {
                    ToastShowUtil.show("设备未联网或开启离线模式！")
                    LogUtil.d(TAG, "设备未联网或开启离线模式！")
                }
            }
        }
    }
}