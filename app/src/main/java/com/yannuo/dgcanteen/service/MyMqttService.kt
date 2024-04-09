package com.yannuo.dgcanteen.service


import android.app.Service
import android.content.Intent
import android.os.Build
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
import com.yannuo.dgcanteen.dao.CardPay
import com.yannuo.dgcanteen.dao.DishesTable
import com.yannuo.dgcanteen.dao.MealTable
import com.yannuo.dgcanteen.dao.OffLineTable
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.mqtt.InteractionBinder
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


//交互过程可通过binder设置主题到达监听。以更新页面
class MyMqttService: Service(), NetworkStateManager.NetWorkListener{
    private lateinit var binder : InteractionBinder
    private var TAG = javaClass.simpleName
    private lateinit var mqttStateListener : MqttConnectState

    //订阅的主题
    private var TOPIC_TITLE = "ccb/pub/dishes/${CommonAndDpToPxUtil.getDeviceSerial()}" //订阅本机专属主题

    private var runTask = true  //控制任务，无网络将睡眠

    private lateinit var mScope : CoroutineScope
    private lateinit var mHandle : CoroutineExceptionHandler
    private lateinit var mRespository : PayRepositoryOfPay
    private lateinit var mDataPresenter : DataPresenter



    override fun onCreate() {
        super.onCreate()
        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler $e ${e.message}")
        }
        mScope = CoroutineScope (Dispatchers.Default + mHandle)

        mScope.launch {

            mRespository = PayRepositoryOfPay()
            mDataPresenter = DataPresenter()
            binder = InteractionBinder(this@MyMqttService)
            mqttStateListener = MqttConnectState()
            binder.registerListener(mqttStateListener)
            binder.connect()    //开启mqtt连接


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
        cardFillMoney()    //离线刷卡补扣
        downPerson()     //下载人员
        upDataDishes()   //定时同步餐别和菜品图片、菜品价格
        LogUtil.d(TAG,"服务启动")
        checkNetPass()
    }


    private suspend fun getPayCfg() {
        val result = mRespository.getPayCfg()
        if (result.code == 200){
            val mv = MMKV.defaultMMKV()
            mv.encode(Constant.PAY_CONFIG,result.data)
            LogUtil.i(TAG,"已更新配置信息！")
        }else{
            LogUtil.w(TAG,"更新配置信息失败！")
        }
    }


    @OptIn(ExperimentalTime::class)
    private fun checkNetPass() {
        mScope.launch {
            while (isActive){
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
    private fun synConsumerDish(){
        mScope.launch {

            while(isActive){
                LogUtil.i(TAG,"离线消费上传任务开始...")
                delay(Duration.minutes(30))
//                delay(Duration.seconds(30))
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
                        val res = mRespository.synCsRecord(bean)
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
    @OptIn(ExperimentalTime::class)
    private fun offLineFillMoney(){
        mScope.launch {
            while (isActive){
//                delay(Duration.hours(1))
                delay(Duration.minutes(40))
//                delay(Duration.seconds(30))
                if (runTask && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)){ //有网并且不为离线状态 //进行离线补扣
                    LogUtil.i(TAG,"离线订单补扣开始请求...")
                    do {
                        val order = DishesDBHelper.getInstance().queryOffLineOrder()
                        order?.also { it ->
                            val bean = Gson().fromJson(Gson().toJson(order), OffLineTable::class.java)
                            val map = CanteenEncryptionUtil.getScanToPay(bean)
                            val res = mRespository.getCcbData(map).body()?.let { //扫码支付
                                Gson().fromJson(it.string().replace("\r\n",""), ScanQrResultBean::class.java)
                            }
                            if (res == null){
                                LogUtil.e(TAG, "扫码离线补扣${bean.ordeR_ID} 订单失败==网络错误")
                                //修改请求标志
                                it.postTag = true
                                DishesDBHelper.getInstance().updateOffLineOrder(it)
                                return@also
                            }
                            val dishes: MutableList<DishesInfo> = mutableListOf()
                            it.offLineDishesList.forEach {
                                dishes.add(
                                    DishesInfo(
                                        it.dishesId,
                                        it.dishesName,
                                        0,
                                        null,
                                        it.dishesPrice,
                                        "",
                                        "",
                                        0,
                                        it.dishesNumber
                                    )
                                )
                            }

//                            if (res?.RESULT.toString() == "Y"){
//                                //上传消费记录
//                                res?.let { mDataPresenter.consumeRecord( bean, it, dishes) }
//                                //删除对应离线记录的菜品
//                                DishesDBHelper.getInstance().deleteOffLineDish(it.offLineDishesList[0].orderid)
//                                //删除对应的离线订单记录
//                                DishesDBHelper.getInstance().deleteOffLineOrder(it.ordeR_ID)
//                                LogUtil.i(TAG,"离线订单${bean.ordeR_ID} 上传成功!")
//                            }else {
//                                LogUtil.e(TAG,"离线补扣${bean.ordeR_ID} 订单失败==\n${res?.ERRMSG}")
//                                //修改请求标志
//                                it.postTag = true
//                                DishesDBHelper.getInstance().updateOffLineOrder(it)
//                            }
                            if (res?.RESULT.toString() == "Y"){
                                LogUtil.i(TAG,"离线订单${bean.ordeR_ID} 上传成功!")
                            }else {
                                LogUtil.e(TAG,"离线补扣${bean.ordeR_ID} 订单失败==\n${res?.ERRMSG}")
                            }
                            //上传消费记录
                            res?.let { mDataPresenter.consumeRecord( bean, it, dishes) }
                            //删除对应离线记录的菜品
                            DishesDBHelper.getInstance().deleteOffLineDish(it.offLineDishesList[0].orderid)
                            //删除对应的离线订单记录
                            DishesDBHelper.getInstance().deleteOffLineOrder(it.ordeR_ID)
                        }
                    }while (order != null && runTask && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH))
                    LogUtil.i(TAG,"离线订单补扣请求结束...")
                }
            }
        }
    }

    /**
     * 恢复网络并且不是离线模式离线刷卡补扣
     */
    @OptIn(ExperimentalTime::class)
    private fun cardFillMoney(){
        mScope.launch {
            while (isActive){
//                delay(Duration.hours(1))
                delay(Duration.minutes(35))
//                delay(Duration.seconds(30))
//                if (NetworkStateManager.getInstance().isOnline(this@MyMqttService)) {
//                    MMKV.defaultMMKV().encode(Constant.SWITCH, false)
//                }
//
                if (runTask && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) { //有网并且不为离线状态
                    LogUtil.i(TAG,"离线刷卡订单请求开始...")
                    // 1、先复位上传标志
                    do {
                        val dishList = DishesDBHelper.getInstance().extractCardConsumerOrder(true)
                        dishList.forEach {
                            it.up = false
                        }
                        DishesDBHelper.getInstance().updateCardConsumerOrders(dishList)
                    } while (dishList.size == 100 && runTask)

                    do {
                        val order = DishesDBHelper.getInstance().queryCardOrder()
                        order?.also { it ->
                            val bean = Gson().fromJson(Gson().toJson(order), CardPay::class.java)
                            val map = CanteenEncryptionUtil.getCardToPay(bean)
                            val res = mRespository.getCcbData(map).body()?.let { //扫码支付
                                Gson().fromJson(it.string().replace("\r\n",""), ScanQrResultBean::class.java)
                            }

                            if (res == null){
                                //修改请求标志
                                it.up = true
                                DishesDBHelper.getInstance().updateCardOrder(it)
                                return@also
                            }

                            val dishes: MutableList<DishesInfo> = mutableListOf()
                            it.cardDishesList.forEach {
                                dishes.add(
                                    DishesInfo(
                                        it.dishesId,
                                        it.dishesName,
                                        0,
                                        null,
                                        it.dishesPrice,
                                        "",
                                        "",
                                        0,
                                        it.dishesNumber
                                    )
                                )
                            }
//                            if (res?.RESULT.toString() == "Y"){
//
//                                //上传消费记录
//                                res?.let { mDataPresenter.cardConsumeRecord( bean, it, dishes) }
//
//                                //删除对应离线记录的菜品
//                                DishesDBHelper.getInstance().deleteCardDish(it.cardDishesList[0].orderid)
//                                //删除对应的离线订单记录
//                                DishesDBHelper.getInstance().deleteCardOrder(it.order_id)
//                                LogUtil.i(TAG,"离线订单${bean.order_id} 上传成功!")
//                            }else {
//                                LogUtil.e(TAG,"离线补扣${bean.order_id} 订单失败==\n${res?.ERRMSG}")
//                                //修改请求标志
//                                it.up = true
//                                DishesDBHelper.getInstance().updateCardOrder(it)
//                            }
                            if (res?.RESULT.toString() == "Y"){
                                LogUtil.i(TAG,"离线订单${bean.order_id} 上传成功!")
                            }else {
                                LogUtil.e(TAG,"离线补扣${bean.order_id} 订单失败==\n${res?.ERRMSG}")
                            }

                            //上传消费记录
                            res?.let { mDataPresenter.cardConsumeRecord( bean, it, dishes) }

                            //删除对应离线记录的菜品
                            DishesDBHelper.getInstance().deleteCardDish(it.cardDishesList[0].orderid)
                            //删除对应的离线订单记录
                            DishesDBHelper.getInstance().deleteCardOrder(it.order_id)
                        }
                    }while (order != null && runTask && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH))
                    LogUtil.i(TAG,"离线刷卡订单请求结束...")
                }
            }
        }
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
        else{
            //修改更新标志
            binder.changeNetwork(false)
            binder.disconnect()
            false
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
            LogUtil.d(TAG,"topic $topic  ,message${message}")
            message?.payload?.also {
                val payload = String(it, Charset.forName("utf-8"))
                updateMeal(payload)
            }
        }

        override fun onConnectLost(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH,false))
        }

    }

    /**
     * 定时更新菜品
     */
    @OptIn(ExperimentalTime::class)
    fun upDataDishes(){
        mScope.launch() {
            while (isActive) {
                delay(Duration.minutes(80+ Random.nextInt(30)))
//                delay(Duration.minutes(1))
//                delay(Duration.seconds(30))
                LogUtil.i(TAG,"定时任务:开始同步菜品")
                val rs = mRespository.getDayDishes()
                if (rs.code == HttpURLConnection.HTTP_OK) {
                    val mealList = mutableListOf<MealTable>()
                    val dishList = mutableListOf<DishesTable>()
                    val picList = mutableListOf<String>() //菜品图片

                    //提取下架菜品
                    val dishMap =  DishesDBHelper.getInstance().queryDishes().stream()
                        .filter { it.status == 0 }.collect(Collectors.toMap({ "${it.mealId}:${it.dishesId}:${it.dishesName}" }) { t -> t.status })
                    LogUtil.i(TAG,"未更新时已下架菜品总数: ${dishMap.size}")
                    dishMap.forEach { t, u ->
                        LogUtil.i(TAG,"下架的菜品 $t $u")
                    }

                    for (da in rs.data!!) {
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
                        for (bean in da.selectedDishesList) {
                            val dish = DishesTable()
                            dish.dishesId = bean.dishesId
                            dish.dishesName = bean.dishesName
                            dish.mealId = da.mealId
                            dish.price = bean.price.toDouble()
                            dish.unit = bean.unit
                            dish.imgUrl = bean.imgUrl
                            dish.status = dishMap.get("${dish.mealId}:${dish.dishesId}:${dish.dishesName}") ?: 1
                            dishList.add(dish)
                            picList.add(bean.imgUrl)
                        }
                    }
                    DishesDBHelper.getInstance().clearAllDishes()
                    DishesDBHelper.getInstance().clearAllMeal()
                    DishesDBHelper.getInstance().insertDishes(dishList)
                    DishesDBHelper.getInstance().insertMeals(mealList)

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
            }
        }
    }

    /**
     * mqtt更新今天菜品和下载菜品图片
     */
    private fun updateMeal(payload :String){
        mScope.launch {
            val gson = Gson()
            val type = object : TypeToken<MutableList<DayDishesBean>>(){}.type
            val dishes = gson.fromJson<MutableList<DayDishesBean>>(payload,type)

            //提取下架菜品
            val dishMap =  DishesDBHelper.getInstance().queryDishes().stream()
            .filter { it.status == 0 }.collect(Collectors.toMap({ "${it.mealId}:${it.dishesId}:${it.dishesName}" }) { t -> t.status })
            LogUtil.i(TAG,"未更新时已下架菜品总数: ${dishMap.size}")
            dishMap.forEach { t, u ->
                LogUtil.i(TAG,"下架的菜品 $t $u")
            }

            val mealList = mutableListOf<MealTable>()  //餐别
            val dishList = mutableListOf<DishesTable>() //菜品
            val picList =  mutableListOf<String>() //菜品图片
            for (da in dishes){
                val meal = MealTable()
                meal.mealId = da.mealId
                meal.mealName = da.mealName

                da.startTime?.also {
                    val split =it.split(":")
                    val date = Date()
                    date.hours = split[0].toInt()
                    date.minutes = split[1].toInt()
                    date.seconds = split[2].toInt()
                    meal.startTime = date
                }

                da.endTime?.also {
                    val split =it.split(":")
                    val date = Date()
                    date.hours = split[0].toInt()
                    date.minutes = split[1].toInt()
                    date.seconds = split[2].toInt()
                    meal.endTime = date
                }

                mealList.add(meal)
                for (bean in da.selectedDishesList) {
                    val dish = DishesTable()
                    dish.dishesId = bean.dishesId
                    dish.dishesName = bean.dishesName
                    dish.mealId = da.mealId
                    dish.price = bean.price.toDouble()
                    dish.unit = bean.unit
                    dish.imgUrl = bean.imgUrl
                    dish.status = dishMap.get("${dish.mealId}:${dish.dishesId}:${dish.dishesName}") ?: 1
                    dishList.add(dish)
                    picList.add(bean.imgUrl)
                }
            }
            //存储到数据库中
            DishesDBHelper.getInstance().clearAllDishes()
            DishesDBHelper.getInstance().clearAllMeal()
            DishesDBHelper.getInstance().insertDishes(dishList)
            DishesDBHelper.getInstance().insertMeals(mealList)

            //下载菜品图片
            downLoadPic(picList)

            //设置菜品数据已更新
            val kv = MMKV.defaultMMKV()
            val now = DateFormat.format("yyyyMMdd HH:mm:ss",System.currentTimeMillis()).toString()
            kv.encode(Constant.UPDATE_TIME,now.substring(0,8))
            kv.encode(Constant.FINAL_TIME,now )

            //发送菜品更新通知
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_FIFTH, null))
        }

    }

    private fun downLoadPic(picList: MutableList<String>) {
        if (picList.size<1)return
        // 清空目录
        val savePath = File(filesDir, Constant.PIC_DIR)
        if (!savePath.exists()) {
            savePath.mkdirs()
        }
        for (fi in savePath.listFiles()){
            fi.delete()
        }
        for (path in picList) {
            val pic = Glide.with(this)
                .load(path)
                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
            writeFile2Sd(pic, path.substring(path.lastIndexOf("/")+1))
        }
    }


    private fun writeFile2Sd(source :File ,name :String) {
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
            LogUtil.i(TAG,"download ：${file.name} !")
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
    private fun downPerson(){
        mScope.launch {
            val prvKey ="MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIGRJ0RqOaaYrem6zmTo" +
                    "/SF2OROMcJwRws/b05kaG0N90ZKFdRucIuiWvCiU4y9LLD6yNaCIyDGH0VubFOGnwzF7BqGR" +
                    "4LTJgCHtfYodkE8XA99/P/cT/gi38uoX+UBnjxR2WeJPhHEr59tvVejb93KJQPMnhs7wJnxX" +
                    "YycTmJuLAgMBAAECgYAvoMcZfB7jIb7Ua2oRaCAc29ORXw/KHzFIrVs0LYeWILsYLFznIFco" +
                    "vrg+BrUYnn6OMX5LG9zTcETCctiTNtMmaBwG6J41GNWdwwJDdTmhjXs/jh6q1Wp3oT4jzlJW" +
                    "DozrwTWnzxFg/zoywntSFd46xzlt0YIXxpSQR8e0WxktYQJBAME/MTnp8csABoZ/OGhIS4qb" +
                    "xlVayHS+H8qCOU1mdb/aYDoiqf94LYpebqkCwcerjhz02ZX9xwqWTA2TUib8p1ECQQCrpDDi" +
                    "/M+mU2f63qs66usmLCIeJpaD56AeYIm5TdVC7PI6ZOx/FsBxJGkk1b6pmrOEAKQ7lFhMoq4U" +
                    "Z2I28hYbAkAqQF/J8s2L/ehvVbeGjW/+0UpO9Tdo1vzqcQiIVMOf++YYL+YNVkBWxYjaaSDn" +
                    "QColSJ+ePMtdFDlyqmhG3+zRAkAghHq+hibQ2/xXCthl0Ru7n6DXFXhuhPNQzflJofVFOJ6r" +
                    "cXNcoHLU/JDu6Y+1khlwaK60muYfnrJcKznwLu0BAkAKhJcHprKRRCJpT//A169jrbfuX1B6" +
                    "mFcOGXwPzO2s1JYzUlXCU4ylOVrLmdOpV+e7OSkrKNihVeIUm+TJt4MK"
            val mv = MMKV.defaultMMKV()
            while (isActive) {

                val upTime = mv.decodeLong(Constant.PERSONINFO_TIME,0)
                var timeout = (System.currentTimeMillis() - upTime) >= (TimeUnit.MINUTES.toMillis(30 + Random.nextLong(10)))
//                timeout = true
                var finish = false
                var currentPage = 1
                var failTime = 0
                if (timeout){
                    LogUtil.d(TAG,"准备全量更新人员")
                    do {
                        val res = mRespository.downPerson(200, currentPage)
                        try {
                            if (res.code == 200) {
                                val result = DES3CBCUtil.decryptRSA(res.data, prvKey)
                                val bean = Gson().fromJson(result, PersonList::class.java)
                                DishesDBHelper.getInstance().insertPersons(bean.list)
                                if (currentPage >= bean.totalPage) {
                                    finish = true
                                    mv.encode(Constant.PERSONINFO_TIME, System.currentTimeMillis())
                                }
                                else {
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
                    }while (runTask && !finish &&  (failTime <10) )
                    LogUtil.d(TAG,"全量更新人员完成")
                }
                delay(Duration.minutes(5))
            }
        }
    }



    override fun onDestroy() {

        LogUtil.d(TAG,"服务关闭")
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