package com.yannuo.dgcanteen.activitys.repositorys

import com.google.gson.Gson
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.CanteenEncryptionUtil
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.paymoney.utils.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PayRepositoryOfPay {
    private val TAG = javaClass.simpleName


    suspend fun getDayDishes(): CanteenResponse<MutableList<DayDishesBean>> {
        return apiCall {
            val sn = CommonAndDpToPxUtil.getDeviceSerial()
            val ben = RetrofitClient.getApi().ccbDishes(sn)
            return@apiCall ben
        }
    }

    suspend fun synCsRecord(data :SynConsumeRecordBean): CanteenResponse<String> {
        return apiCall {
            val ben = RetrofitClient.getApi().synCsRecord(data)
            return@apiCall ben
        }
    }


    suspend fun getScanQrData(bean: CcbScanPayBean): ScanQrResultBean {
        return apiCallForScanCode {
            val sn = CanteenEncryptionUtil.requestScanData(bean)
            RetrofitClient.getApi().scanQrPay(sn)
        }
    }

    suspend fun setConsumeRecord(data :SynConsumeRecordBean){
        data.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
        RetrofitClient.getApi().synConsumeRecord(data)
    }
//
//
//    /**
//     * 获取建行聚合支付动态二维码
//     * @param bean PayInfoCcb
//     * @return PayResponse<CcbPandaSweptResponse>
//     */
//    suspend fun getccbAggregation(bean: PayInfoCcb): PayResponse<String> {
//        return apiCall {
//            var json = Gson().toJson(bean)
//            json = AESUtils.encrypt(key,json)
//            val ben = RetrofitClientOfPay.getApi().ccbAggregation(json)
//            val res = PayResponse<String>(ben.code,ben.msg)
//            ben.data?.also {
//                val info = AESUtils.decrypt(key,it)
//                res.data = info
//            }
//            return@apiCall res
//        }
//    }
//
//
//    /**
//     * 支付交易
//     * @param bean PayInfoCcb
//     * @return PayResponse<CcbPandaSweptResponse>
//     */
//    suspend fun ccbSweptQr(data: PayInfoCcb):PayResponse<CcbPandaSweptResponse>{
//        return apiCall {
//            var json = Gson().toJson(data)
//            json = AESUtils.encrypt(key,json)
//            val ben = RetrofitClientOfPay.getApi().ccbPay(json)
//            val res = PayResponse<CcbPandaSweptResponse>(ben.code,ben.msg)
//            ben.data?.also {
//                val info = Gson().fromJson(AESUtils.decrypt(key,it),CcbPandaSweptResponse::class.java)
//                res.data = info
//            }
//            return@apiCall res
//        }
//    }
//
//
//    /**
//     * 查询订单支付结果
//     * @param bean PayStateSearchCcb
//     * @return PayResponse<SearchPaySResult>
//     */
//     suspend fun ccbSearchPayState(bean: PayStateSearchCcb):PayResponse<SearchPaySResult>{
//         return apiCall {
//             var json = Gson().toJson(bean)
//             json = AESUtils.encrypt(key,json)
//             val ben = RetrofitClientOfPay.getApi().ccbPayState(json)
//             val res = PayResponse<SearchPaySResult>(ben.code,ben.msg)
//             ben.data?.also {
////                 val cont  = "{"SUCCESS":"true","Detail_Grp":[{"STATUSCODE":"00","ORDERID":"1011213022122617215853515","AMOUNT":"0.01","ORDERDATE":"20221226172159","ACCNAME":"程**","Pref_Amt":"0.00","CmAvy_Cntnt":""}]}"
////                 val info = Gson().fromJson(AESUtils.decrypt(key,cont),SearchPaySResult::class.java)
//                 val info = Gson().fromJson(AESUtils.decrypt(key,it),SearchPaySResult::class.java)
//                 res.data = info
//             }
//             return@apiCall res
//         }
//     }
//
//    /**
//     * 查询订单支付结果
//     * @param bean PayStateSearchCcb
//     * @return PayResponse<SearchPaySResult>
//     */
//    suspend fun ccbAggregationPayState(bean: PayStateAggregationCcb):PayResponse<String>{
//        return apiCall {
//            var json = Gson().toJson(bean)
//            json = AESUtils.encrypt(key,json)
//            val ben = RetrofitClientOfPay.getApi().ccbAggregationState(json)
//            val res = PayResponse<String>(ben.code,ben.msg)
//            ben.data?.also {
////                 val cont  = "{"SUCCESS":"true","Detail_Grp":[{"STATUSCODE":"00","ORDERID":"1011213022122617215853515","AMOUNT":"0.01","ORDERDATE":"20221226172159","ACCNAME":"程**","Pref_Amt":"0.00","CmAvy_Cntnt":""}]}"
////                 val info = Gson().fromJson(AESUtils.decrypt(key,cont),SearchPaySResult::class.java)
//                val str = AESUtils.decrypt(key,it)
//                LogUtil.d(TAG,"str:$str")
////                XmlPullParser
////                val info = Gson().fromJson(str,SearchPaySResult::class.java)
//                res.data = str
//            }
//            return@apiCall res
//        }
//    }
//
//
//
//
//    /**
//     * 获取商户信息
//     * @return PayResponse<MerchantInfo>
//     */
//    suspend fun getMerchantInfo():PayResponse<List<MerchantPackage>>{
//        return apiCall {
//            var sn = DeviceUtil.getDeviceSerial()
//            sn = AESUtils.encrypt(key,sn)
//            val bean =  RetrofitClientOfPay.getApi().ccbMerchant(sn)
//            val res = PayResponse<List<MerchantPackage>>(bean.code,bean.msg)
//            bean.data?.also {
//                val content = AESUtils.decrypt(key,it)
//                val type = object : TypeToken<List<MerchantPackage>>() {}.type
//                val gson = Gson()
//                val info = gson.fromJson<List<MerchantPackage>>(content,type)
//
//                for (bn in info){
//                      when(bn.type){
//                          PayConstant.MERCHANT_PANDA ->{
//                              TempMerchantsInfo.ccbPanda = gson.fromJson(bn.data,MerchantInfo::class.java)
//                              LogUtil.d(TAG,"获取到熊猫支付商户：${bn.data}")
//                          }
//                          PayConstant.MERCHANT_AGGREGATION ->{
//                              TempMerchantsInfo.aggregation = gson.fromJson(bn.data,MerchantInfo::class.java)
//                              LogUtil.d(TAG,"获取到聚合支付商户：${bn.data}")
//                          }
//                      }
//                }
//              //   info = Gson().fromJson(content,MerchantInfo::class.java)
//                res.data = info
//            }
//            return@apiCall res
//        }
//    }
//
    private suspend fun <T> apiCall(call :suspend CoroutineScope.() -> CanteenResponse<T>):CanteenResponse<T>{
        return withContext(Dispatchers.IO){
            val res:CanteenResponse<T>
            try {
                res = call()
            }catch (e: Throwable){
                return@withContext ApiException.build(e).toResponse<T>()
            }
            res
        }
    }

    private suspend fun apiCallForScanCode(call :suspend CoroutineScope.() -> ScanQrResultBean):ScanQrResultBean {
        return withContext(Dispatchers.IO){
            val res:ScanQrResultBean
            try {
                res = call()
            }catch (e: Throwable){
                return@withContext ApiException.build(e).toResponseForScanCode()
            }
            res
        }
    }
}