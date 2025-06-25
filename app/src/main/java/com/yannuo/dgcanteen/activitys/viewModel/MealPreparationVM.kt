package com.yannuo.dgcanteen.activitys.viewModel

import androidx.annotation.Nullable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.InfoBean
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
    private val infoBeanList: MutableList<InfoBean> = mutableListOf()
    private val orderList: MutableList<Order> = mutableListOf()
    val orderDishCount: MutableLiveData<MutableList<InfoBean>> = MutableLiveData<MutableList<InfoBean>>(mutableListOf())
    private val mHandler = CoroutineExceptionHandler { _, throwable ->
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
            }else{
                mealListener?.onMeal(-1,response.msg)
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

    fun closeList(){
        infoBeanList.clear()
    }
    
    //查询订单
    fun queryMealList(
        type: Boolean,
        page: Int,
        @Nullable personName: String? = null,
        @Nullable spinnerText: Int? = null,
        dateList: List<String>,
        @Nullable orderStatus: String? = null,
        callback: (Int, MutableList<Order>, String) -> Unit
    ){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val bean = MealPreparationBean().apply {
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
                if(receive.totalRecord.toInt() > orderList.size) {
                    queryMealList(
                        type,
                        page + 1,
                        personName,
                        spinnerText,
                        dateList,
                        orderStatus,
                        callback
                    )
                } else {
                    callback(3, orderList, response.msg)
                    if(type){
                        dishCount(orderList)
                    }
                    orderDishCount.postValue(infoBeanList)
                    listener?.onMeal(0,"")
                }
            }else{
                callback(-1,mutableListOf(),response.msg)
            }
        }
    }

    fun dishCount(list: MutableList<Order>){
        list.forEach {
            it.dcOrderDishesList.forEach { dc ->
                var typr = false
                infoBeanList.forEach { order ->
                    if(order.describe == dc.dishesName){
                        typr = true
                        order.content = "${order.content.toInt() + dc.dishesNum.toInt()}"
                    }
                }
                if(!typr){
                    infoBeanList.add(InfoBean(dc.dishesName,dc.dishesNum))
                }
            }
        }
    }

    fun setInfoBeanList(list: MutableList<Order>){
        infoBeanList.clear()
        dishCount(list)
        orderDishCount.postValue(infoBeanList)
    }

    interface OnMealListener {
        fun onMeal(type: Int, data: Any)
    }
}