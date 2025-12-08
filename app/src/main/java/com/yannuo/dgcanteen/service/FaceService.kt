package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.yannuo.dgcanteen.download.FaceDataWorker
import com.yannuo.dgcanteen.util.Constant
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FaceService : Service() {

    override fun onCreate() {
        super.onCreate()
        uploadFaceData()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 定时增量人脸特征值
     */
    private fun uploadFaceData() {
        Log.d("TAG", "uploadFaceData: 开启人脸定时任务")
        val work = PeriodicWorkRequest.Builder(
            FaceDataWorker::class.java,
            240L + Random.nextInt(30),
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                Constant.FACE_DATA_UPLOAD,
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
    }
}