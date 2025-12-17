package com.yannuo.dgcanteen.controls

import android.util.Log
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.greenrobot.eventbus.EventBus

class OpenApiCheckTask {
    private val TAG = javaClass.simpleName
    private val mutex: Mutex = Mutex()
    private var isOpenApi: Boolean = false
    private val kv = MMKV.defaultMMKV()
    private val mRespository: PayRepositoryOfPay = PayRepositoryOfPay()

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        throwable.printStackTrace()
    }

    private val mScope = CoroutineScope(Dispatchers.IO + mHandler)

    companion object {
        @Volatile
        private var openApiCheckTask: OpenApiCheckTask? = null

        @JvmStatic
        fun getInstance(): OpenApiCheckTask {
            if (openApiCheckTask == null) {
                synchronized(OpenApiCheckTask::class.java) {
                    if (openApiCheckTask == null) openApiCheckTask = OpenApiCheckTask()
                }
            }
            return openApiCheckTask!!
        }
    }

    fun getOpenApiStatus() {
        LogUtil.d(TAG,"服务健康检测")
        if (isOpenApi) return
        mScope.launch {
            mutex.withLock { isOpenApi = true }
            while (isActive) {
                val result = mRespository.getServerStatus(CommonAndDpToPxUtil.getDeviceSerial())
                LogUtil.d(TAG, Gson().toJson(result))
                var onlineStatus = 1
                if (result.code == "200") {
                    val apiBean = result.data
//                    if (apiBean?.data?.metrics != null && apiBean.data?.dbHealth != null) {
//                        if (apiBean.code == 429) {
//                            Log.d(TAG, "getOpenApiStatus: 请求频繁，请稍后")
//                            return@launch
//                        }
//                        if (apiBean.code == 200 && apiBean.data?.dbHealth?.dbStatus == "UP") onlineStatus = 0
//                    }
                    if (apiBean?.metrics != null && apiBean.dbHealth != null) {
                        if (apiBean.metrics?.httpCode == "200" && apiBean.dbHealth?.dbStatus == "UP") onlineStatus = 0
                    }
                }
                if (onlineStatus != kv.decodeInt(Constant.APP_ONLINE_STATUS, 0)) {
                    kv.encode(Constant.APP_ONLINE_STATUS, onlineStatus)
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_APP_ONLINE_STATUS, null))
                }
                delay(60000)
            }
            mutex.withLock { isOpenApi = false }
        }
    }
}