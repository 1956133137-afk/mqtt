package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.adapters.OfflineOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentOfflineOrderBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class OfflineOrderFragment : BaseFragment<FragmentOfflineOrderBinding>() {
    private val offlineOrderAdapter by lazy { OfflineOrderAdapter(requireContext()) }
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
        binding.btnUpdateOrder.setOnClickListener {
            if (System.currentTimeMillis() - currentTime < 1000) return@setOnClickListener
            currentTime = System.currentTimeMillis()
            updateOffLineOrder()
        }
    }

    private fun updateOffLineOrder() {
        if (isUpdateStatus) return
        lifecycleScope.launch(Dispatchers.IO + mHandler) {
            if (NetworkStateManager.getInstance().isOnline(requireContext()) && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) { //有网并且不为离线状态
                mutex.withLock { isUpdateStatus = true }
                withContext(Dispatchers.Main) { if (!awaitingDialog.isShowing) awaitingDialog.show() }
                LogUtil.i(TAG, "手动离线补扣开始...")
                var updateFailureCount = 0
                val offlineOrder = DishesDBHelper.getInstance().queryOfflineOrderToAll()
                offlineOrder?.forEachIndexed { index, offline ->
                    val payForUI = Gson().fromJson(Gson().toJson(offline), PayForUI::class.java)
                    payForUI.payTime = TimeUtil.dateFormat(offline.signTime)
                    payForUI.payDate = TimeUtil.formatDate(offline.signTime)
                    offline.paymentDishes.forEach { payForUI.paymentDishes.add(Gson().fromJson(Gson().toJson(it), Dish::class.java)) }
                    val response = when (payForUI.payType) {
                        "2" -> {
                            val request = Gson().fromJson(Gson().toJson(payForUI), CodePayBean::class.java)
                            request.qrCode = payForUI.payContent
                            val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                            payRepositoryOfPay.payByQrCode(encryption)
                        }
                        "3" -> {
                            val request = Gson().fromJson(Gson().toJson(payForUI), CardPayBean::class.java)
                            request.cardId = payForUI.payContent
                            val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                            payRepositoryOfPay.payByIcCard(encryption)
                        }
                        else -> CanteenResponse<String>()
                    }
                    if (response.code == "200") {
                        val decryptStr = DES3CBCUtil.decryptRSA(response.data ?: "")
                        val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                        LogUtil.d(TAG, Gson().toJson(result))
                        payForUI.result = result.RESULT
                        payForUI.accType = result.ACC_TYPE
                        payForUI.accNo = result.ACC_NO
                        payForUI.accBal = result.ACC_BAL
                        result.ACC_LIST.forEach {
                            val acclist = ACCLIST().apply {
                                ACC_NO = it.ACC_NO
                                ACC_BAL = it.ACC_BAL
                                ACC_TYPE = it.ACC_TYPE
                                TRAN_ID = it.TRAN_ID
                                PAYMENT = it.PAYMENT
                            }
                            payForUI.accList.add(acclist)
                        }
//                        payForUI.accList = result.ACC_LIST
                        payForUI.actualPayment = result.ACTUAL_PAYMENT
                        payForUI.orderId = result.ORDERID
                        payForUI.traceId = result.TRACEID
                        payForUI.errCode = result.ERRCODE
                        payForUI.errMsg = result.ERRMSG
                    }
                    if (payForUI.result == "Y") {
                        offline.flag = 1
                        DishesDBHelper.getInstance().updateOfflineOrder(offline)
                        saveOrderRecord(payForUI, offline.sessionId)
                    } else updateFailureCount++
                    withContext(Dispatchers.Main) { if (awaitingDialog.isShowing) awaitingDialog.updateText("上传进度: 成功${index + 1 - updateFailureCount}, 失败$updateFailureCount") }
                }
                LogUtil.i(TAG, "手动离线补扣结束...")
                withContext(Dispatchers.Main) {
                    offlineOrderAdapter.data = DishesDBHelper.getInstance().queryOfflineOrderByUsername("")
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

    private fun saveOrderRecord(payForUI: PayForUI, session: String) {
        val order = DishesDBHelper.getInstance().queryPayOrder(session) ?: return
//        order.accNo = payForUI.accNo
//        order.accBal = payForUI.accBal
        order.accType = payForUI.accType
//        order.accList = payForUI.accList
        order.orderId = payForUI.orderId
        order.traceId = payForUI.traceId
        order.actualPayment = payForUI.actualPayment
        order.flag = 1
        DishesDBHelper.getInstance().updatePayOrderById(order)
    }

    override fun onDestroy() {
        super.onDestroy()
        awaitingDialog.cancel()
    }
}