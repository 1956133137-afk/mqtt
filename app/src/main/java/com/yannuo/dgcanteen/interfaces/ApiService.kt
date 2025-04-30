package com.yannuo.dgcanteen.interfaces

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yannuo.dgcanteen.download.AppInfoB
import com.yannuo.dgcanteen.download.AppUpdateResultB
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.model.*
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    //检查APP新版本
    @Headers("content-type: application/json")
    @POST
    fun checkAppUpdate(@Url url: String?, @Body info: AppInfoB?): Observable<AppUpdateResultB>

    /**
     * 获取设备指定的支付配置
     */

    @Headers("content-type: application/json")
    @POST("deviceData/selectDeviceData")
    suspend fun getPayCfg(@Body sn: PayCfgRequest): CanteenResponse<PayCfg>


    /**
     * 根据卡号搜索用户信息
     */

    @Headers("content-type: application/json")
    @POST("deviceData/byCardIdSelectUser")
    suspend fun queryPerson(@Body bean: CardUserRequest): CanteenResponse<UserInfoBean>

    /**
     * 下载园区人员
     */

    @Headers("content-type: application/json")
    @POST("deviceData/selectUserData")
    suspend fun downPerson(@Body request: PersonRequest): CanteenResponse<String>

    @Headers("content-type: application/json")
    @POST("deviceData/selectUserData2")
    suspend fun downPerson2(@Body request: PersonRequest): CanteenResponse<String>

    // 获取菜品
    @Headers("content-type: application/json")
    @GET("android/getDishes")
    suspend fun ccbDishes(@Query("deviceId") sn: String): CanteenResponse<MutableList<DayDishesBean>>

    //获取带餐别菜品信息
    @Headers("content-type: application/json")
    @GET("android/getDishesV2")
    suspend fun ccbDishesAndCategory(@Query("deviceId") sn: String): CanteenResponse<MutableList<DayDishesBean>>

    // 同步消费记录
    @Headers("content-type: application/json")
    @POST("deviceData/insertPaymentRecord")
    suspend fun synCsRecord(@Body data: SynConsumeRecordBean): CanteenResponse<String>

    // 根据卡号查询用户信息
    @Headers("content-type: application/json")
    @POST("deviceData/byCardIdSelectUser")
    suspend fun queryPersonByCardId(@Body bean: RequestPerson): CanteenResponse<Persons>

    // 根据卡号或二维码查询余额
    @Headers("content-type: application/json")
    @POST("android/queryBalance")
    suspend fun queryBalance(@Body encryptedData: EncryptedDataRequest): CanteenResponse<String>

    //智慧食堂请求
    @Headers("content-type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("B2CMainPlat_00_ZHST")
    suspend fun ccbRequestNet(@FieldMap map: Map<String, String>): Response<ResponseBody>

    @Headers("content-type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("B2CMainPlat_00_ZHST")
    suspend fun ccbPersonBanlance(@FieldMap map: Map<String, String>): BalanceResponse


    @Headers("content-type: application/json")
    @POST("android/isConsumeLimit")
    suspend fun spendLimit(@Body data: SpendLimitBean): CanteenResponse<LimitBean>


    @Headers("content-type: application/json")
    @POST("ccbPay/pay")
    suspend fun ccbPayBeSwept(@Body data: CCBRequest): CanteenResponse<String>

    /**
     * 订餐核销
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/verification")
    suspend fun ccbCodeVerification(@Body data: VerificationRequest): CanteenResponse<JsonObject>

    /**
     * 订餐统计
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/countDCofDay")
    suspend fun verificationCount(@Body data: VerificationCountRequest): CanteenResponse<VerificationCountResponse>

    /**
     * 菜品统计
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/countDCofDishes")
    suspend fun dishesCount(@Body data: VerificationCountRequest): CanteenResponse<DishesCountResponse>

    /**
     * 二维码被扫支付
     */
    @Headers("content-type: application/json")
    @POST("android/qrCodePayment")
    suspend fun payByQrCode(@Body bean: RequestPay): CanteenResponse<String>

    /**
     * 刷卡支付
     */
    @Headers("content-type: application/json")
    @POST("android/cardPayment")
    suspend fun payByIcCard(@Body bean: RequestPay): CanteenResponse<String>

    /**
     * 刷脸支付
     */
    @Headers("content-type: application/json")
    @POST("android/scanFacePayment")
    suspend fun payByFace(@Body bean: RequestPay): CanteenResponse<String>

    /**
     * 获取TOKEN
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/decrypt")
    suspend fun getToken(@Body bean: TokenBean): CanteenResponse<TokenReceive>

    /**
     * 获取商家配置
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/businessConfig/qryBusinessConfig")
    suspend fun getBusinessConfig(@Header("dcccbauthorization") token: String, @Body bean: OrderMealBean): CanteenResponse<JsonObject>

    /**
     * 获取窗口列表
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/windowList")
    suspend fun getWindowList(@Header("dcccbauthorization") token: String, @Body bean: OrderMealBean): CanteenResponse<JsonArray>

    /**
     * 获取餐别已订份数
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/order/dateHasOrderMeal")
    suspend fun getMealOrderSize(@Header("dcccbauthorization") token: String, @Body bean: MealSizeBean): CanteenResponse<JsonObject>

    /**
     * 查询餐别信息
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/order/qryOrderMeal")
    suspend fun queryOrderMeal(@Header("dcccbauthorization") token: String, @Body bean: OrderMealBean): CanteenResponse<OrderMealReceive>

    /**
     * 查询餐别菜品信息
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dishes/batchList")
    suspend fun queryOrderDish(@Header("dcccbauthorization") token: String, @Body bean: OrderDishBean): CanteenResponse<OrderDishReceive>

    /**
     * 订餐下单接口
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/insert")
    suspend fun insertOrder(@Header("dcccbauthorization") token: String, @Body bean: InsertOrderBean): CanteenResponse<InsertOrderReceive>

    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/batchOrder")
    suspend fun insertBatchOrder(@Header("dcccbauthorization") token: String, @Body bean: InsertBatchOrderBean): CanteenResponse<InsertBatchOrderReceive>

    /**
     * 查询订餐列表
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/list")
    suspend fun getOrderList(@Header("dcccbauthorization") token: String, @Body bean: OrderListBean): CanteenResponse<JsonObject>

    /**
     * 查询订餐列表(备餐模式)
     */
    @Headers("content-type: application/json")
    @POST("android/getOrderMealList")
    suspend fun getMealPreparaionList(@Body bean: MealPreparationBean): CanteenResponse<JsonObject>

    /**
     * 修改订单状态(备餐模式)
     */
    @Headers("content-type: application/json")
    @POST("android/changeOrderStatus")
    suspend fun ModifyTheOrderStatus(@Body bean: OrderStatusBean): CanteenResponse<JsonObject>


    //查询所有餐别
    @Headers("content-type: application/json")
    @POST("android/getMealList")
    suspend fun queryAllMeal(@Body bean: MealRequestPerson): CanteenResponse<MutableList<MealBean>>

    /**
     * 退餐接口
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/directRefund")
    suspend fun orderDirectRefund(@Header("dcccbauthorization") token: String, @Body bean: OrderRefundBean): CanteenResponse<JsonObject>

    /**
     * 订餐菜品统计
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/countDishesOfWindow")
    suspend fun countDishesOfWindow(@Body bean: CountDishesOfWindowBean): CanteenResponse<CountDishesOfWindowResponse>

    /**
     * 预订餐核销
     */
    @Headers("content-type: application/json")
    @POST("android/preOrderMealVerify")
    suspend fun orderVerify(@Body data: VerificationRequest): CanteenResponse<JsonObject>

    /**
     * 查询个人的剩余餐次(申万宏源)
     */
    @Headers("content-type: application/json")
    @POST("swAndroid/qryAllowanceUser")
    suspend fun queryPersonRestMealTime(@Body encryptedData: EncryptedDataRequest): CanteenResponse<MutableList<PersonRestMealTime>>

    /**
     * 查询餐别剩余被取餐次数(申万宏源)
     */
    @Headers("content-type: application/json")
    @POST("swAndroid/getAllowanceUserCount")
    suspend fun queryMealRestTime(@Body encryptedData: EncryptedDataRequest): CanteenResponse<MealRestTimeReceive>

    /**
     * 查询人员餐次规则(申万宏源)
     */
    @Headers("content-type: application/json")
    @POST("swAndroid/selectMealAllowance")
    suspend fun queryMealTimeRule(@Body encryptedData: EncryptedDataRequest): CanteenResponse<MutableList<MealTimeRuleReceive>>

    /**
     * 刷卡支付(申万宏源)
     */
    @Headers("content-type: application/json")
    @POST("swAndroid/swCardPayment")
    suspend fun swPayWithCard(@Body bean: RequestPay): CanteenResponse<String>

    /**
     * 扫码支付(申万宏源)
     */
    @Headers("content-type: application/json")
    @POST("swAndroid/swQrCodePayment")
    suspend fun swPayWithCode(@Body bean: RequestPay): CanteenResponse<String>

    /**
     * 根据订单号获取对应扣减餐次详情
     */
    @Headers("content-type: application/json")
    @POST("swAndroid/qryAllowanceByTradeOrderId")
    suspend fun swQueryAllowance(@Body encryptedData: EncryptedDataRequest): CanteenResponse<QueryAllowanceResponse>
}