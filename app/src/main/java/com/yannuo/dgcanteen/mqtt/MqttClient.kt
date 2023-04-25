package com.yannuo.dgcanteen.mqtt

import android.content.Context
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.concurrent.TimeUnit

class MqttClient(context: Context) {
    private var client :MqttAndroidClient? =null
    private var serverURI  =""

    private lateinit var clientid : String
    private val TAG = "MqttClient"
    private var option = MqttConnectOptions()
    var mListener : IMqttConnectState? = null
    private val retry = 5 //断线重连次数
    private var context = context
    private var disposable : Disposable?=null
    private var CONNECT_STATUS = ConnectStatue.DISCONNECT
    private var mOnline = true //网络状态，离线下将关闭重连，提供性能

    private enum class ConnectStatue{
        CONNECT,DISCONNECT,DISCONNECTING,CONNECTING
    }

    //发布
    private val topic_heart = "deviceHeart"
    private val topic_deviceStatus ="deviceStatus"



    init {
        config()
    }

    fun config(){
        val mv = MMKV.defaultMMKV()
        clientid = CommonAndDpToPxUtil.getDeviceSerial()
        option.keepAliveInterval = 60
        option.userName = mv.decodeString(Constant.MQTT_ACCOUNT)
        option.password = mv.decodeString(Constant.MQTT_PASSWORD)?.toCharArray()

        option.connectionTimeout = 20
        option.isCleanSession = false
        option.setWill(topic_deviceStatus,Gson().toJson(Status(0)).toByteArray(),1,false)  //设置遗嘱主题
        serverURI = mv.decodeString(Constant.MQTT_ADDRESS).toString()
        client = MqttAndroidClient(context,serverURI,clientid)
        client?.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                LogUtil.e(TAG,"Connection lost ${cause.toString()} ")
                CONNECT_STATUS = ConnectStatue.DISCONNECT
                if (disposable?.isDisposed == false){
                    disposable?.dispose() //停掉心跳主题
                }
                mListener?.onConnectLost(cause?.message)
                if (mOnline) reconnect()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                LogUtil.i(TAG,"Receive message: ${message.toString()} from topic: $topic")
                //todo
                try {
                    mListener?.onTopicArrive(topic,message)
                }catch (e : IllegalArgumentException){
                    e.printStackTrace()
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                LogUtil.i(TAG,"message arrived broker: ${token.toString()} ")
            }
        })
    }

    fun connect(){
        if (CONNECT_STATUS == ConnectStatue.CONNECTING || CONNECT_STATUS != ConnectStatue.DISCONNECT)return
        CONNECT_STATUS = ConnectStatue.CONNECTING
        Observable.just(1)
            .subscribeOn(Schedulers.io())
            .doOnNext {
                var retryTime = retry
                val finish = false
                while ((CONNECT_STATUS == ConnectStatue.CONNECTING)  && retryTime > 0 && mOnline) {
                    try {
                        if (!finish) {
                            client?.connect(option, null, object : IMqttActionListener {
                                override fun onSuccess(asyncActionToken: IMqttToken?) {
                                    LogUtil.i(TAG, "mqtt connection success!")
                                    CONNECT_STATUS = ConnectStatue.CONNECT
                                    //开启心跳推送
//                                    sendHeart()
                                    //推送设备状态主题（上下线）
                                    publish(topic_deviceStatus, Gson().toJson(Status(1)), 1)    //上线
                                    mListener?.onConnectSuccess()
                                }


                                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                    LogUtil.e(TAG, "mqtt connection failure ${exception.toString()}")
                                    mListener?.onConnectFail(exception.toString())
                                    retryTime--
                                    if (retryTime <= 0){
                                        retryTime = retry
                                    }
//                                    CONNECT_STATUS = ConnectStatue.DISCONNECT
                                }
                            })
                            Thread.sleep(TimeUnit.MINUTES.toMillis(2))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        connect()
                    }
                }
            }.subscribe()

    }

    @Synchronized
    fun autoConnect(){
        try {
//            CONNECT_STATUS = ConnectStatue.DISCONNECT
            disconnect()
            config()
        }catch (e :Exception){e.printStackTrace()}
        finally {
            connect()
        }
    }

    /**
     *
     * @param online Boolean 网络在线-true
     */
    fun changeNetwork(online : Boolean){
        if(online) CONNECT_STATUS = ConnectStatue.DISCONNECT
        mOnline = online
    }

    /**
     * 订阅 topic
     */
    fun subscribe(topic: String, qos : Int = 1){
        try {
            client?.subscribe(topic,qos,null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    LogUtil.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    LogUtil.e(TAG, "Failed to subscribe $topic")
                }

            })
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    /**
     * 取消订阅
     */
    fun unsubscribe(topic : String){
        try {
            client?.unsubscribe(topic,null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    LogUtil.d(TAG, "Unsubscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    LogUtil.e(TAG, "Failed to unsubscribe $topic")
                }
            })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * 发布消息
     */
    fun publish(topic: String, msg: String, qos: Int, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            client?.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    LogUtil.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    LogUtil.e(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * 断开连接
     */
    fun disconnect(){
        //已断开或者正断开直接返回
//        if (CONNECT_STATUS == ConnectStatue.DISCONNECTING || CONNECT_STATUS == ConnectStatue.DISCONNECT)return
        if (CONNECT_STATUS != ConnectStatue.CONNECT)return
//        CONNECT_STATUS = ConnectStatue.DISCONNECTING
        try {
            client?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    CONNECT_STATUS = ConnectStatue.DISCONNECT
                    LogUtil.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    CONNECT_STATUS = ConnectStatue.CONNECT
                    LogUtil.e(TAG, "Failed to disconnect")
                }
            })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * 断线重连
     */
    fun reconnect() {
        connect()
    }

    fun sendHeart(){
        disposable?.dispose()
        disposable = Observable.interval(60, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe {
                val gson = Gson()
                publish(topic_heart, gson.toJson(Heart()), 1, false)
                LogUtil.i(TAG,"心跳...")
            }
    }

    fun reConfiguration(){
        //已断开或者正断开直接返回
        if (CONNECT_STATUS == ConnectStatue.DISCONNECTING)return
        CONNECT_STATUS = ConnectStatue.DISCONNECTING
        try {

            client?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    LogUtil.d(TAG, "reConfiguration disconnected")
                    CONNECT_STATUS = ConnectStatue.DISCONNECT
                    config()
                    connect()
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    CONNECT_STATUS = ConnectStatue.CONNECT
                    LogUtil.e(TAG, "reConfiguration failed to disconnect")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    inner class Status(stat :Int){
        var status = stat //默认离线状态
        var deviceNum = CommonAndDpToPxUtil.getDeviceSerial()
    }

    inner class Heart{
        var heartTime = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss",System.currentTimeMillis()).toString()
        var deviceNum = CommonAndDpToPxUtil.getDeviceSerial()
        var versionName = MyApplication.applicationContext.packageManager.
        getPackageInfo(MyApplication.applicationContext.packageName,0).versionName

    }
}



