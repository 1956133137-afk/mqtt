package com.yannuo.dgcanteen.activitys.repositorys

import com.google.gson.Gson
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.DES3CBCUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.paymoney.utils.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response


class PayRepositoryOfPay {
    private val TAG = javaClass.simpleName


    suspend fun getDayDishes(): CanteenResponse<MutableList<DayDishesBean>> {
        return apiCall {
            val sn = CommonAndDpToPxUtil.getDeviceSerial()
            val ben = RetrofitClient.getApi().ccbDishes(sn)
            return@apiCall ben
        }
    }


    suspend fun getPayCfg():CanteenResponse<PayCfg>{
        return apiCall {
            val request = PayCfgRequest(CommonAndDpToPxUtil.getDeviceSerial())
            RetrofitClient.getApi().getPayCfg(request)
        }
    }

    /**
     *
     * @param pageSize Int 每页大小
     * @param page Int  第几页
     * @return CanteenResponse<PayCfg>
     */
    suspend fun downPerson(pageSize :Int ,page : Int):CanteenResponse<String>{
        return apiCall {
            val request = PersonRequest(CommonAndDpToPxUtil.getDeviceSerial(),page,pageSize)
            RetrofitClient.getApi().downPerson(request)
        }
    }

    suspend fun synCsRecord(data :SynConsumeRecordBean): CanteenResponse<String> {
        return apiCall {
            val ben = RetrofitClient.getApi().synCsRecord(data)
            return@apiCall ben
        }
    }

    suspend fun getUserInfo(map: MutableMap<String, String>): CanteenResponse<UserInfoBean> {
        return apiCall {
            val ben = RetrofitClient.getApi().userInfo(map)
            return@apiCall ben
        }
    }

    suspend fun getCcbData(map : MutableMap<String, String>): Response<ResponseBody> {
        return RetrofitClient.getApiCcb().ccbRequestNet(map)
    }

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
}