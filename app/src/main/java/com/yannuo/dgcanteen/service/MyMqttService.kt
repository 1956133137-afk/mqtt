package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.StatusValue
import com.yannuo.dgcanteen.mqtt.InteractionBinder
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.File
import java.io.FileReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


//交互过程可通过binder设置主题到达监听。以更新页面
class MyMqttService: Service(), NetworkStateManager.NetWorkListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binder : InteractionBinder
    private var TAG = javaClass.simpleName
    private lateinit var mqttStateListener : MqttConnectState

    //订阅的主题
    private var TOPIC_TITLE = "device/"+ CommonAndDpToPxUtil.getDeviceSerial() //订阅本机专属主题

    private var stopAddPeopleTask = false  //停止人员添加任务
    private lateinit var mStatusValue : StatusValue
    private lateinit var mScope : CoroutineScope
    private lateinit var mHandle : CoroutineExceptionHandler




    override fun onCreate() {
        super.onCreate()
        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler $e ${e.message}")
        }
        mScope = CoroutineScope (Dispatchers.Default + mHandle)

        mScope.launch(mHandle) {
            mStatusValue = StatusValue()

            binder = InteractionBinder(this@MyMqttService)
            mqttStateListener = MqttConnectState()
            binder.registerListener(mqttStateListener)
            binder.connect()    //开启mqtt连接


            //打开扫码器
            ScanDevice.openScan()
            //新版本检查任务
            checkNewAppAndKeepAlive()
            //网络状态监听
            NetworkStateManager.getInstance().registerObserver(this@MyMqttService)
            //
        }

        //同步消费记录
        synConsumerDish()

        LogUtil.d(TAG,"服务启动")


    }





    @OptIn(ExperimentalTime::class)
    private fun synConsumerDish(){
        mScope.launch(mHandle) {
            while(isActive){
                
                val offline =  MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
                if (offline)continue
                delay(Duration.hours(1))
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }



    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }


    /**
     * 新app检查,和开启软件保活
     */
    private fun checkNewAppAndKeepAlive(){
        val work =  PeriodicWorkRequest.Builder(
            CheckVersionWorker::class.java,
            15,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(Constant.PERIODIC_WORK_KEY, ExistingPeriodicWorkPolicy.REPLACE,work)
        LogUtil.i(TAG,"启动软件版本更新任务")

        // JobScheduler 拉活
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeepAliveJobService.startJob(this)
            LogUtil.i(TAG,"开启软件保活设置")
        }
    }


    override fun onDestroy() {
        WorkManager.getInstance(this).cancelUniqueWork(Constant.PERIODIC_WORK_KEY)
        LogUtil.d(TAG,"服务关闭")
        //取消mqtt监听
        binder.unRegisterListener()
        //断开mqtt
        binder.disconnect()

        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)
        //关闭扫码头
        ScanDevice.closeScan();
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