package com.yannuo.dgcanteen.activitys.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

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
    private val DAY_TIME: Long = 86400000L

    private var currentCcbToken: String = ""
    private var currentCustId: String = ""
    private var payCfg = PayCfg()
    private val mOrderList: MutableList<Order> = mutableListOf()
    private val orderList: MutableLiveData<MutableList<Order>> = MutableLiveData<MutableList<Order>>()
    private val windowList: MutableList<WindowBean> = mutableListOf()

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        listener?.onOrder(0, "")
    }

    fun getOrderList(): MutableLiveData<MutableList<Order>> = orderList

    fun getWindowList(): MutableList<WindowBean> = windowList

    fun setListener(listener: OnOrderListener?) {
        this.listener = listener
    }

    fun setUserId(ccbToken: String, custId: String) {
        mOrderList.clear()
        currentCcbToken = ccbToken
        currentCustId = custId
        payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
        synWindowList()
    }

    private fun synWindowList() {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            /*获取窗口信息*/
            windowList.clear()
            val windowReceive = mRepository.getWindowList(currentCcbToken, payCfg.businessId, payCfg.campusId)
            if (windowReceive.code == "200") {
                val type = object : TypeToken<MutableList<WindowBean>>() {}.type
                val list = Gson().fromJson<MutableList<WindowBean>>(Gson().toJson(windowReceive.data), type)
                LogUtil.d(TAG, Gson().toJson(list))
                windowList.addAll(list)
            }
        }
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
                Log.d(TAG, "queryOrderList: 订餐列表查询：${Gson().toJson(receive.list)}")
                mOrderList.addAll(receive.list)
                if (page >= receive.totalPage.toInt() || page * pageSize >= receive.totalRecord.toInt()) {
                    mOrderList.sortBy { it.mealDate }
                    callback(1, mOrderList)
                } else queryOrderList(page + 1, pageSize) { type, orderList -> callback(1, orderList) }
            } else {
                mOrderList.sortBy { it.mealDate }
                callback(3, mOrderList)
            }
        }
    }

    fun getDateWeek(): MutableList<SelectDateBean> {
        val beanList = mutableListOf<SelectDateBean>()
        beanList.add(SelectDateBean("-1"))
        for (i in 0..kv.decodeInt(Constant.ORDER_ADVANCE_DAY, 6)) {
            val millis = System.currentTimeMillis() + i * DAY_TIME
            val dateFormat = TimeUtil.timeFormat("yyyy-MM-dd", millis)
            beanList.add(SelectDateBean(dateFormat, getWeekDay(dateFormat)))
        }
        return beanList
    }

    private fun getWeekDay(date: String): String {
        if (!isValidDate(date)) return ""
        val split = date.split("-".toRegex())
        val calendar = Calendar.getInstance()
        calendar.set(split[0].toInt(), split[1].toInt() - 1, split[2].toInt())
        return when (calendar[Calendar.DAY_OF_WEEK]) {
            Calendar.SUNDAY -> "7"
            Calendar.MONDAY -> "1"
            Calendar.TUESDAY -> "2"
            Calendar.WEDNESDAY -> "3"
            Calendar.THURSDAY -> "4"
            Calendar.FRIDAY -> "5"
            Calendar.SATURDAY -> "6"
            else -> "0"
        }
    }

    private fun isValidDate(dateFormat: String): Boolean {
        // yyyy-MM-dd
        val regex = Regex("""^\d{4}-\d{2}-\d{2}$""", RegexOption.IGNORE_CASE)
        return regex.matches(dateFormat)
    }

    fun getOrderDate(mealDate: String) {
        if (mealDate != "-1") {
            val orders = mOrderList.filter { it.mealDate == mealDate }.toMutableList()
            orders.sortBy { it.mealId }
            orderList.value = orders
        } else orderList.value = mOrderList
    }

    fun orderRefund(order: Order, result: (Boolean,String) -> Unit) {
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
                LogUtil.d(TAG, Gson().toJson(refundBean))
                val refundRes = mRepository.orderDirectRefund(currentCcbToken, refundBean)
                LogUtil.d(TAG, Gson().toJson(refundRes))
                if(refundRes.code == "200") result(true,"")
                else result(false,refundRes.msg)
            } else result(false,"退款金额超过支付金额")
        }
    }

    fun DCRefundIsOverTime(order: Order, result: (String) -> Unit){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, payCfg::class.java)
            if(payCfg == null) {
                LogUtil.e(TAG,"未获取到支付信息，退款失败")
                result("-1")
                return@launch
            }
            //获取商家配置
            val businessConfig = mRepository.getBusinessConfig(currentCcbToken, payCfg.campusId, payCfg.businessId)
            //获取自定义退款信息
            val queryOrderMeal = mRepository.queryOrderMeal(
                currentCcbToken,
                payCfg.campusId,
                payCfg.businessId
            )
            var customizeRefund = ""
            queryOrderMeal.data?.orderMealList?.forEach {
                if(it.mealId == order.mealId) customizeRefund = it.customizeRefund
            }
            if(customizeRefund.isEmpty()){
                LogUtil.e(TAG,"未获取到自定义退款信息，退款失败")
                result("-1")
                return@launch
            }
            LogUtil.d(TAG, "商家配置：${Gson().toJson(businessConfig)}")
            val busCigBean = Gson().fromJson(Gson().toJson(businessConfig.data), DCRefundIsOverTimeBean::class.java)
            val bean = DCRefundIsOverTimeBean().apply {
                campusId = payCfg.campusId
                businessId = payCfg.businessId
                isNeedRefundDate = busCigBean.isNeedRefundDate
                limitRefundDay = busCigBean.limitRefundDay
                limitRefundTime = busCigBean.limitRefundTime
                mealDate = order.mealDate
                mealId = order.mealId.toInt()
                customizeRefundType = customizeRefund
            }
            //判断
            val refundRes = mRepository.DCRefundIsOverTime(currentCcbToken,bean)
            result(refundRes.code)
        }
    }

    interface OnOrderListener {
        fun onOrder(type: Int, data: Any)
    }
}