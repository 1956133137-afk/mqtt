package com.yannuo.dgcanteen.download

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.BuildConfig
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.model.MessageEvent

import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.service.UpdateServer
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil


class CheckVersionWorker(cnt : Context, params :WorkerParameters) :Worker(cnt,params) {
    private val cnt = cnt
    private val TAG = javaClass.simpleName
    private val CHECK_APP_URL = "https://acms.yannuozhineng.com/api/deviceData/checkLatestVersion"

    @SuppressLint("CheckResult")
    override fun doWork(): Result {
        //TODO 填入申请的appid
        val info = AppInfoB("XH2JVY4K5T4XU4IV",
            BuildConfig.CHANNEL.toString(),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString(),
            CommonAndDpToPxUtil.getDeviceSerial())
        LogUtil.i(TAG,"请求信息: $info")
        RetrofitClient.getApi().checkAppUpdate(CHECK_APP_URL,info)
            .subscribe( {
                if (it.code == 200) {
                    it.data?.needUpgrade?.let { sit ->
                        if (!sit) return@let
                        LogUtil.i(TAG,"新版本 \n版本名:${it?.data?.version} " +
                                "\n版本号:${it.data?.versionCode} \n更新说明:${it.data?.versionDesc}")
                        val intent = Intent(cnt, UpdateServer::class.java)
                        intent.putExtra("url",it.data?.fileUrl)
                        cnt.startService(intent)
                    }
                }
            },{
                LogUtil.e(TAG,"新版本检测:${it.message}")
            })
        return Result.success()
    }
}