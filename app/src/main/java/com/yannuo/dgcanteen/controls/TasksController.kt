package com.yannuo.dgcanteen.controls

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.util.Constant
import java.util.concurrent.TimeUnit

class TasksController {

    companion object {
        val instance: TasksController by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            synchronized(TasksController::class.java) { TasksController() }
        }
    }

    //开启定时任务
    fun openTimingTask() {
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            ExecuteTimedTasks::class.java,
            30,
            TimeUnit.MINUTES
        ).build()
        //防止相互取消，添加标签，启动工作程序
        WorkManager.getInstance(MyApplication.applicationContext).enqueueUniquePeriodicWork(
            Constant.TIMING_TASKS,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }
}