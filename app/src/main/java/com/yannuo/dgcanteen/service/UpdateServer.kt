package com.yannuo.dgcanteen.service

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import com.proembed.service.MyService
import com.yannuo.dgcanteen.util.LogUtil
import java.io.File


class UpdateServer : Service() {
    private val TAG ="UpdateServer"
    private var downloadId = -1L
    private var receiver : UpdateStateBroadcastReceiver? =null //广播接收器
    private var context:Context = this
    private val updateFileName = "dgcanteen.apk"


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("url")?.let {
            if (it.isEmpty())return@let
            downApk(it)
        }
        return START_REDELIVER_INTENT
    }

    private fun downApk(url: String?) {
        if (url.isNullOrEmpty()){
            LogUtil.i(TAG,"APK下载地址空！")
            return
        }
        val absoluteFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absoluteFile
        if (absoluteFile!= null){
            val file = File("${absoluteFile.absolutePath}/$updateFileName")
            if (file.exists()) {
                file.delete()
            }
        }
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setTitle("新版本下载")
        request.setDescription("餐厅功能有更新")
        request.setAllowedOverRoaming(true)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,updateFileName)
        downloadId = manager.enqueue(request)

        // 注册广播接收者，监听下载状态
        if (receiver == null) {
            receiver = UpdateStateBroadcastReceiver()
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    // 广播监听下载的各个状态
    private inner class UpdateStateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            checkStatus()
        }
    }

    // 检查下载状态
    @SuppressLint("Range")
    private fun checkStatus() {
        if (downloadId == -1L)return
        val query = DownloadManager.Query()
        // 通过下载的id查找
        query.setFilterById(downloadId)
        downloadId = -1L
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val cursor: Cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    // 下载完成后静默安装APK
                    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath+File.separator+updateFileName
                    val mXService = MyService(context)
                    mXService.silentInstallApk(path,"com.yannuo.dgcanteen",true)
                    cursor.close()
                    stopSelf()
                }
                DownloadManager.STATUS_FAILED -> {
                    cursor.close()
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        if (receiver!=null)
            unregisterReceiver(receiver)
        LogUtil.i(TAG,"UpdateServer on destroy!")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}