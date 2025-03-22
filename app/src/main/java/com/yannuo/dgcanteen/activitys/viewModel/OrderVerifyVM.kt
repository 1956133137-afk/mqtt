package com.yannuo.dgcanteen.activitys.viewModel

import android.text.format.DateFormat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/18 10:53
 **/
class OrderVerifyVM : ViewModel(), OnReadDataListener {
    private val TAG = javaClass.simpleName
    private val mmkv = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()
    private val mRepository by lazy { PayRepositoryOfPay() }
    private val mutex: Mutex = Mutex()
    private var requestStatus = false

    private var mCardHandle: SerialPortHelper? = null
    private var listener: VerifyCallBack? = null

    private var isVerifyStatus = true

    private var mMealId = 0
    private var mealId = -1
    val mealTime: MutableLiveData<String> = MutableLiveData<String>("未开餐")

    private var verifyName: String = ""
    val mOrderVerify: MutableLiveData<OrderVerify> = MutableLiveData<OrderVerify>()

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
    }

    init {
        checkTime()
    }

    fun getVerifyName(): String = verifyName

    fun setIsVerifyStatus(status: Boolean) {
        isVerifyStatus = status
    }

    fun setVerifyListener(listener: VerifyCallBack) {
        this.listener = listener
    }

    fun openIcCard() {
        mCardHandle = SerialPortHelper()
        mCardHandle?.readDataListener = this
        mCardHandle?.openSerialPort("/dev/ttyS4")
//        mCardHandle?.openSerialPort("/dev/ttyXRUSB0")
    }

    fun closeIcCard() {
        mCardHandle?.readDataListener = null
        mCardHandle?.closeSerialPort()
        mCardHandle = null
    }

    override fun numberOfIcCard(number: String?) {
        if (number == null) return
        val icCard = number.replace("(\n\r|\r\n|\r|\n)".toRegex(), "").trim().uppercase()
        if (!mmkv.decodeBool(Constant.ORDER_QUERY, false) && !isVerifyStatus) {
            viewModelScope.launch(Dispatchers.Main) { ToastShowUtil.show("无效刷卡") }
            return
        }
        orderVerify(icCard)
    }

    private fun orderVerify(cardId: String) {
        /*加锁防止触发多次请求*/
        if (requestStatus) return
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            mutex.withLock { requestStatus = true }
            listener?.onVerifyResult(-1, OrderVerifyBean())
            /*查询人员信息*/
            val persons = dbHelper.queryPersonToCardId(cardId)
            /*获取请求参数*/
            val request = VerificationRequest().apply {
                dcEncryptParam = getCavEncryptParam(if (persons != null) persons.personName else "", cardId)
                flag = if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) 1 else 0
            }
            LogUtil.d(TAG, Gson().toJson(request))
            val response = mRepository.orderVerify(request)
            LogUtil.d(TAG, Gson().toJson(response))
            if (response.code == "200") {
                val orderVerifyBean = Gson().fromJson(Gson().toJson(response.data ?: ""), OrderVerifyBean::class.java)
                if (!mmkv.decodeBool(Constant.ORDER_QUERY, false)) isVerifyStatus = false
                listener?.onVerifyResult(0, orderVerifyBean)
            } else {
                isVerifyStatus = true
                listener?.onVerifyResult(1, OrderVerifyBean(), response.msg)
            }
            mutex.withLock { requestStatus = false }
        }
    }

    private fun checkTime() {
        viewModelScope.launch(Dispatchers.Default + mHandler) {
            while (isActive) {
                mMealId = TimeUtil.CurrentTimeSection()
                if (mMealId != mealId) {
                    mealId = mMealId
                    val timeStr = when (mealId) {
                        0 -> "未开餐"
                        else -> {
                            val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
                            "${meal.mealName} ${DateFormat.format("HH:mm", meal.startTime)}~${DateFormat.format("HH:mm", meal.endTime)}"
                        }
                    }
                    if (timeStr != mealTime.value) mealTime.postValue(timeStr)
                }
                delay(5000)
            }
        }
    }

    fun getOrderQuery(verifyMap: HashMap<String, JsonArray>): MutableList<Verify> {
        val verifyList: MutableList<Verify> = mutableListOf()
        val dishList = arrayListOf<String>()
        val windowList = arrayListOf<String>()
        verifyMap.forEach { (key, value) ->
            val verify = Verify()
            verify.mealName = key
            dishList.clear()
            val verifyReceive = Gson().fromJson<MutableList<VerifyReceive>>(value, object : TypeToken<MutableList<VerifyReceive>>() {}.type)
            verifyReceive.forEach { receive ->
                windowList.clear()
                receive.window.split("，").forEach { if (it.isNotEmpty() && !windowList.contains(it)) windowList.add(it) }
                dishList.add("${receive.dishes}  →  ${Gson().toJson(windowList).replace("(\\[|\\]|\")".toRegex(), "")}")
            }
            verify.dishesList = dishList
            verifyList.add(verify)
        }
        return verifyList
    }

    fun getOrderVerify(bean: OrderVerifyBean): MutableList<OrderVerify> {
        val orderVerifyList: MutableList<OrderVerify> = mutableListOf()
        verifyName = bean.personName
        /*核销成功*/
        if (bean.verifySuccessDishes.size > 0) {
            val verifySuccess = OrderVerify().apply {
                verifyType = 0
                verifyDishList = bean.verifySuccessDishes
            }
            mOrderVerify.postValue(verifySuccess)
            orderVerifyList.add(verifySuccess)
        } else {
            val orderVerify = OrderVerify().apply {
                verifyType = 0
                verifyMsg = "核销失败原因：${bean.verifyFail.ErrorMessage}"
                verifyDishList = mutableListOf()
            }
            mOrderVerify.postValue(orderVerify)
        }
        /*核销失败*/
        if (bean.verifyFail.verifyFailDishes.size > 0) {
            val verifyFailure = OrderVerify().apply {
                verifyType = 1
                verifyMsg = bean.verifyFail.ErrorMessage
                verifyDishList = bean.verifyFail.verifyFailDishes
            }
            orderVerifyList.add(verifyFailure)
        }
        /*待核销*/
        if (bean.unVerifyDishes.size > 0) {
            val unVerify = OrderVerify().apply {
                verifyType = 2
                verifyMsg = "${Gson().toJson(bean.unVerifyWindowName).replace("(\\[|\\]|\")".toRegex(), "")}"
                verifyDishList = bean.unVerifyDishes
            }
            orderVerifyList.add(unVerify)
        }
        return orderVerifyList
    }

    private fun getCavEncryptParam(custId: String, cardId: String): String {
        /*获取设备商户信息*/
        val mPayCfg = mmkv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
        val encryptStr = StringBuilder()
        /*拼接加密参数*/
        encryptStr.append("CAMPUS_ID=${mPayCfg.campusId}")
            .append("&BUSINESS_ID=${mPayCfg.businessId}")
            .append("&CUST_ID=${custId}")
            .append("&ORDER_ID=")
            .append("&DEVICE_ID=${Utils.getSN()}")
            .append("&CARD_ID=${cardId}")
        LogUtil.d(TAG, encryptStr.toString())
        /*参数加密返回*/
        return CanteenEncryptionUtil.encryption(encryptStr.toString())
    }

    interface VerifyCallBack {
        fun onVerifyResult(type: Int, orderVerifyBean: OrderVerifyBean, errMsg: String = "")
    }
}