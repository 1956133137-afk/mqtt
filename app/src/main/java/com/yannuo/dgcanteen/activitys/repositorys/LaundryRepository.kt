package com.yannuo.dgcanteen.activitys.repositorys

import com.google.gson.JsonObject
import com.yannuo.dgcanteen.model.CanteenResponse
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.InsertBatchOrderBean
import com.yannuo.dgcanteen.model.InsertBatchOrderReceive
import com.yannuo.dgcanteen.model.InsertOrderBean
import com.yannuo.dgcanteen.model.InsertOrderReceive
import com.yannuo.dgcanteen.model.LaundryListRequest
import com.yannuo.dgcanteen.model.LaundryOrderRequest
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LaundryRepository {

    suspend fun placeLaundryOrder(token: String, request: LaundryOrderRequest): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().placeLaundryOrder(token, request)
        }
    }

    suspend fun getLaundryList(token: String, request: LaundryListRequest): CanteenResponse<JsonObject> {
        return apiCall {
            RetrofitClient.getApi().getLaundryList(token, request)
        }
    }

    suspend fun insertBatchOrder(token: String, bean: InsertBatchOrderBean): CanteenResponse<InsertBatchOrderReceive> {
        return apiCall {
            RetrofitClient.getApi().insertBatchOrder(token, bean)
        }
    }

    suspend fun insertOrder(token: String, bean: InsertOrderBean): CanteenResponse<InsertOrderReceive> {
        return apiCall {
            RetrofitClient.getApi().insertOrder(token, bean)
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
}
