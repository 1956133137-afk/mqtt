package com.yannuo.dgcanteen.download

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import java.text.SimpleDateFormat
import java.util.Locale

class FaceDataWorker(cnt : Context, params : WorkerParameters) : Worker(cnt,params) {

    override fun doWork(): Result {
        downloadFace()
        return Result.success()
    }

    /**
     * 定时增量更新人脸数据
     */
    private fun downloadFace(){
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(System.currentTimeMillis())
        Log.d("TAG", "downloadFace: 人脸数据更新")
        DownloadVM.instance.downUserFaceDBImg(format)
    }
}