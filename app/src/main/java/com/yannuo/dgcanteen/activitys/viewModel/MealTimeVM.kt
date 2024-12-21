package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.exception.ResponseException
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.DES3CBCUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/15 11:03
 * @Version 1.0
 */
class MealTimeVM: ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()

    private val mRespository: PayRepositoryOfPay = PayRepositoryOfPay()
    private val mPayCfg: PayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()

    val showErrorToast: MutableLiveData<String> = MutableLiveData()

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "协程异常： $throwable ${throwable.stackTraceToString()}")
        showErrorToast.postValue("错误： ${throwable.message}")
    }

    suspend fun requestMealTimeRule(custId: String): HashMap<Int, MealTimeRuleInfo>{
        val mealTimeRuleMap = hashMapOf<Int, MealTimeRuleInfo>()
        try {
            val gson = Gson()
            val mealTimeRuleRequest = MealTimeRuleRequest(
                mPayCfg.businessId,
                mPayCfg.campusId,
                custId
            )
            LogUtil.i(TAG, "queryMealTimeRule 加密前：$mealTimeRuleRequest")
            val encryption1 = DES3CBCUtil.encryption(gson.toJson(mealTimeRuleRequest))
            val mealTimeRule = mRespository.queryMealTimeRule(EncryptedDataRequest(encryption1))
            LogUtil.i(TAG, "餐次规则：${mealTimeRule}")
            if (mealTimeRule.code != "200") {
                LogUtil.e(TAG, "queryMealTimeRule error: $mealTimeRule")
                throw ResponseException(mealTimeRule.code, mealTimeRule.msg)
            }
            val rule = mealTimeRule.data ?: throw ResponseException("", "queryMealTimeRule response data == null")
            // 获取到餐次消费规则
            val queryAllMeals = dbHelper.queryAllMeals()
            for (r in rule) {
                var no = -1
                for ((i,v) in queryAllMeals.withIndex()) {
                    if (v.mealName == r.mealName) {
                        no = i + 1
                        break
                    }
                }
                val item = MealTimeRuleInfo().apply {
                    id = r.id
                    mealName = r.mealName
                    flag = r.flag
                    mealNum = r.mealNum
                    standardName = r.standardName
                    this.standardNum = r.standardNum
                    subsidyMoney = r.subsidyMoney
                    userTypeId = r.userTypeId
                    userTypeName = r.userTypeName
                    price = r.price
                    price1 = r.price1
                    NO = no
                }
                if (no != -1) mealTimeRuleMap[no] = item
            }
            return mealTimeRuleMap
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, e.message)
            if (e is ResponseException) {
                showErrorToast.postValue("错误： $e")
            } else showErrorToast.postValue("错误： ${e.message}")
            val error = MealTimeRuleInfo().apply {
                mealName = e.message ?: "错误"
            }
            return hashMapOf(-1 to error)
        }
    }

    suspend fun requestPersonRestMealTime(custId: String): HashMap<Int, PersonRestMealTime> {
        val personRestMealTime = hashMapOf<Int, PersonRestMealTime>()
        try {
            val gson = Gson()
            val request = PersonRestMealTimeRequest(
                mPayCfg.businessId,
                mPayCfg.campusId,
                custId
            )
            LogUtil.i(TAG, "queryPersonRestMealTime 加密前：$request")
            val encryption1 = DES3CBCUtil.encryption(gson.toJson(request))
            val restTime = mRespository.queryPersonRestMealTime(EncryptedDataRequest(encryption1))
            LogUtil.i(TAG, "个人剩余餐次：${restTime}")
            if (restTime.code != "200") {
                LogUtil.e(TAG, "queryPersonRestMealTime error: $restTime")
                throw ResponseException(restTime.code, restTime.msg)
            }
            if (restTime.data == null) throw ResponseException("", "queryMealTimeRule response data == null")
            restTime.data?.forEach {
                var no = -1
                val queryAllMeals = dbHelper.queryAllMeals()
                for ((i,v) in queryAllMeals.withIndex()) {
                    if (v.mealId == it.mealId) {
                        no = i + 1
                        break
                    }
                }
                if (no != -1) personRestMealTime[no] = it
            }
            return personRestMealTime
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, e.message)
            if (e is ResponseException) {
                showErrorToast.postValue("错误： $e")
            } else showErrorToast.postValue("错误： ${e.message}")
            val error = PersonRestMealTime(-1,-1,e.message ?: "错误")
            return hashMapOf(-1 to error)
        }
    }

    suspend fun requestMealRestTime(custId: String, mealId: Int): Pair<String, MealRestTimeReceive?> {
        try {
            val gson = Gson()
            val request = MealRestTimeRequest(
                mPayCfg.businessId,
                mPayCfg.campusId,
                custId,
                mealId
            )
            LogUtil.i(TAG, "requestMealRestTime 加密前：$request")
            val encryption1 = DES3CBCUtil.encryption(gson.toJson(request))
            val mealRestTime = mRespository.queryMealRestTime(EncryptedDataRequest(encryption1))
            LogUtil.i(TAG, "mealId: $mealId 餐别剩余可用餐次：${mealRestTime}")
            if (mealRestTime.code != "200") {
                LogUtil.e(TAG, "queryMealRestTime error: $mealRestTime")
                throw ResponseException(mealRestTime.code, mealRestTime.msg)
            }
            if (mealRestTime.data == null) throw ResponseException("", "queryMealTimeRule response data == null")
            return "" to mealRestTime.data!!
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, e.message)
            if (e is ResponseException) {
                showErrorToast.postValue("错误： $e")
            } else showErrorToast.postValue("错误： ${e.message}")
            return (e.message ?: "错误") to null
        }
    }

    suspend fun queryAllowance(orderId: String): Pair<String, QueryAllowanceResponse?> {
        try {
            val gson = Gson()
            val request = QueryAllowanceRequest().apply {
                businessId = mPayCfg.businessId
                campusId = mPayCfg.campusId
                this.orderId = orderId
            }
            LogUtil.i(TAG, "queryAllowance 加密前：${gson.toJson(request)}")
            val encryption1 = DES3CBCUtil.encryption(gson.toJson(request))
            val queryAllowance = mRespository.swQueryAllowance(EncryptedDataRequest(encryption1))
            LogUtil.i(TAG, "queryAllowance 响应结果：${queryAllowance}")
            if (queryAllowance.code != "200") {
                LogUtil.e(TAG, "queryAllowance error: $queryAllowance")
                throw ResponseException(queryAllowance.code, queryAllowance.msg)
            }
//            if (queryAllowance.data == null) throw ResponseException("", "queryAllowance response data == null")
            return "" to queryAllowance.data
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, e.message)
            if (e is ResponseException) {
                showErrorToast.postValue("错误： $e")
            } else showErrorToast.postValue("错误： ${e.message}")
            return (e.message ?: "错误") to null
        }
    }
}