package com.yannuo.paylib.nets








import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiServiceOfPay {


    // 获取建行商户信息
//    @FormUrlEncoded
//    @Headers("content-type: application/json")
//    @POST("pay/ccb/panda/merchant")
//    suspend fun ccbMerchant( @Query("carrier")sn : String): PayResponse<String>
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