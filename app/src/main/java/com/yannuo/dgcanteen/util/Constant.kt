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
    const val PERSON_ADDRESS = "personAddress"  //人员信息下载地址

    //是否离线
    const val SWITCH = "Switch"

    //启用打印机
    const val EN_PRINTER = "en_printer"

    //mqtt服务地址
    const val MQTT_ADDRESS = "MqttAddress"

    //mqtt账号
    const val MQTT_ACCOUNT = "MqttAccount"

    //mqtt密码
    const val MQTT_PASSWORD = "MqttPassword"

    //卡号格式
    const val CARD_FORMAT = "cardFormat"

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
    const val PERSONINFO_TIME = "p_i_time "  //人员数据更新时间，定时更新人员信息

    //    const val CURRENT_PAGE = "page" //当前人员下载页
    const val SERIALPORT = "sn"

    const val AWAIT_PAY_TIME = "awaitPayTime" //等待支付时间

    const val APP_MODE = "appMode" //点餐模式 、付款模式、未设置
    const val ORDERING_FOOD_MODE = "点餐模式"
    const val PROCEEDS_MODE = "收款模式"
    const val ORDERING_TWO_MODE = "点餐模式2"
    const val ORDERING_MEAL_MODE = "订餐模式"
    const val PROCEEDS_TWO_MODE = "收款模式2"
    const val ORDERING_VERIFY_MODE = "订餐核销"
    const val MEAL_PREPARATION_MODE = "备餐模式"

    lateinit var CORP_ID: String  //合作方ID
    lateinit var CCB_API_PATH: String  //ccb开放平台接口地址
    lateinit var STR_KEY: String  //扫码解码密码
    lateinit var CIPHER: String  //离线码解码密码
    lateinit var ENCRYPTION_VECTOR: String  //离线码加密向量

    const val QUOTA_SWITCH = "Quota_Switch" //是否定额收款
    const val BALANCE_SWITCH = "balance_Switch" //余额查询
    const val QUOTA_AMOUNT = "Quota_Amount" //定额收款金额
    const val QUOTA_TIME_DEFAULT_AMOUNT = "quotaTimeDefaultAmount"  //分时段默认定额收款金额

    const val LIMIT_AMOUNT = "limitAmount"  //单笔最高收款
    const val TITLE_CONTENT = "titleContent"//副屏标题设置
    const val CODE_VERIFICATION_SET = "code_vts" //核销模式
    const val MEAL_TIME = "meal_time" //显示核销菜品倒计时
    const val QUERY_VERIFY = "query_verify" //核销查询
    const val AUTO_VERIFY = "auto_verify"   //自动核销
    const val VERIFY_CHANGE = "verify_change" //核销变量(防止二次刷脸)
    const val AUTO_PAY = "auto_pay"   //自动收款
    const val SUPPORT_PAY = "support_pay"  //副屏支付按钮显示
    const val DISPLAY_CARD_VERIFY = "displayCardVerify" //副屏刷卡核销按钮
    const val VERIFY_PERSON_STATISTIC = "verifyPersonStatistic" //核销人数统计

    const val ORDER_VERIFY_IC_CARD = "order_verify_ic_card" //订餐核销默认刷卡

    const val REPEAT_PAY_JUDGE = "repeatPayJudge"   //是否判断重复支付 默认开启

    const val ORDER_LOGIN_FACE = "orderLoginFace"   //刷脸登录 Boolean
    const val ORDER_LOGIN_CODE = "orderLoginCode"   //扫码登录 Boolean
    const val ORDER_LOGIN_CARD = "orderLoginCard"   //刷卡登录 Boolean
    const val ORDER_QUERY = "orderQuery"    //订餐查询 Boolean
    const val ORDER_PRINTER_FORMAT = "OrderPrinterFormat"   //订餐打印格式 0-一天 1-全部
    const val ORDER_ADVANCE_DAY = "orderAdvanceDay"     //订餐提前天数
    const val ORDER_DEFAULT_WAY = "orderDefaultWay"     //订餐默认方式 （自提+核销）
    const val ORDER_DISCOUNT_SWITCH = "orderDiscountSwitch" //订餐优惠开关
    const val ORDER_MEAL_LIMIT = "orderMealLimit"               //订餐限制是否与后台一致 Boolean
    const val ORDER_MEAL_LIMIT_SWITCH = "orderMealLimitSwitch"  //订餐餐别限制开关
    const val ORDER_MEAL_LIMIT_SIZE = "orderMealLimitSize"      //订餐餐别限制份数
    const val ORDER_MEAL_SIZE = "orderMealSize" //已订餐别份数
    const val ORDER_VERIFY_CONFIRM = "orderVerifyConfirm"   //订餐核销确认 Boolean

    const val USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH = "收款模式受开餐状态限制" // 开餐后才能收款  0：关闭   1：打开
    const val IS_USE_MEAL = "是否开餐" // 0: 未开餐  1：开餐

    const val MEAL_TIME_SWITCH = "mealTimeSwitch" //餐次按钮是否显示 0:不显示 1:显示
    const val MEAL_TIME_MODE = "餐次消费模式" // 0：关闭  1：打开  目前只有申万宏源使用，目前只做了在线模式的
    const val QUERY_TIME_SWITCH = "查询余次开关" //查询余次开关  0：关闭  1：打开
    const val PAY_RESULT_DIALOG_TIME = "支付结果dialog显示时间"
    const val MEAL_TIME_PAY_RESULT_TIME = "餐次模式消费结果展示时间"
    const val MEAL_TIME_QUERY_BALANCE_TIME = "餐次模式查询余次结果展示时间"

    const val BTN_CONFIRM_STATE = "确认金额按钮状态" // 0：关闭  1：打开
    const val QUICK_SWITCH_MODE = "quickSwitchMode" //快捷切换模式

    const val WHETHER_SHOW_PAYMENT = "whetherShowPayment"   //是否显示交易金额信息 Boolean

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
    const val EVENT_OTHER_PAY = 23 //其它支付方式收款
    const val EVENT_VERIFY = 24 //校验金额
    const val EVENT_OPEN_BTN = 25 //打开支付按钮
    const val EVENT_OFLINE_CHANGE = 26 //
    const val EVENT_FACE = 28 //刷脸核销
    const val EVENT_CODE = 29 //二维码核销
    const val EVENT_VERIFY_CHANGE = 30 //核销统计
    const val EVENT_TRAN_MODE = 32  //交易方式
    const val EVENT_FACE_STATUS = 33    //刷脸状态改变
    const val EVENT_MEAL_TIME_BULK_PAY = 34 //餐次模式，零点(键盘输入金额)支付
    const val EVENT_SHOW_BULK_PAYMENT = 35 //显示零点支付的金额
    const val EVENT_QUIT_CONFIRM = 36  // 取消确定金额
    const val EVENT_KEYBOARD_CANCEL = 37 // 清空键盘的金额
    const val EVENT_SHOW_CALCULATE_AWAIT_DIALOG = 38 //显示calculate activity的 await dialog
    const val EVENT_DISMISS_CALCULATE_AWAIT_DIALOG = 39 //消失calculate activity的 await dialog
    const val EVENT_SHOW_CALCULATE_PAY_DIALOG = 40     //显示calculate activity的 pay result dialog
    const val EVENT_DISMISS_CALCULATE_PAY_DIALOG = 41   //消失calculate activity的 pay result dialog
    const val EVENT_MEAL_TIME_MODE = 42 //餐次模式
    const val UPDATE_MEAL_TIME_BILL = 43 // 更新calculate activity的订单量信息
    const val EVENT_MEAL_TIME_SWITCH = 44   //餐次按钮状态

    const val VERIFY_MODE = "verifyMode"//核销方式
    const val PRINTER_PATH_SET = "printer_path"  //打印机路径
    const val PRINTER_BAUD_SET = "printer_baud"  //打印机波特率
    const val PRINTER_UPDATE_TIME = "printerUpdateTime" //打印更新时间
    const val PRINTER_AMOUNT = "printerAmount"          //打印数量
    const val PRINTER_TICKET_NAME = "printerTicketName" //票名
    const val PRINTER_CASHIER_NAME = "printerCashierName"   //收银名

    const val PRINTER_VERIFY = "printerVerify"          //核销打印小票序号

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

    //餐次消费方式
    const val MEAL_TIME_FACE_TYPE = 4     //刷脸餐次
    const val MEAL_TIME_IC_TYPE = 5       //刷卡餐次
    const val MEAL_TIME_CODE_TYPE = 6     //扫码餐次

}