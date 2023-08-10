package com.yannuo.dgcanteen.util

object Constant {



    //配置文件名
//    const val fileName = "mmkv"
//    const val strDefault  =""
    //事件通知
    //mqtt地址
//    const val mqttAddressKey = "mqtt"
//    const val mqttAddressValue = "tcp://acms.yannuozhineng.com:3883"
    //mqtt账号
//    const val mqttAccountKey = "mqtta"
//    const val mqttAccountValue = "acms"
    //mqtt密码
//    const val mqttPassworkKey = "mqttp"
//    const val mqttPassworkValue = "ACMS2022~!@"
    //周期任务-检查软件版本
    const val PERIODIC_WORK_KEY = "app-update-task"
//    const val BROADCAST_ACTION  = "com.yannuo.mqtt.state"   //mqtt连接广播
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
    //支付配置信息
    const val PAY_CONFIG = "PCfg"
    //当前版本
    const val VERSION = "Version"
    //支付结果显示时间
    const val SHOW_TIME = "show_time"
    //菜品更新更新标志
    const val UPDATE_TIME = "update_time"
    const val update_time = "19700000"  //默认1970年
    const val PIC_DIR = "pic" //菜品图片保存目录
    const val GROUP_NAME = "yannuoface" //人脸特征库
    const val PERSONINFO_TIME ="p_i_time "  //人员数据更新时间，定时更新人员信息
//    const val CURRENT_PAGE = "page" //当前人员下载页

    const val AWAIT_PAY_TIME = "awaitPayTime" //等待支付时间

    const val APP_MODE = "appMode" //点餐模式 、付款模式、未设置
    const val ORDERING_FOOD_MODE = "点餐模式"
    const val PROCEEDS_MODE = "收款模式"

    lateinit var CORP_ID :String  //合作方ID
    lateinit var CCB_API_PATH :String  //ccb开放平台接口地址
    lateinit var STR_KEY :String  //扫码解码密码
    lateinit var CIPHER :String  //离线码解码密码
    lateinit var ENCRYPTION_VECTOR :String  //离线码加密向量

    const val QUOTA_SWITCH = "Quota_Switch" //是否定额收款
    const val QUOTA_AMOUNT = "Quota_Amount" //定额收款金额

    const val LIMIT_AMOUNT = "limitAmount"  //单笔最高收款
    const val TITLE_CONTENT = "titleContent"//副屏标题设置

    /************** EventBus *****************/
    const val EVENT_FIRST = 1 //取餐
    const val EVENT_SECOND = 2 // 刷脸支付
    const val EVENT_FOURTH = 3 //被扫支付
    const val EVENT_THIRD = 4 // 返回点餐界面
    const val EVENT_FIFTH = 5 //菜品更新
    const val EVENT_NINTH = 9 //mqtt设置变更
    const val EVENT_TENTH = 10 //mqtt连接状态
    const val EVENT_OFF_CHANGE = 21 //离线模式改变
    const val EVENT_QUOTA_CHANGE = 22 //定额模式改变


    //手输入金额使用的配置
    //1)状态码
    const val NO_ERROR = 1000
    const val OPEN_CAMERA_ERROR_TYPE = 1001 //相机打开失败


    //支付方式
    const val PAY_MODE = "payMode"
    const val PAY_FACE_TYPE = 0     //刷脸
    const val PAY_IC_TYPE = 1       //刷卡
    const val PAY_CODE_TYPE = 2     //扫码
    const val PAY_CODE_IC_TYPE = 3  //码卡

    //传递参数
    const val PAY_DATE = "data"  //支付信息
    const val PAY_RESULT = "result" //支付结果信息
    const val FIRST_START = "first" //应用首次启动

    //2）设置配置
    const val YAW_SET = "yaw" //相机偏航角
    const val YAW_SET_V = 25f //相机偏航角
    const val PITCH_SET = "pith" //相机俯仰角
    const val PITCH_SET_V = 25f //相机俯仰角
    const val ROLL_SET = "roll" //相机翻滚角
    const val ROLL_SET_V = 25f //相机翻滚角
    const val ROTATE_SET = "rotate" //相机旋转角
    const val ROTATE_SET_V = 0 //相机旋转角
    const val PRE_ANGLE_SET = "pv_ag" //预览角度
    const val PRE_ANGLE_SET_V = 0 //预览角度
    const val MIRROR_SET = "mirror" //相机镜像
    const val MIRROR_SET_V = false //相机镜像
    const val LIVE_VALUE_SET = "live_vl" //活检阈值
    const val LIVE_VALUE_SET_V = 70f //活检阈值
    const val LIVE_ENABLE_SET = "live_en"  //活检开关
    const val LIVE_ENABLE_SET_V = false  //活检默认关闭
    const val RECOGNIZE_VALUE_SET = "regn_vl" //识别阈值
    const val RECOGNIZE_VALUE_SET_V = 70f //识别阈值
    const val DISTANCE_SET = "distance" //识别距离阈值
    const val DISTANCE_SET_V = 0.8f //识别距离阈值(米)
    const val VOICE_ENABLE_SET = "voice_en" //人脸识别语音播报
    const val VOICE_ENABLE_SET_V = true //人脸识别语音播报


}