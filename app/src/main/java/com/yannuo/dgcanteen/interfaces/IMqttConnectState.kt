package com.yannuo.dgcanteen.interfaces

import org.eclipse.paho.client.mqttv3.MqttMessage

interface IMqttConnectState {

    //Mqtt连接成功
    fun onConnectSuccess()

    //Mqtt连接失败
    fun onConnectFail(reason :String?)

    //订阅主题已到达
    fun onTopicArrive(topic: String?, message: MqttMessage?)

    //Mqtt断开连接
    fun onConnectLost(reason :String? )

}