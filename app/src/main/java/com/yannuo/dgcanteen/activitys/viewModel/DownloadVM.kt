package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.OrderMeal
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.SelectDateBean
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

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
    }

    fun synOrderMeal(ccbToken: String, mealList: (MutableList<OrderMeal>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            if (payCfg.campusId.isEmpty() || payCfg.businessId.isEmpty()) {

                return@launch
            }
            val response = mRepository.queryOrderMeal(ccbToken, payCfg.campusId, payCfg.businessId)
            LogUtil.d(TAG, Gson().toJson(response))
            var resMealList: MutableList<OrderMeal> = mutableListOf()
            if (response.code == "200") {
                val list = response.data?.orderMealList
                if (list != null && list.size > 0) resMealList = list
            }
            mealList(resMealList)
        }
    }

    fun synOrderDish(ccbToken: String, date: String, mealId: String) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            if (payCfg.businessId.isEmpty()) {

                return@launch
            }
            val orderDish = mRepository.queryOrderDish(ccbToken, date, mealId, payCfg.businessId)
            LogUtil.d(TAG, Gson().toJson(orderDish))
        }
    }

    fun getDateWeek(): MutableList<SelectDateBean> {
        val beanList = mutableListOf<SelectDateBean>()
        for (i in 0..6) {
            val millis = System.currentTimeMillis() + i * 86400000
            val dateFormat = TimeUtil.timeFormat("yyyy-MM-dd", millis)
            beanList.add(SelectDateBean(dateFormat, getWeekDay(dateFormat)))
        }
        return beanList
    }

    private fun getWeekDay(date: String): String {
        if (!isValidDate(date)) return ""
        val split = date.split("-".toRegex())
        val calendar = Calendar.getInstance()
        calendar[split[0].toInt(), split[1].toInt()] = split[2].toInt()
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