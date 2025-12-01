package com.yannuo.dgcanteen.download

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.util.Constant
import java.text.SimpleDateFormat
import java.util.Locale

class FaceDataWorker(cnt : Context, params : WorkerParameters) : Worker(cnt,params) {
    private val kv = MMKV.defaultMMKV()

    override fun doWork(): Result {
        downloadFace()
        return Result.success()
    }

    /**
     * 定时增量更新人脸数据
     */
    private fun downloadFace(){
        Log.d("TAG", "downloadFace: 人脸数据增量更新")
        val decodeString = kv.decodeString(Constant.LOCAL_FACE_UPLOAD_DATE) ?: ""
        DownloadVM.instance.downUserFaceDBImg(decodeString)

    }
}