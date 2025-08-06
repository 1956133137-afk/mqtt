package com.yannuo.dgcanteen.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.text.format.DateFormat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.presenters.DataPresenter
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.*
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.mqtt.InteractionBinder
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

//交互过程可通过binder设置主题到达监听。以更新页面
class MyMqttService : Service(), NetworkStateManager.NetWorkListener {
    private lateinit var binder: InteractionBinder
    private var TAG = javaClass.simpleName
    private lateinit var mqttStateListener: MqttConnectState
    private val gson = Gson()

    //订阅的主题
    private var TOPIC_TITLE = "ccb/pub/dishes/${CommonAndDpToPxUtil.getDeviceSerial()}" //订阅本机专属主题

    private var runTask = true  //控制任务，无网络将睡眠

    private lateinit var mScope: CoroutineScope
    private lateinit var mHandle: CoroutineExceptionHandler
    private lateinit var mRespository: PayRepositoryOfPay
    private lateinit var mDataPresenter: DataPresenter


    override fun onCreate() {
        super.onCreate()
        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "Exception: ${e.message}")
        }
        mScope = CoroutineScope(Dispatchers.Default + mHandle)

        mRespository = PayRepositoryOfPay()
        mScope.launch {
            mDataPresenter = DataPresenter()
            binder = InteractionBinder(this@MyMqttService)
            mqttStateListener = MqttConnectState()
            binder.registerListener(mqttStateListener)
            binder.connect()    //开启mqtt连接
            // 数字键盘
            KeyboardUtil.instance.closeKeyboard()
            KeyboardUtil.instance.openKeyboard()

            //连接打印机
            USBPrinterHelper.instance.queryPrinter()
            //打开扫码器
//            ScanDevice.openScan()
            //新版本检查任务
            checkNewAppAndKeepAlive()
            //网络状态监听
            NetworkStateManager.getInstance().registerObserver(this@MyMqttService)
            //获取配置
            getPayCfg()
        }

        synConsumerDish()//同步消费记录
        offLineFillMoney() //离线补扣
        downPerson()     //下载人员
        upDataDishes()   //定时同步餐别和菜品图片、菜品价格
        LogUtil.d(TAG, "服务启动")
        checkNetPass()
        deleteNonTodayRecords()
    }


    private suspend fun getPayCfg() {
        val result = mRespository.getPayCfg()
        LogUtil.d(TAG, "${Gson().toJson(result)} -> ${CommonAndDpToPxUtil.getDeviceSerial()}")
        if (result.code == "200") {
            val mv = MMKV.defaultMMKV()
            mv.encode(Constant.PAY_CONFIG, result.data)
            LogUtil.i(TAG, "已更新配置信息！")
        } else {
            LogUtil.w(TAG, "更新配置信息失败！")
        }
    }


    @OptIn(ExperimentalTime::class)
    private fun checkNetPass() {
        mScope.launch {
            while (isActive) {
                delay(Duration.minutes(5))
                if (NetworkStateManager.getInstance().isOnline(this@MyMqttService)) {
                    if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) {
                        MMKV.defaultMMKV().encode(Constant.SWITCH, false)
                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFLINE_CHANGE))
                    }
                }
            }
        }
    }


    /**
     * 上传消费记录到自有平台
     */
    @OptIn(ExperimentalTime::class)
    private fun synConsumerDish() {
        mScope.launch {
            while (isActive) {
                LogUtil.i(TAG, "消费记录上传任务开始...")
                delay(Duration.minutes(30))
//                delay(Duration.seconds(30))
                val offline = MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
                if (offline) continue
                //在线模式下
                val payOrderToAll = DishesDBHelper.getInstance().queryPayOrderToAll()
                payOrderToAll.forEach { order ->
                    val bean = SynConsumeRecordBean().apply {
                        deviceSerialNumber = order.deviceId
                        businessId = order.businessId
                        campusId = order.campusId
                        counterId = order.vposId
                        consumptionType = order.payType
                        RESULT = order.result
                        CUST_ID = order.custId
                        PAYMENT = order.payment
                        ACTUAL_PAYMENT = order.actualPayment ?: "0.0"
                        ACC_NO = order.accNo
                        ACC_BAL = order.accBal
                        ACC_TYPE = order.accType
                        TRACEID = order.traceId
                        ORDER_ID = order.orderId
                        TRAN_RESULT = order.tranResult
                        OFFLINE = order.offline
                        ERRCODE = order.errCode
                        ERRMSG = order.errMsg
                        order.accList.forEach {
                            val acclist = ACCLIST().apply {
                                ACC_NO = it.acC_NO
                                ACC_BAL = it.acC_BAL
                                ACC_TYPE = it.acC_TYPE
                                TRAN_ID = it.traN_ID
                                PAYMENT = it.payment
                            }
                            ACC_LIST.add(acclist)
                        }
                        PAYTIME = order.payTime
                        BUSINESS_NAME = order.businessName
                    }
                    order.paymentDishesList.forEach { dish ->
                        bean.paymentDishesList.add(Gson().fromJson(Gson().toJson(dish), Dish::class.java))
                    }
                    order.accList.forEach {
                        bean.ACC_LIST.add(Gson().fromJson(Gson().toJson(it), ACCLIST::class.java))
                    }
                    val res = mRespository.synCsRecord(bean)
                    if (res.code == "200") {
                        order.flag = 1
                        DishesDBHelper.getInstance().updatePayOrder(order)
                        LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
                    } else LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                }
                LogUtil.i(TAG, "消费记录上传任务结束...")
            }
        }
    }

    /**
     * 恢复网络并且不是离线模式离线补扣
     */
    @OptIn(ExperimentalTime::class)
    private fun offLineFillMoney() {
        mScope.launch {
            while (isActive) {
                delay(Duration.minutes(40))
                if (runTask && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) { //有网并且不为离线状态
                    val offlineOrder = DishesDBHelper.getInstance().queryOfflineOrderToAll()
                    LogUtil.i(TAG, "离线订单补扣开始请求...")
                    offlineOrder.forEach { offline ->
                        val payForUI = Gson().fromJson(Gson().toJson(offline), PayForUI::class.java)
                        payForUI.payTime = TimeUtil.dateFormat(offline.signTime)
                        payForUI.payDate = TimeUtil.formatDate(offline.signTime)
                        offline.paymentDishes.forEach { payForUI.paymentDishes.add(Gson().fromJson(Gson().toJson(it), Dish::class.java)) }
                        val response = when (payForUI.payType) {
                            "2" -> {
                                val request = Gson().fromJson(Gson().toJson(payForUI), CodePayBean::class.java)
                                request.qrCode = payForUI.payContent
                                val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                                mRespository.payByQrCode(encryption)
                            }
                            "3" -> {
                                val request = Gson().fromJson(Gson().toJson(payForUI), CardPayBean::class.java)
                                request.cardId = payForUI.payContent
                                val encryption = DES3CBCUtil.encryption(Gson().toJson(request))
                                mRespository.payByIcCard(encryption)
                            }
                            else -> CanteenResponse<String>()
                        }
                        if (response.code == "200") {
                            val decryptStr = DES3CBCUtil.decryptRSA(response.data ?: "")
                            val result = Gson().fromJson(decryptStr, ResponsePay::class.java)
                            payForUI.result = result.RESULT
                            payForUI.accType = result.ACC_TYPE
                            payForUI.accNo = result.ACC_NO
                            payForUI.accBal = result.REMAIN_BAL.ifEmpty { result.ACC_BAL }
                            result.ACC_LIST.forEach {
                                val acclist = ACCLIST().apply {
                                    ACC_NO = it.ACC_NO
                                    ACC_BAL = it.ACC_BAL
                                    ACC_TYPE = it.ACC_TYPE
                                    TRAN_ID = it.TRAN_ID
                                    PAYMENT = it.PAYMENT
                                }
                                payForUI.accList.add(acclist)
                            }
//                            payForUI.accList = result.ACC_LIST
                            payForUI.actualPayment = result.ACTUAL_PAYMENT
                            payForUI.orderId = result.ORDERID
                            payForUI.traceId = result.TRACEID
                            payForUI.errCode = result.ERRCODE
                            payForUI.errMsg = result.ERRMSG
                        } else {
                            payForUI.errCode = response.code
                            payForUI.errMsg = response.msg
                        }
                        if (payForUI.result == "Y") {
                            offline.flag = 1
                            DishesDBHelper.getInstance().updateOfflineOrder(offline)
                            saveOrderRecord(payForUI, offline.sessionId)
                        }
                        LogUtil.d(TAG, Gson().toJson(payForUI))
                    }
                    LogUtil.i(TAG, "离线订单补扣请求结束...")
                }
            }
        }
    }

    private fun saveOrderRecord(payForUI: PayForUI, session: String) {
        val order = DishesDBHelper.getInstance().queryPayOrder(session) ?: return
//        order.accNo = payForUI.accNo
//        order.accBal = payForUI.accBal
        order.accType = payForUI.accType
//        order.accList = payForUI.accList
        order.orderId = payForUI.orderId
        order.traceId = payForUI.traceId
        order.actualPayment = payForUI.actualPayment
        order.flag = 1
        DishesDBHelper.getInstance().updatePayOrderById(order)
    }

    private fun deleteNonTodayRecords() {
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        LogUtil.d(TAG, "删除非今日记录...$date")
        //消费订单
        DishesDBHelper.getInstance().deletePayOrderByPayDate(date)
        DishesDBHelper.getInstance().deleteSwPayOrderByPayDate(date)
        //离线订单
        DishesDBHelper.getInstance().deleteOfflineOrderByPayDate(date)
        //核销记录
        DishesDBHelper.getInstance().deleteVerifyUser(date)
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
    private fun checkNewAppAndKeepAlive() {
        val work = PeriodicWorkRequest.Builder(
            CheckVersionWorker::class.java,
            15,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(Constant.PERIODIC_WORK_KEY, ExistingPeriodicWorkPolicy.REPLACE, work)
        LogUtil.i(TAG, "启动软件版本更新任务")

        // JobScheduler 拉活
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            KeepAliveJobService.startJob(this)
//            LogUtil.i(TAG,"开启软件保活设置")
//        }
    }


    override fun netWorkStatus(statue: String?) {
        runTask = if (statue.equals("0")) {
            binder.changeNetwork(true)
            binder.connect()
            true
        }  //断线重连
        else {
            //修改更新标志
            binder.changeNetwork(false)
            binder.disconnect()
            false
        }
    }


    inner class MqttConnectState : IMqttConnectState {
        override fun onConnectSuccess() {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH, true))
            //订阅主题
            binder.subscribe(TOPIC_TITLE, 1)
        }

        override fun onConnectFail(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH, false))
        }

        override fun onTopicArrive(topic: String?, message: MqttMessage?) {
            LogUtil.d(TAG, "topic $topic  ,message${message}")
            message?.payload?.also {
                val payload = String(it, Charset.forName("utf-8"))
                updateMeal(payload)
            }
        }

        override fun onConnectLost(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH, false))
        }

    }

    /**
     * 定时更新菜品
     */
    @OptIn(ExperimentalTime::class)
    fun upDataDishes() {
        mScope.launch() {
            while (isActive) {
                delay(Duration.seconds(30))
//                delay(Duration.minutes(1))
//                delay(Duration.seconds(30))
                LogUtil.i(TAG, "定时任务:开始同步菜品")
                val rs = mRespository.getDayDishes()
                if (rs.code == "200") {
                    val mealList = mutableListOf<MealTable>()
                    val dishList = mutableListOf<DishesTable>()
                    val picList = mutableListOf<String>() //菜品图片
                    val catList = mutableListOf<CategoryTable>() //菜品类别

                    //提取下架菜品
                    val dishMap = DishesDBHelper.getInstance().queryDishes().stream()
                        .filter { it.status == 0 }.collect(Collectors.toMap({ "${it.mealId}:${it.dishesId}:${it.dishesName}" }) { t -> t.status })
                    LogUtil.i(TAG, "未更新时已下架菜品总数: ${dishMap.size}")
                    dishMap.forEach { t, u ->
                        LogUtil.i(TAG, "下架的菜品 $t $u")
                    }

                    for (da in rs.data!!) {
                        val meal = MealTable()
                        meal.mealId = da.mealId
                        meal.mealName = da.mealName
                        if (da.mealName.isEmpty()) continue

                        da.startTime?.also {
                            val split = it.split(":")
                            val date = Date()
                            date.hours = split[0].toInt()
                            date.minutes = split[1].toInt()
                            date.seconds = split[2].toInt()
                            meal.startTime = date
                        }

                        da.endTime?.also {
                            val split = it.split(":")
                            val date = Date()
                            date.hours = split[0].toInt()
                            date.minutes = split[1].toInt()
                            date.seconds = split[2].toInt()
                            meal.endTime = date
                        }

                        mealList.add(meal)
                        for (bean in da.selectedDishesCategoryData) {
                            if (!bean.categoryName.isNullOrEmpty()) {
                                val cat = CategoryTable()
                                cat.categoryId = bean.categoryId
                                cat.categoryName = bean.categoryName
                                cat.sort = bean.sort
                                cat.mealId = da.mealId
                                catList.add(cat)
                            }
                            bean.selectedDishesList.forEach {
                                val dish = DishesTable()
                                dish.dishesId = it.dishesId
                                dish.dishesName = it.dishesName
                                dish.mealId = da.mealId
                                dish.price = it.price.toDouble()
                                dish.unit = it.unit
                                dish.imgUrl = it.imgUrl ?: ""
                                dish.status = dishMap.get("${dish.mealId}:${dish.dishesId}:${dish.dishesName}") ?: 1
                                dish.categoryName = bean.categoryName
                                dishList.add(dish)
                                picList.add(dish.imgUrl)
                            }
                        }
                    }
                    DishesDBHelper.getInstance().clearAllDishes()
                    DishesDBHelper.getInstance().clearAllMeal()
                    DishesDBHelper.getInstance().clearAllCategory()
                    DishesDBHelper.getInstance().insertDishes(dishList)
                    DishesDBHelper.getInstance().insertMeals(mealList)
                    DishesDBHelper.getInstance().insertCategory(catList)

                    //下载菜品图片
                    downLoadPic(picList)

                    //设置菜品数据已更新
                    val kv = MMKV.defaultMMKV()
                    val now = DateFormat.format("yyyyMMdd HH:mm:ss", System.currentTimeMillis()).toString()
                    kv.encode(Constant.UPDATE_TIME, now.substring(0, 8))
                    kv.encode(Constant.FINAL_TIME, now)
                    //发送菜品更新通知
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_FIFTH, null))
                } else {
                    LogUtil.e(TAG, "菜品下载出错 ${rs.msg}")
                }
                delay(Duration.minutes(80 + Random.nextInt(30)))
            }
        }
    }

    /**
     * mqtt更新今天菜品和下载菜品图片
     */
    private fun updateMeal(payload: String) {
        mScope.launch {
            val gson = Gson()
            val type = object : TypeToken<MutableList<DayDishesBean>>() {}.type
            val dishes = gson.fromJson<MutableList<DayDishesBean>>(payload, type)

            //提取下架菜品
            val dishMap = DishesDBHelper.getInstance().queryDishes().stream()
                .filter { it.status == 0 }.collect(Collectors.toMap({ "${it.mealId}:${it.dishesId}:${it.dishesName}" }) { t -> t.status })
            LogUtil.i(TAG, "未更新时已下架菜品总数: ${dishMap.size}")
            dishMap.forEach { t, u ->
                LogUtil.i(TAG, "下架的菜品 $t $u")
            }

            val mealList = mutableListOf<MealTable>()  //餐别
            val dishList = mutableListOf<DishesTable>() //菜品
            val picList = mutableListOf<String>() //菜品图片
            val catList = mutableListOf<CategoryTable>() //菜品类别
            for (da in dishes) {
                val meal = MealTable()
                meal.mealId = da.mealId
                meal.mealName = da.mealName

                da.startTime?.also {
                    val split = it.split(":")
                    val date = Date()
                    date.hours = split[0].toInt()
                    date.minutes = split[1].toInt()
                    date.seconds = split[2].toInt()
                    meal.startTime = date
                }

                da.endTime?.also {
                    val split = it.split(":")
                    val date = Date()
                    date.hours = split[0].toInt()
                    date.minutes = split[1].toInt()
                    date.seconds = split[2].toInt()
                    meal.endTime = date
                }

                mealList.add(meal)
                for (bean in da.selectedDishesCategoryData) {
                    val cat = CategoryTable()
                    cat.categoryId = bean.categoryId
                    cat.categoryName = bean.categoryName
                    cat.sort = bean.sort
                    cat.mealId = da.mealId
                    catList.add(cat)
                    bean.selectedDishesList.forEach {
                        val dish = DishesTable()
                        dish.dishesId = it.dishesId
                        dish.dishesName = it.dishesName
                        dish.mealId = da.mealId
                        dish.price = it.price.toDouble()
                        dish.unit = it.unit
                        dish.imgUrl = it.imgUrl ?: ""
                        dish.status = dishMap.get("${dish.mealId}:${dish.dishesId}:${dish.dishesName}") ?: 1
                        dish.categoryName = bean.categoryName
                        dishList.add(dish)
                        picList.add(dish.imgUrl)
                    }
                }
            }
            //存储到数据库中
            DishesDBHelper.getInstance().clearAllDishes()
            DishesDBHelper.getInstance().clearAllMeal()
            DishesDBHelper.getInstance().clearAllCategory()
            DishesDBHelper.getInstance().insertDishes(dishList)
            DishesDBHelper.getInstance().insertMeals(mealList)
            DishesDBHelper.getInstance().insertCategory(catList)

            //下载菜品图片
            downLoadPic(picList)

            //设置菜品数据已更新
            val kv = MMKV.defaultMMKV()
            val now = DateFormat.format("yyyyMMdd HH:mm:ss", System.currentTimeMillis()).toString()
            kv.encode(Constant.UPDATE_TIME, now.substring(0, 8))
            kv.encode(Constant.FINAL_TIME, now)

            //发送菜品更新通知
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_FIFTH, null))
        }

    }

    private fun downLoadPic(picList: MutableList<String>) {
        if (picList.size < 1) return
        // 清空目录
        val savePath = File(filesDir, Constant.PIC_DIR)
        if (!savePath.exists()) {
            savePath.mkdirs()
        }
        for (fi in savePath.listFiles()) {
            fi.delete()
        }
        for (path in picList) {
            if (path == null || path.isEmpty()) continue
            val pic = Glide.with(this)
                .load(path)
                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
            writeFile2Sd(pic, path.substring(path.lastIndexOf("/") + 1))
        }
    }


    private fun writeFile2Sd(source: File, name: String) {
        val file = File("${filesDir.absolutePath}${File.separator}${Constant.PIC_DIR}${File.separator}${name}")
        var fos: FileOutputStream? = null
        var fis: FileInputStream? = null
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            fis = FileInputStream(source)
            fos = FileOutputStream(file)

            val buf = ByteArray(1024)
            var len: Int
            while (fis.read(buf, 0, buf.size).also { len = it } != -1) {
                fos.write(buf, 0, len)
            }
            fos.flush()
            LogUtil.i(TAG, "download ：${file.name} !")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
                fis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }


    @OptIn(ExperimentalTime::class)
    private fun downPerson() {
        mScope.launch {
            val mv = MMKV.defaultMMKV()
            while (isActive) {

                val upTime = mv.decodeLong(Constant.PERSONINFO_TIME, 0)
                var timeout = (System.currentTimeMillis() - upTime) >= (TimeUnit.MINUTES.toMillis(30 + Random.nextLong(10)))
//                timeout = true
                var finish = false
                var currentPage = 1
                var failTime = 0
                if (timeout) {
                    LogUtil.d(TAG, "准备全量更新人员")
                    do {
                        val res = mRespository.downPerson(500, currentPage)
                        try {
                            if (res.code == "200") {
                                val result = DES3CBCUtil.decryptRSA(res.data)
                                val bean = Gson().fromJson(result, PersonList::class.java)
                                DishesDBHelper.getInstance().insertPersons(bean.list)
                                if (currentPage >= bean.totalPage) {
                                    finish = true
                                    mv.encode(Constant.PERSONINFO_TIME, System.currentTimeMillis())
                                } else {
                                    currentPage = bean.page + 1
                                }
                            } else {
                                LogUtil.e(TAG, "人员下载错误 ${res.msg}")
                                failTime++
                            }
                        } catch (e: Exception) {
                            failTime++
                            LogUtil.e(TAG, "error ${e.message}")
                        }
                    } while (runTask && !finish && (failTime < 10))
                    LogUtil.d(TAG, "全量更新人员完成")
                }
                delay(Duration.minutes(5))
            }
        }
    }


    override fun onDestroy() {

        LogUtil.d(TAG, "服务关闭")
        //关闭打印机
        USBPrinterHelper.instance.closePrinter()
        //取消mqtt监听
        binder.unRegisterListener()
        //断开mqtt
        binder.disconnect()

        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)
        MyApplication.setCreate(false)
        //关闭扫码头
//        ScanDevice.closeScan()
        mScope.cancel()
        super.onDestroy()
    }

}