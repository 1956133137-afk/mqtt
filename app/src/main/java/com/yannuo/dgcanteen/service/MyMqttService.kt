package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.StatusValue
import com.yannuo.dgcanteen.mqtt.InteractionBinder
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.supervisorScope
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.File
import java.io.FileReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


//交互过程可通过binder设置主题到达监听。以更新页面
class MyMqttService: Service(), NetworkStateManager.NetWorkListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binder : InteractionBinder
    private var TAG = javaClass.simpleName

    private lateinit var mqttStateListener : MqttConnectState
    private var pictureHost = "https://acms.yannuozhineng.com/api/"
    private var mDelay : Disposable?= null

    //订阅的主题
    private var TOPIC_TITLE = "device/"+ CommonAndDpToPxUtil.getDeviceSerial() //订阅本机专属主题
    private var topic_bossResult ="boss"
    private var faceAddTaskRunning = AtomicBoolean(false)  //人脸添加线程启动
    private var stopAddPeopleTask = false  //停止人员添加任务
    private lateinit var mStatusValue : StatusValue




    override fun onCreate() {
        super.onCreate()
        val handle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler $e ${e.message}")
        }
        val scope = CoroutineScope (Dispatchers.Default +handle)

        mStatusValue = StatusValue()

        binder = InteractionBinder(this)
        mqttStateListener = MqttConnectState()
        binder.registerListener(mqttStateListener)
        binder.connect()    //开启mqtt连接
        NetworkStateManager.getInstance().registerObserver(this);   //网络状态监听
//        pre?.register(this)
        LogUtil.d(TAG,"服务启动")
        //新版本检查任务
        checkNewApp()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }



    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }


    /**
     * 新app检查
     */
    private fun checkNewApp(){
       val work =  PeriodicWorkRequest.Builder(
            CheckVersionWorker::class.java,
            15,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(Constant.PERIODIC_WORK_KEY, ExistingPeriodicWorkPolicy.REPLACE,work)

    }


    override fun onDestroy() {
        WorkManager.getInstance(this).cancelUniqueWork(Constant.PERIODIC_WORK_KEY)
        LogUtil.d(TAG,"服务关闭")
        binder.unRegisterListener()
//        pre?.unregister(this)
        binder.disconnect()
        mDelay?.dispose()

        NetworkStateManager.getInstance().unRegisterObserver(this)
        super.onDestroy()
    }

    override fun netWorkStatus(statue: String?) {
        LogUtil.d(TAG,"网络状态( 0--有网 ，1无网络)--> $statue")
        if (statue.equals("0")) {
            binder.connect()

        }  //断线重连
        else{
            //修改更新标志
            stopAddPeopleTask = true
        }
    }

    inner class MqttConnectState : IMqttConnectState {
        override fun onConnectSuccess() {
            //订阅主题
            binder.subscribe(TOPIC_TITLE,1)
        }

        override fun onConnectFail(reason: String?) {}

        override fun onTopicArrive(topic: String?, message: MqttMessage?) {

        }

        override fun onConnectLost(reason: String?) {

        }

    }




    /**
     * 设备回调mqtt服务器
     * @param visitorInfo VisitorPeopleRecord
     */
    private fun deviceCallback(messageId :String ,name :String,number :String) {


    }




    private fun readPic(filename : String?) : String? {
        if (filename.isNullOrEmpty()) return null
        val file = File(filesDir, filename)
        if (file.exists()) {
            val fileReader = FileReader(file)
            val char = CharArray(1024)
            var size = -1
            val buffer = StringBuffer()
            do {
                size = fileReader.read(char)
                if (size != -1) {
                    buffer.append(char.copyOfRange(0, size))
                }
            } while (size != -1)
            return buffer.toString()
        }
        return null
    }

    private fun deletePic(filename : String?) {
        if (filename.isNullOrEmpty()) return
        val file = File(filesDir, filename)
        if (file.exists()) {
            file.delete()
            LogUtil.i(TAG, "删除健康记录图片，$filename")
        }
    }

    //监听设置值变化
    override fun onSharedPreferenceChanged(pre: SharedPreferences, key: String) {
        when(key){

        }
    }



}