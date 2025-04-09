package com.yannuo.dgcanteen.activitys.viewModel

import androidx.annotation.Nullable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.MealBean
import com.yannuo.dgcanteen.model.MealPreparationBean
import com.yannuo.dgcanteen.model.MealRequestPerson
import com.yannuo.dgcanteen.model.Order
import com.yannuo.dgcanteen.model.OrderListReceive
import com.yannuo.dgcanteen.model.OrderStatusBean
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MealPreparationVM : ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv = MMKV.defaultMMKV()
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()
    private var listener: OnMealListener? = null
    private var mealListener: OnMealListener? = null
    private var payCfg = PayCfg()
    private val orderList: MutableList<Order> = mutableListOf()
    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
        listener?.onMeal(0,"")
    }

    fun setUserId() {
        orderList.clear()
        payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    }

    fun setListener(listener: OnMealListener?) {
        this.listener = listener
    }

    fun setMealListener(mealListener: OnMealListener?) {
        this.mealListener = mealListener
    }

    //修改订单状态
    fun modifyTheOrderStatus(bean: OrderStatusBean) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val response = mRepository.ModifyTheOrderStatus(bean)
            if (response.code == "200") {
                mealListener?.onMeal(1,"")
                mealListener = null
            }
        }
    }

    fun queryAllMeal(callback: (HashMap<String, String>) -> Unit){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            var bean = MealRequestPerson().apply {
                this.campusId = payCfg.campusId
                this.businessId = payCfg.businessId
            }
            val response = mRepository.queryAllMeal(bean)
            if (response.code == "200") {
                val dataMap = HashMap<String, String>()
                for(meal : MealBean in response.data!!){
                    dataMap[meal.mealId.toString()] = meal.mealName
                }
                callback(dataMap)
            }
        }
    }

    //查询订单
    fun queryMealList(
        page: Int,
        @Nullable personName: String? = null,
        @Nullable spinnerText: Int? = null,
        dateList: List<String>,
        @Nullable orderStatus: String? = null,
        callback: (Int, MutableList<Order>) -> Unit
    ){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            var bean = MealPreparationBean().apply {
                this.page = page
                businessId = payCfg.businessId
                campusId = payCfg.campusId
                this.personName = personName
                this.mealId = spinnerText
                this.dateList = dateList
                this.orderStatus = orderStatus
            }
            val response = mRepository.getMealPreparaionList(bean)
            if (response.code == "200") {
                val receive = Gson().fromJson(response.data.toString(), OrderListReceive::class.java)
                orderList.addAll(receive.list)
                if(receive.totalRecord.toInt() > orderList.size) queryMealList(page + 1,personName,spinnerText,dateList,orderStatus,callback)
                else callback(3, orderList)
                listener?.onMeal(0,"")
            }
        }
    }

    interface OnMealListener {
        fun onMeal(type: Int, data: Any)
    }
}