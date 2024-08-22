package com.yannuo.dgcanteen.activitys.repositorys

import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val request = PersonRequest(CommonAndDpToPxUtil.getDeviceSerial(), page, pageSize)
            RetrofitClient.getApi().downPerson(request)
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

    suspend fun getCcbData(map: MutableMap<String, String>): Response<ResponseBody> {
        return RetrofitClient.getApiCcb().ccbRequestNet(map)
    }

    suspend fun getConsumeStatus(data: SpendLimitBean): CanteenResponse<LimitBean> {
        return apiCall {
            return@apiCall RetrofitClient.getApi().spendLimit(data)
        }
    }

    suspend fun getCcbCodeVerification(data: VerificationRequest): CanteenResponse<VerificationResponse> {
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

    private suspend fun <T> apiCall(call: suspend CoroutineScope.() -> CanteenResponse<T>): CanteenResponse<T> {
        return withContext(Dispatchers.IO) {
            val res: CanteenResponse<T>
            try {
                res = call()
            } catch (e: Throwable) {
                return@withContext ApiException.build(e).toResponse<T>()
            }
            res
        }
    }
}