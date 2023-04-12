package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PaymentDishesList
import com.yannuo.dgcanteen.model.StatusValue
import com.yannuo.dgcanteen.model.SynConsumeRecordBean
import com.yannuo.dgcanteen.mqtt.InteractionBinder
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileReader
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


//交互过程可通过binder设置主题到达监听。以更新页面
class MyMqttService: Service(), NetworkStateManager.NetWorkListener{
    private lateinit var binder : InteractionBinder
    private var TAG = javaClass.simpleName
    private lateinit var mqttStateListener : MqttConnectState

    //订阅的主题
    private var TOPIC_TITLE = "device/"+ CommonAndDpToPxUtil.getDeviceSerial() //订阅本机专属主题

    private var runTask = true  //控制任务，无网络将睡眠
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

        //离线补扣
        offLineFillMoney()

        LogUtil.d(TAG,"服务启动")


    }


    /**
     * 上传消费记录到自有平台
     */
    @OptIn(ExperimentalTime::class)
    private fun synConsumerDish(){
        mScope.launch(mHandle) {
            while(isActive){
                LogUtil.i(TAG,"离线消费上传任务开始...")
                delay(Duration.hours(1))
                val offline =  MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
                if (offline)continue
                //在线模式下
                // 1、先复位上传标志
                do {
                    val dishList = DishesDBHelper.getInstance().extractConsumerOrder(true)
                    dishList.forEach {
                        it.up = false
                    }
                    DishesDBHelper.getInstance().updateConsumerOrders(dishList)
                }while (dishList.size == 100 && runTask)
                //2、上传记录
                val respository = PayRepositoryOfPay()
                val gson = Gson()
                do {
                    val order = DishesDBHelper.getInstance().queryConsumerOrder()
                    order?.also {
                        val js = gson.toJson(order)
                        LogUtil.d(TAG, js)
                        val bean = gson.fromJson(js, SynConsumeRecordBean::class.java)
                        bean.paymentDishesList = mutableListOf()
                        order.paymentDishesList.forEach {
                            bean.paymentDishesList.add(
                                PaymentDishesList(
                                    it.dishesId,
                                    it.dishesName,
                                    it.dishesNumber,
                                    it.dishesPrice
                                )
                            )
                        }
                        val res = respository.synCsRecord(bean)
                        if (res.code == HttpURLConnection.HTTP_OK){
                            //删除对应的消费记录的菜品
                            DishesDBHelper.getInstance().deleteRelatedDish(it.paymentDishesList[0].orderid)
                            //删除对应的消费记录
                            DishesDBHelper.getInstance().deleteConsumerOrder(it.ordeR_ID)
                            LogUtil.i(TAG,"离线订单${bean.ORDER_ID} 上传成功!")
                        }else{
                            LogUtil.e(TAG,"离线上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                            //修改上传标志
                            it.up = true
                            DishesDBHelper.getInstance().updateConsumerOrder(it)
                        }
                    }
                }while (order != null && runTask)
                LogUtil.i(TAG,"离线消费上传任务结束...")
            }
        }
    }

    /**
     * 恢复网络并且不是离线模式离线补扣
     */
    private fun offLineFillMoney(){

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
            runTask = true
        }  //断线重连
        else{
            //修改更新标志
            binder.disconnect()
            runTask = false
        }
    }

    inner class MqttConnectState : IMqttConnectState {
        override fun onConnectSuccess() {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH,true))
            //订阅主题
            binder.subscribe(TOPIC_TITLE,1)
        }

        override fun onConnectFail(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH,false))
        }

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



}