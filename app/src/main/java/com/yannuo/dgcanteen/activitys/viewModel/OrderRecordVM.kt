package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/15 10:16
 **/
class OrderRecordVM : ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv = MMKV.defaultMMKV()
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()
    private var listener: OnOrderListener? = null

    private var currentCcbToken: String = ""
    private var currentCustId: String = ""
    private var payCfg = PayCfg()
    private val orderList: MutableList<Order> = mutableListOf()

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        listener?.onOrder(0, "")
    }

    fun setListener(listener: OnOrderListener?) {
        this.listener = listener
    }

    fun setUserId(ccbToken: String, custId: String) {
        orderList.clear()
        currentCcbToken = ccbToken
        currentCustId = custId
        payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    }

    fun queryOrderList(page: Int, pageSize: Int, callback: (Int, MutableList<Order>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val bean = OrderListBean().apply {
                campusId = payCfg.campusId
                custId = currentCustId
                this.page = page.toString()
                this.pageSize = pageSize.toString()
                batchTranResult.addAll(mutableListOf("3", "7"))
                batchOrderStatus.addAll(mutableListOf("4", "7", "10"))
            }
            LogUtil.d(TAG, Gson().toJson(bean))
            val response = mRepository.getOrderList(currentCcbToken, bean)
            if (response.code == "200") {
                val receive = Gson().fromJson(response.data.toString(), OrderListReceive::class.java)
                orderList.addAll(receive.list)
                if (page >= receive.totalPage.toInt() || page * pageSize >= receive.totalRecord.toInt()) callback(1, orderList)
                else queryOrderList(page + 1, pageSize) { type, orderList -> callback(1, orderList) }
            } else callback(3, orderList)
        }
    }

    fun orderRefund(order: Order, result: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val refundBean = OrderRefundBean().apply {
                campusId = payCfg.campusId
                businessId = payCfg.businessId
                custId = currentCustId
                pOrderId = order.pOrderId ?: ""
                orderId = order.orderId
            }
            var refundMoney = -1.0
            if (order.actualPayment.isNotEmpty() && order.refundPayment.isNotEmpty()) {
                refundMoney = order.actualPayment.toDouble() - order.refundPayment.toDouble()
            }
            if (refundMoney >= 0) {
                refundBean.money = String.format("%.02f", refundMoney)
                val refundRes = mRepository.orderDirectRefund(currentCcbToken, refundBean)
                result(refundRes.code == "200")
            } else result(false)
        }
    }

    interface OnOrderListener {
        fun onOrder(type: Int, data: Any)
    }
}