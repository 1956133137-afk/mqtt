package com.yannuo.dgcanteen.util

object Constant {
    //配置文件名
    const val fileName = "config"
    const val strDefault  =""
    //事件通知
    //mqtt地址
    const val mqttAddressKey = "mqtt"
    const val mqttAddressValue = "tcp://acms.yannuozhineng.com:3883"

    //mqtt账号
    const val mqttAccountKey = "mqtta"
    const val mqttAccountValue = "acms"

    //mqtt密码
    const val mqttPassworkKey = "mqttp"
    const val mqttPassworkValue = "ACMS2022~!@"

    //周期任务
    const val PERIODIC_WORK_KEY = "app-update"

    const val BROADCAST_ACTION  = "com.yannuo.mqtt.state"   //mqtt连接广播

    //服务器地址
    const val Address = ""

    //是否离线
    const val Switch = false

    //mqtt服务地址
    const val MqttAddress = ""

    //mqtt账号
    const val MqttAccount = ""

    //mqtt密码
    const val MqttPassword = ""

    //当前版本
    const val Version = ""


    /************** EventBus *****************/
    const val EVENT_FIRST = 1 //取餐
    const val EVENT_SECOND = 2 // 刷脸支付
}