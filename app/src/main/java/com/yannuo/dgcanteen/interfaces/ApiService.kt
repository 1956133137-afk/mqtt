package com.yannuo.dgcanteen.interfaces



import com.yannuo.dgcanteen.download.AppInfoB
import com.yannuo.dgcanteen.download.AppUpdateResultB
import com.yannuo.dgcanteen.model.CanteenResponse
import com.yannuo.dgcanteen.model.DayDishesBean
import com.yannuo.dgcanteen.model.ScanQrResultBean
import com.yannuo.dgcanteen.model.SynConsumeRecordBean
import io.reactivex.Observable
import retrofit2.http.*

interface ApiService {

    //检查APP新版本
    @Headers("content-type: application/json")
    @POST
    fun checkAppUpdate(@Url url: String?, @Body info: AppInfoB?): Observable<AppUpdateResultB>

    // 获取菜品
    @Headers("content-type: application/json")
    @GET("android/getDishes")
    suspend fun ccbDishes( @Query("deviceId")sn : String): CanteenResponse<MutableList<DayDishesBean>>

    // 同步消费记录
    @Headers("content-type: application/json")
    @POST("deviceData/insertPaymentRecord")
    suspend fun synCsRecord(@Body data: SynConsumeRecordBean) : CanteenResponse<String>

    //二维码被扫支付
    @Headers("content-type: application/x-www-form-urlencoded")
    @POST
    suspend fun scanQrPay(@Url url: String?): ScanQrResultBean

    // 同步消费记录
    @Headers("content-type: application/json")
    @POST("deviceData/insertPaymentRecord")
    suspend fun synConsumeRecord(@Body data: SynConsumeRecordBean)
//
//
//    //  建行
//    @Headers("content-type: application/json")
//    @POST("pay/ccb/panda/swept")
//    suspend fun ccbPay(  @Query("carrier")sn : String): PayResponse<String>
//
//    //建行  查询支付状态
//    @Headers("content-type: application/json")
//    @POST("pay/ccb/panda/payState")
//    suspend fun ccbPayState(  @Query("carrier")sn : String): PayResponse<String>
//
//
//    //建行  获取支付动态二维码
//    @Headers("content-type: application/json")
//    @POST("pay/ccb/panda/dynamicQr")
//    suspend fun ccbDynamicQr(  @Query("carrier")sn : String): PayResponse<String>
//
//    //建行  获取支聚合支付动态二维码
//    @Headers("content-type: application/json")
//    @POST("pay/ccb/aggregation")
//    suspend fun ccbAggregation(  @Query("carrier")sn : String): PayResponse<String>
//
//    //建行  查询聚合支付支付状态
//    @Headers("content-type: application/json")
//    @POST("pay/ccb/aggregation/payState")
//    suspend fun ccbAggregationState(  @Query("carrier")sn : String): PayResponse<String>

}