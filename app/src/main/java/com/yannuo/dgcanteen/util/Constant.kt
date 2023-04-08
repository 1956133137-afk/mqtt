package com.yannuo.dgcanteen.util

object Constant {
    //配置文件名
    const val fileName = "mmkv"
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
    const val ADDRESS = "Address"

    //是否离线
    const val SWITCH = "Switch"

    //mqtt服务地址
    const val MQTT_ADDRESS = "MqttAddress"

    //mqtt账号
    const val MQTT_ACCOUNT = "MqttAccount"

    //mqtt密码
    const val MQTT_PASSWORD = "MqttPassword"

    //菜品数据最后同步时间
    const val FINAL_TIME = "FinalTime"

    //当前版本
    const val VERSION = "Version"
    //菜品更新时间
    const val UPDATE_TIME = "update_time"
    const val update_time = "19700000"  //默认1970年



    /************** EventBus *****************/
    const val EVENT_FIRST = 1 //取餐
    const val EVENT_SECOND = 2 // 刷脸支付
    const val EVENT_THIRD = 3 // 返回点餐界面
}