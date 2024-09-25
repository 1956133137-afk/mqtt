package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
 * Date: 2024/9/23 18:28
 **/
class DownloadVM : ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv = MMKV.defaultMMKV()
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()
    private val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    private val mealList: MutableList<OrderMeal> = mutableListOf()
    private val dishList: MutableList<DishBean> = mutableListOf()

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
    }

    fun synOrderMeal(ccbToken: String, boolean: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            if (payCfg.campusId.isEmpty() || payCfg.businessId.isEmpty()) return@launch
            boolean(false)
            mealList.clear()
            val orderMeal = mRepository.queryOrderMeal(ccbToken, payCfg.campusId, payCfg.businessId)
            LogUtil.d(TAG, Gson().toJson(orderMeal))
            if (orderMeal.code == "200") {
                val orderMealList = orderMeal.data?.orderMealList
                val delFlag = orderMeal.data?.delFlag ?: "1"
                if (orderMealList != null && orderMealList.size > 0 && delFlag != "2") mealList.addAll(orderMealList)
            }
            boolean(true)
        }
    }

    fun synOrderDish(ccbToken: String, date: String, mealId: String, res: (Boolean, MutableList<DishBean>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            if (payCfg.businessId.isEmpty()) return@launch
            dishList.clear()
            res(false, dishList)
            val orderDish = mRepository.queryOrderDish(ccbToken, date, mealId, payCfg.businessId)
            if (orderDish.code == "200") {
                val orderDishList = orderDish.data?.batchDishes
                if (orderDishList != null && orderDishList.size > 0) {
                    orderDishList.forEach {
                        if (it.status == "1") {
                            val bean = DishBean().apply {
                                dishId = it.dishesId
                                dishName = it.dishesName
                                dishPrice = it.price
                                dishUnit = it.unit
                                imgUrl = it.imgUrl
                            }
                            dishList.add(bean)
                        }
                    }
                }
            }
            res(true, dishList)
        }
    }

    fun getDateWeek(): MutableList<SelectDateBean> {
        val beanList = mutableListOf<SelectDateBean>()
        for (i in 0..6) {
            val millis = System.currentTimeMillis() + i * 86400000
            val dateFormat = TimeUtil.timeFormat("yyyy-MM-dd", millis)
            val dateBean = SelectDateBean()
            dateBean.date = dateFormat
            dateBean.value = getWeekDay(dateFormat)
            dateBean.mealList = getDayMeal(dateBean.value)
            beanList.add(dateBean)
        }
        return beanList
    }

    private fun getDayMeal(value: String): MutableList<OrderMeal> {
        val orderMeal: MutableList<OrderMeal> = mutableListOf()
        mealList.forEach {
            val split = it.orderMealDay.split(", ".toRegex())
            if (split.contains(value) && it.delFlag != "1") orderMeal.add(it)
        }
        return orderMeal
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
}