package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.yannuo.dgcanteen.controls.TasksController
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyService : Service() {
    private val TAG = javaClass.simpleName

    private val mHandle = CoroutineExceptionHandler { coroutineContext, e ->
        LogUtil.e(TAG, "Exception: ${e.message}")
        e.printStackTrace()
    }
    private val mScope = CoroutineScope(Dispatchers.Default + mHandle)

    override fun onCreate() {
        super.onCreate()
        LogUtil.d(TAG, "MyService服务开启")
        mScope.launch {
            TasksController.instance.openTimingTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(TAG, "关闭服务")
        mScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}