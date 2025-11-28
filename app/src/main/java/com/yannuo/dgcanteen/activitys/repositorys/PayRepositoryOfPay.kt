package com.yannuo.dgcanteen.activitys.repositorys

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import retrofit2.Response

class PayRepositoryOfPay {
    private val TAG = javaClass.simpleName

    /**
     * 健康状态查询接口
     */
    suspend fun getServerStatus(deviceId: String): CanteenResponse<OpenApiBean> {
        return apiCall2 {
            val serverStatus = RetrofitClient.getOpenApiService().getServerStatus(deviceId)
            Log.d(TAG, "getServerStatus: 健康状态查询直接返回：${Gson().toJson(serverStatus)}")
            serverStatus
        }
    }

    suspend fun getDayDishes(): CanteenResponse<MutableList<DayDishesBean>> {
        return apiCall {
            val sn = CommonAndDpToPxUtil.getDeviceSerial()
            val ben = RetrofitClient.getApi().ccbDishesAndCategory(sn)
            return@apiCall ben
        }
    }


    suspend fun getPayCfg(): CanteenResponse<PayCfg> {
        return apiCall {
            val request = PayCfgRequest(CommonAndDpToPxUtil.getDeviceSerial())
            RetrofitClient.getApi().getPayCfg(request)
        }
    }

    suspend fun CCBPayBeSwept(bean: CCBRequest): CanteenResponse<String> {
        return apiCall {
            val ben = RetrofitClient.getApi().ccbPayBeSwept(bean)
            return@apiCall ben
        }
    }

    //3、根据卡号搜索用户信息
    suspend fun queryPerson(bn: CardUserRequest): CanteenResponse<UserInfoBean> {
        return apiCall {
            RetrofitClient.getApi().queryPerson(bn)
        }
    }

    //查询用户全部账户余额
    suspend fun ccbPersonBanlance(path: Map<String, String>): CanteenResponse<BalanceResponse> {
        return apiCall {
            val ccbPersonBanlance = RetrofitClient.getApiCcb().ccbPersonBanlance(path)
            CanteenResponse<BalanceResponse>("200").apply {
                data = ccbPersonBanlance
            }
        }
    }


    /**
     *
     * @param pageSize Int 每页大小
     * @param page Int  第几页
     * @return CanteenResponse<PayCfg>
     */
    suspend fun downPerson(pageSize: Int, page: Int): CanteenResponse<String> {
        return apiCall {
            val request = PersonRequest(page, pageSize, "", CommonAndDpToPxUtil.getDeviceSerial())
            RetrofitClient.getApi().downPerson(request)
        }
    }

    suspend fun downPerson2(pageSize: Int, page: Int, campusId: String = ""): CanteenResponse<String> {
        return apiCall {
//            val request = PersonRequest(page, pageSize, campusId, CommonAndDpToPxUtil.getDeviceSerial())
            RetrofitClient.getPersonApi().downPerson2(campusId, page, pageSize)
        }
    }

    suspend fun synCsRecord(data: SynConsumeRecordBean): CanteenResponse<String> {
        return apiCall {
            val ben = RetrofitClient.getApi().synCsRecord(data)
            return@apiCall ben
        }
    }

    suspend fun queryPersonByCardId(bean: RequestPerson): CanteenResponse<Persons> {
        return apiCall {
            return@apiCall RetrofitClient.getApi().queryPersonByCardId(bean)
        }
    }

    suspend fun queryBalance(encryptedData: EncryptedDataRequest): CanteenResponse<String> {
        return apiCall {
            RetrofitClient.getApi().queryBalance(encryptedData)
        }
    }

    suspend fun getCcbData(map: MutableMap<String, String>): Response<ResponseBody> {
        return RetrofitClient.getApiCcb().ccbRequestNet(map)
    }

    suspend fun getCcbServlet(map: Map<String, String>): String {
        val response = apiCall2 { RetrofitClient.getApiCcb().ccbRequestNet(map) }
        var params: String = ""
        if (response.code == "200") params = response.data?.body()?.string() ?: ""
        else {
            val errorMap = HashMap<String, String>()
            errorMap["RESULT"] = "N"
            errorMap["ERRCODE"] = response.code
            errorMap["ERRMSG"] = response.msg
            params = Gson().toJson(errorMap)
        }
        return params.trim().replace("(\r\n|\n\r|\n|\r)".toRegex(), "")
    }

    suspend fun getConsumeStatus(data: SpendLimitBean): CanteenResponse<LimitBean> {
        return apiCall {
            return@apiCall RetrofitClient.getApi().spendLimit(data)
        }
    }

    suspend fun getCcbCodeVerification(data: VerificationRequest): CanteenResponse<JsonObject> {
        return apiCall {
            val ben = RetrofitClient.getApi().ccbCodeVerification(data)
            return@apiCall ben
        }
    }

    suspend fun getCcbCountDCofDay(data: VerificationCountRequest): CanteenResponse<VerificationCountResponse> {
        return apiCall {
            return@apiCall RetrofitClient.getApi().verificationCount(data)
        }
    }

    suspend fun getDishesCount(data: VerificationCountRequest): CanteenResponse<DishesCountResponse> {
        return apiCall {
            return@apiCall RetrofitClient.getApi().dishesCount(data)
        }
    }

    suspend fun payByQrCode(encryptedData: String): CanteenResponse<String> {
        return apiCall {
            val request = RequestPay(encryptedData)
            RetrofitClient.getApi().payByQrCode(request)
        }
    }

    suspend fun payByIcCard(encryptedData: String): CanteenResponse<String> {
        return apiCall {
            val request = RequestPay(encryptedData)
            RetrofitClient.getApi().payByIcCard(request)
        }
    }

    suspend fun payByFace(encryptedData: String): CanteenResponse<String> {
        return apiCall {
            val request = RequestPay(encryptedData)
            RetrofitClient.getApi().payByFace(request)
        }
    }

    suspend fun payYnServlet(payType: String, payParams: String): String {
        val pathUrl = when (payType) {
            "2" -> "qrCodePayment"
            else -> "cardPayment"
        }
        LogUtil.d(TAG, "支付请求参数: $payParams")
        val request = RequestPay(DES3CBCUtil.encryption(payParams))
        val response = apiCall { RetrofitClient.getApi().payYnServlet(pathUrl, request) }
        var params: String = ""
        judgeNetworkException(response.code == "200")
        if (response.code == "200") params = DES3CBCUtil.decryptRSA(response.data ?: "")
        else {
            val errorMap = HashMap<String, String>()
            errorMap["RESULT"] = "N"
            errorMap["ERRCODE"] = response.code
            errorMap["ERRMSG"] = response.msg
            params = Gson().toJson(errorMap)
        }
        LogUtil.d(TAG, "${Constant.NRE_TIMES}")
        LogUtil.d(TAG, "支付响应参数: $params")
        return params
    }

    private fun judgeNetworkException(boolean: Boolean) {
        if (boolean && Constant.NRE_TIMES > 0) {
            if (Constant.NRE_TIMES > 1) EventBus.getDefault().post(MessageEvent(Constant.EVENT_NETWORK_EXCEPTION, 0))
            Constant.NRE_TIMES = 0
        } else if (!boolean && Constant.NRE_TIMES < 10) {
            Constant.NRE_TIMES++
            if (Constant.NRE_TIMES == 2) EventBus.getDefault().post(MessageEvent(Constant.EVENT_NETWORK_EXCEPTION, 1))
        }
    }

    suspend fun getToken(campusId: String, corpId: String, encryptStr: String): CanteenResponse<TokenReceive> {
        return apiCall {
            RetrofitClient.getApi().getToken(TokenBean(campusId, corpId, encryptStr))
        }
    }

    suspend fun getBusinessConfig(token: String, campusId: String, businessId: String): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().getBusinessConfig(token, OrderMealBean(campusId, businessId))
        }
    }

    suspend fun getWindowList(token: String, businessId: String, campusId: String): CanteenResponse<JsonArray> {
        return apiCall {
            RetrofitClient.getApi().getWindowList(token, OrderMealBean(campusId, businessId))
        }
    }

    suspend fun getMealOrderSize(token: String, bean: MealSizeBean): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().getMealOrderSize(token, bean)
        }
    }

    suspend fun getCategoryIdList(token: String, bean: RequeCategoryIdBean): CanteenResponse<JsonArray> {
        return apiCall {
            RetrofitClient.getApi().getCategoryIdList(token, bean)
        }
    }

    suspend fun queryOrderMeal(token: String, campusId: String, businessId: String): CanteenResponse<OrderMealReceive> {
        return apiCall {
            RetrofitClient.getApi().queryOrderMeal(token, OrderMealBean(campusId, businessId))
        }
    }

    suspend fun queryOrderDish(token: String, date: String, mealId: String, businessId: String, campusId: String): CanteenResponse<OrderDishReceive> {
        return apiCall {
            RetrofitClient.getApi().queryOrderDish(token, OrderDishBean(date, mealId, businessId, campusId))
        }
    }

    suspend fun insertOrder(token: String, bean: InsertOrderBean): CanteenResponse<InsertOrderReceive> {
        return apiCall {
            RetrofitClient.getApi().insertOrder(token, bean)
        }
    }

    suspend fun insertBatchOrder(token: String, bean: InsertBatchOrderBean): CanteenResponse<InsertBatchOrderReceive> {
        return apiCall {
            RetrofitClient.getApi().insertBatchOrder(token, bean)
        }
    }

    suspend fun getOrderList(token: String, bean: OrderListBean): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().getOrderList(token, bean)
        }
    }

    suspend fun getMealPreparaionList(bean: MealPreparationBean): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().getMealPreparaionList(bean)
        }
    }

    /**
     * 修改订单状态
     */
    suspend fun ModifyTheOrderStatus(bean: OrderStatusBean): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().ModifyTheOrderStatus(bean)
        }
    }

    suspend fun queryAllMeal(bean: MealRequestPerson): CanteenResponse<MutableList<MealBean>> {
        return apiCall {
            RetrofitClient.getApi().queryAllMeal(bean)
        }
    }

    suspend fun DCRefundIsOverTime(token: String, bean: DCRefundIsOverTimeBean): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().DCRefundIsOverTime(token, bean)
        }
    }

    suspend fun orderDirectRefund(token: String, bean: OrderRefundBean): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().orderDirectRefund(token, bean)
        }
    }

    suspend fun getCountDishes(bean: CountDishesOfWindowBean): CanteenResponse<CountDishesOfWindowResponse> {
        return apiCall {
            RetrofitClient.getApi().countDishesOfWindow(bean)
        }
    }

    suspend fun orderVerify(data: BookMealRequest): CanteenResponse<JsonObject> {
        return apiCall {
            val ben = RetrofitClient.getApi().orderVerify(data)
            return@apiCall ben
        }
    }

    suspend fun queryPersonRestMealTime(encryptedData: EncryptedDataRequest): CanteenResponse<MutableList<PersonRestMealTime>> {
        return apiCall {
            RetrofitClient.getApi().queryPersonRestMealTime(encryptedData)
        }
    }

    suspend fun queryMealRestTime(encryptedData: EncryptedDataRequest): CanteenResponse<MealRestTimeReceive> {
        return apiCall {
            RetrofitClient.getApi().queryMealRestTime(encryptedData)
        }
    }

    suspend fun queryMealTimeRule(encryptedData: EncryptedDataRequest): CanteenResponse<MutableList<MealTimeRuleReceive>> {
        return apiCall {
            RetrofitClient.getApi().queryMealTimeRule(encryptedData)
        }
    }

    suspend fun swPayByIcCard(encryptedData: String, orderFlag: String, isAllowance: String, actualMealId: Int?, mealId: Int?, userMealId: Int?): CanteenResponse<String> {
        return apiCall {
            val request = RequestPay(encryptedData, 1, orderFlag, isAllowance, actualMealId, mealId, userMealId)
            LogUtil.i(TAG, "swPayByIcCard request: $request")
            RetrofitClient.getApi().swPayWithCard(request)
        }
    }

    suspend fun swPayByQrCode(encryptedData: String, orderFlag: String, isAllowance: String, actualMealId: Int?, mealId: Int?, userMealId: Int?): CanteenResponse<String> {
        return apiCall {
            val request = RequestPay(encryptedData, 1, orderFlag, isAllowance, actualMealId, mealId, userMealId)
            LogUtil.i(TAG, "swPayByQrCode request: $request")
            RetrofitClient.getApi().swPayWithCode(request)
        }
    }

    suspend fun swQueryAllowance(encryptedData: EncryptedDataRequest): CanteenResponse<QueryAllowanceResponse> {
        return apiCall {
            RetrofitClient.getApi().swQueryAllowance(encryptedData)
        }
    }

    suspend fun printOrderTicket(bean: PrintTicketBean): CanteenResponse<String> {
        return apiCall {
            RetrofitClient.getApi().printOrderTicket(bean)
        }
    }

    /**
     * 下载人脸特征值
     */
    suspend fun downUserFaceDBImg(encryptedData: EncryptedDataRequest): CanteenResponse<String> {
        return apiCall {
            RetrofitClient.getApi().downUserFaceDBImg(encryptedData)
        }
    }

    suspend fun uploadUserEigenvalue(encryptedData: EncryptedDataRequest): CanteenResponse<String> {
        return apiCall {
            RetrofitClient.getApi().uploadUserEigenvalue(encryptedData)
        }
    }

    suspend fun uploadFaceDBImg(encryptedData: EncryptedDataRequest): CanteenResponse<String> {
        return apiCall {
            RetrofitClient.getApi().uploadFaceDBImg(encryptedData)
        }
    }


    private suspend fun <T> apiCall(call: suspend CoroutineScope.() -> CanteenResponse<T>): CanteenResponse<T> {
        return withContext(Dispatchers.IO) {
            val res: CanteenResponse<T>
            try {
                res = call()
            } catch (e: Throwable) {
                e.printStackTrace()
                return@withContext ApiException.build(e).toResponse<T>()
            }
            res
        }
    }

    private suspend fun <T> apiCall2(call: suspend CoroutineScope.() -> T): CanteenResponse<T> {
        return withContext(Dispatchers.IO) {
            val res: CanteenResponse<T>
            try {
                res = CanteenResponse()
                res.data = call()
            } catch (e: Throwable) {
                // 请求出错，将状态码和消息封装为 ResponseResult
                return@withContext ApiException.build(e).toResponse<T>()
            }
            return@withContext res
        }
    }
}