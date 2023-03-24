package com.yannuo.dgcanteen.util

object Constant {
    //配置文件名
    const val fileName = "pre"
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
}