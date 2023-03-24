package com.yannuo.dgcanteen.mqtt

import android.content.Context
import android.os.Binder
import com.yannuo.dgcanteen.interfaces.IMqttConnectState

/**
 * 与Mqtt服务交互接口
 */
class InteractionBinder(context :Context) : Binder() {

    private var mqttClient : MqttClient = MqttClient(context)


    fun connect() {
        mqttClient.connect()
     }

    /**
     * 订阅 topic
     */
    fun subscribe(topic: String, qos : Int){
        mqttClient.subscribe(topic,qos)
    }

    /**
     * 取消订阅
     */
    fun unsubscribe(topic : String){
        mqttClient.unsubscribe(topic)
    }

    /**
     * 发布消息
     */
    fun publish(topic: String, msg: String, qos: Int, retained: Boolean = false) {
        mqttClient.publish(topic,msg,qos,retained)
    }

    /**
     * 断开连接
     */
    fun disconnect(){
        mqttClient.disconnect()
    }

    /**
     * 断线重连
     */
    fun reconnect(){
        mqttClient.reconnect()
    }

    fun registerListener(listener : IMqttConnectState){
        mqttClient.mListener = listener
    }

    fun unRegisterListener(){
        mqttClient.mListener = null
    }

    fun reConfiguration(){
        mqttClient.reConfiguration()
    }

    fun configChange(){
        mqttClient.autoConnect()
    }

}