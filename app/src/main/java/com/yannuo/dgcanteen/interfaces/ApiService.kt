package com.yannuo.dgcanteen.interfaces

import com.yannuo.dgcanteen.download.AppInfoB
import com.yannuo.dgcanteen.download.AppUpdateResultB
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


    // 获取菜品
    @Headers("content-type: application/json")
    @GET("android/getDishes")
    suspend fun ccbDishes(@Query("deviceId") sn: String): CanteenResponse<MutableList<DayDishesBean>>

    // 同步消费记录
    @Headers("content-type: application/json")
    @POST("deviceData/insertPaymentRecord")
    suspend fun synCsRecord(@Body data: SynConsumeRecordBean): CanteenResponse<String>

    // 根据卡号查询用户信息
    @Headers("content-type: application/json")
    @POST("deviceData/byCardIdSelectUser")
    suspend fun userInfo(@Body map: Map<String, String>): CanteenResponse<UserInfoBean>

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
    suspend fun ccbCodeVerification(@Body data: VerificationRequest): CanteenResponse<VerificationResponse>

    /**
     * 订餐统计
     */
    @Headers("content-type: application/json")
    @POST("dcCcb/dcOrderRecord/countDCofDay")
    suspend fun verificationCount(@Body data: VerificationCountRequest): CanteenResponse<VerificationCountResponse>
}