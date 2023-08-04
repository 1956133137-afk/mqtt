package com.yannuo.dgcanteen.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.presenters.DataPresenter
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.*
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.facepass.AuthFace
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.facepass.FacePassUtils
import com.yannuo.dgcanteen.facepass.SDKInitResult
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.mqtt.InteractionBinder
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import mcv.facepass.FacePassHandler
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class CameraService : Service(), NetworkStateManager.NetWorkListener {
    private val TAG = javaClass.simpleName

    private lateinit var initcallback: SDKInitResult
    private lateinit var mScope: CoroutineScope
    private lateinit var binder: InteractionBinder
    private lateinit var mqttStateListener: MqttConnectState

    //订阅的主题
    private var TOPIC_TITLE = "ccb/pub/dishes/${CommonAndDpToPxUtil.getDeviceSerial()}" //订阅本机专属主题

    //订阅的主题
    private var TOPIC_LOCAL = "device/" + CommonAndDpToPxUtil.getDeviceSerial() //订阅本机专属主题
    private lateinit var mRespository: PayRepositoryOfPay
    private var runTask = true  //控制任务，无网络将睡眠
    private var faceAddTaskRunning = AtomicBoolean(false)  //人脸添加线程启动
    private var stopAddPeopleTask = false  //停止人员添加任务
    private lateinit var mDataPresenter: DataPresenter
    private val mService = LocalBinder()
    private var callbackListener: CallbackListener? = null

    override fun onCreate() {
        super.onCreate()
        val job = SupervisorJob()
        mScope = CoroutineScope(Dispatchers.IO + job)
        if (applicationInfo != null) {
            if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0)
                LogUtil.currentLev = 3
        }

        mScope.launch {

            mRespository = PayRepositoryOfPay()
            mDataPresenter = DataPresenter()
            binder = InteractionBinder(this@CameraService)

            mqttStateListener = MqttConnectState()
            binder.registerListener(mqttStateListener)
            binder.connect()    //开启mqtt连接

            checkNewAppAndKeepAlive()   //新版本检查任务
            NetworkStateManager.getInstance().registerObserver(this@CameraService) //网络状态监听
            getPayCfg()//获取配置
        }

        val authFace = AuthFace(this) //算法授权检查
        initcallback = object : SDKInitResult {
            override fun faceInitResult(code: Int, message: String) {
                when (code) {
                    0 -> {
                        LogUtil.d(TAG, "初始化 version: ${FacePassHandler.getVersion()} 算法完成")
                        FaceHandler.getFaceHInstance().isFaceInit = true
                    }
                    else -> {
                        //todo 算法初始化失败
                        LogUtil.e(TAG, "算法初始化失败")
                        ToastShowUtil.showt("算法初始化失败")
                        CommonAndDpToPxUtil.speakWork("算法初始化失败")
                        FaceHandler.getFaceHInstance().isFaceInit = false
                    }
                }
            }

            override fun faceLicenseResult(code: Int, message: String) {
                LogUtil.i(TAG, "算法授权: $message")
                when (code) {
                    0 -> {
                        FaceHandler.getInstance(applicationContext).initAlgorithm(initcallback)
                    }
                    else -> {
                        //todo 算法授权失败
                        ToastShowUtil.showt("算法未授权")
                        CommonAndDpToPxUtil.speakWork("算法未授权, $message")
                    }
                }
            }
        }
        authFace.authCheck(initcallback)
        deviceInit()
    }


    private fun deviceInit() {
        downPerson()//下载人员
        processingData()   //启动图片下载重新入库任务
        synConsumerDish()   //同步消费记录
        offLineFillMoney() //离线补扣
        cardFillMoney() //离线刷卡补扣
    }

    private suspend fun getPayCfg() {
        val result = mRespository.getPayCfg()
        if (result.code == 200) {
            val mv = MMKV.defaultMMKV()
            mv.encode(Constant.PAY_CONFIG, result.data)
            LogUtil.i(TAG, "已更新配置信息！")
        } else LogUtil.w(TAG, "更新配置信息失败！")
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
            .enqueueUniquePeriodicWork(
                Constant.PERIODIC_WORK_KEY,
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
        LogUtil.i(TAG, "启动软件版本更新任务")

        // JobScheduler 拉活
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeepAliveJobService.startJob(this)
            LogUtil.i(TAG, "开启软件保活设置")
        }
    }


    inner class MqttConnectState : IMqttConnectState {
        override fun onConnectSuccess() {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH, true))
            //订阅主题
//            binder.subscribe(TOPIC_TITLE,1)
        }

        override fun onConnectFail(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH, false))
        }

        override fun onTopicArrive(topic: String?, message: MqttMessage?) {
            LogUtil.d(TAG, "topic $topic  ,message${message}")
//            val payload = String(message.toString(), Charset.forName("utf-8"))

            when (topic) {
                TOPIC_LOCAL -> {
                    val bean = Gson().fromJson(message.toString(), PeopleBean::class.java)
                    if (bean.type == 1) { //人员数据
                        LogUtil.i(TAG, "下发 ${bean.data.size} 条人员记录")
                        DishesDBHelper.getInstance().getsession().callInTx {
                            DishesDBHelper.getInstance().insertPersons(bean.data) //插入并替换
                            //图片不为空，    插入一条待入的记录
                            if (bean.data[0].image.isNotEmpty()) {
                                val faceRecord = FaceRecord()
                                faceRecord.custId = bean.data[0].custId
                                faceRecord.image = bean.data[0].image
                                DishesDBHelper.getInstance().insertWaitAddFace(faceRecord)
                            }
                            return@callInTx true
                        }
                        //启动处理任务
                        processingData()
                    } else {
                        //命令
                        controlDevice(bean)
                    }
                }
            }
        }

        override fun onConnectLost(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH, false))
        }

    }

    private fun downLoadPic(picPath: String) {

        // 清空目录
//        val savePath = File(filesDir, Constant.PIC_PEP_DIR)
//        if (!savePath.exists()) {
//            savePath.mkdirs()
//        }
//        for (fi in savePath.listFiles()){
//            fi.delete()
//        }

        val pic = Glide.with(this)
            .load(picPath)
            .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .get()
        writeFile2Sd(pic, picPath.substring(picPath.lastIndexOf("/") + 1))

    }

    private fun writeFile2Sd(source: File, name: String) {
        val file =
            File("${filesDir.absolutePath}${File.separator}${Constant.PIC_DIR}${File.separator}${name}")
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


    /**
     * 接口不统一，所以分开内部人员和访客记录保存
     */
    @SuppressLint("CheckResult")
    fun processingData() {
        if (faceAddTaskRunning.get()) return
        faceAddTaskRunning.set(true)
        stopAddPeopleTask = false
        //先延迟一段时间，以便算法初始化
        mScope.launch {
            for (i in 1..60) {
                delay(1000)
                if (FaceHandler.getFaceHInstance().isFaceInit) break
            }
            if (FaceHandler.getFaceHInstance().isFaceInit.not()) {
                LogUtil.w(TAG, "算法超时未初始化！")
                faceAddTaskRunning.set(false)
                return@launch
            }

            var info: FaceRecord
            var downloadSuccess = 0   //成功下载条数
            var downloadFailed = 0    //失败下载条数
            var addLibSuccess = 0   //成功入库条数
            var addLibFailed = 0    //失败入库条数
            var count = 0
            try {
                while ((DishesDBHelper.getInstance()
                        .queryOnePerson() != null) && !stopAddPeopleTask
                ) {
                    info = DishesDBHelper.getInstance().queryOnePerson()
                    LogUtil.d(TAG, "下载图片 ${info.image}")
                    var pic: File? = null
                    try {
                        pic = Glide.with(this@CameraService)
                            .load(info.image)
                            .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get()
                    } catch (e: Exception) {
                        LogUtil.w(TAG, "下载失败${info.image}")
                    }
                    callbackListener?.onOtherListener(0, "")
                    if (pic != null) {
                        //下载成功
                        downloadSuccess++
                        val bitmap = BitmapFactory.decodeFile(pic.absolutePath)
                        val faceTokens = FaceTokens()
                        faceTokens.number = info.custId
                        //获取人脸操作句柄
                        val handler = FaceHandler.getInstance().ksHandler
                        //获取旧记录
                        DishesDBHelper.getInstance().searchFaceToken(faceTokens.number)?.also {
                            FacePassUtils.unbindFaceFromGroup(handler, it.token)//解绑原有人脸
                            DishesDBHelper.getInstance().deleteFaceToken(it.number) //清除特征记录
                        }
                        //提取特征并入库人脸
                        val success = FacePassUtils.registerFaceForOne(handler, bitmap, faceTokens)
                        LogUtil.d(TAG, "图片入库 $success")

//                    deviceCallback(info.messageId,info.name,info.number) //回调通知添加结果
                        DishesDBHelper.getInstance().getsession().callInTx {
                            DishesDBHelper.getInstance().deleteFaceRecord(info.custId) //删除待入库的记录
                            when (success) {
                                true -> {
                                    DishesDBHelper.getInstance().insertFaceToken(faceTokens)//特征保存
                                    addLibSuccess++
                                }
                                else -> {
                                    FacePassUtils.unbindFaceFromGroup(
                                        handler,
                                        faceTokens.token
                                    )//删除入库人脸
                                    addLibFailed++
                                }
                            }
                            return@callInTx true
                        }
                    } else {
                        //下载失败
                        downloadFailed++
                        //修改更新标志
                        info.tryd = true
                        DishesDBHelper.getInstance().updatePeopleInfo(info)
                    }
                    count++
                    callbackListener?.onOtherListener(0, "$count")
                }

                //重置下载失败的标志位，以便下载失败的图片下次可以再次下载
                do {
                    val visitorList = DishesDBHelper.getInstance().searchFaceRecords(0, true)
                    if (visitorList.isNotEmpty()) {
                        for (vis in visitorList) {
                            vis.tryd = false
                            LogUtil.i(TAG, "重置下载标识 : ${vis.custId}")
                        }
                        DishesDBHelper.getInstance().insertFaceRecords(visitorList) //更新重试标志
                    }

                } while (visitorList.isNotEmpty())
                faceAddTaskRunning.set(false)
                LogUtil.i(
                    TAG, "人员添加任务结束，成功下载$downloadSuccess 张图片，下载失败" + "$downloadFailed" +
                            " 张图片，$addLibSuccess 张添加到人脸库，$addLibFailed 张添加人脸库失败"
                )
                callbackListener?.onOtherListener(1, null)
                callbackListener = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    /**
     * 设备回调mqtt服务器
     * @param visitorInfo VisitorPeopleRecord
     */
    private fun deviceCallback(messageId: String, name: String, number: String) {
        val callback = MqttAddFaceCallback()
        callback.code = FacePassUtils.getCode()
        callback.msg = FacePassUtils.getMsg()
        callback.messageId = messageId
        callback.data =
            MqttAddFaceCallback.DataBeanZ(name, number, CommonAndDpToPxUtil.getDeviceSerial())

        binder.publish("deviceCallback", Gson().toJson(callback), 1)
    }

    /**
     * 命令操作
     * @param bean PeopleBean
     */
    private fun controlDevice(bean: PeopleBean) {
        if (bean.cmd.isNullOrEmpty()) {
            return
        }
//        when(bean.cmd){
//            "delete_all_user" ->{ //删除所有人员
//                stopAddPeopleTask = true
//
//                //清空设备所有人员
//                DbHelper.getInstance().deletePeopleAll()
//                DbHelper.getInstance().deleteTokenAll()
//                DbHelper.getInstance().deleteVisitorPeopleAll()
//                DbHelper.getInstance().deleteVisitorPeopleTokenAll()
//
//                //删除底库
//                FacePassUtils.deleteFaceLocalGroup(FacePassUtils.group_name)
//            }
//            "delete_user" ->{
//                //解绑原有人脸
//                val faceTokenBean = DbHelper.getInstance().searchFaceToken(bean.param) ?: return
//                FacePassUtils.unbindFaceFromGroup(faceTokenBean.token)
//                //删除单个人员
//                DbHelper.getInstance().deletePeople(bean.param)
//                DbHelper.getInstance().deleteFaceToken(bean.param)
//            }
//        }
    }

    fun synchFace(callback: CallbackListener?) {
        this.callbackListener = callback
        mScope.launch {
            DishesDBHelper.getInstance().deleteAllWaitAddFace()
            DishesDBHelper.getInstance().deleteAllFaceToken()
            //获取人脸操作句柄
            val handler = FaceHandler.getInstance()?.ksHandler
            //删除算法底库
            FacePassUtils.deleteFaceLocalGroup(handler)
            callbackListener?.onOtherListener(0, "重新初始脸库")
            val size = 0
            var index = 0  //页码
            do {
                val searchPersons = DishesDBHelper.getInstance().searchPersons(index)
                //图片不为空，    插入一条待入的记录
                for (bean in searchPersons) {
                    if (bean.image.isNotEmpty()) {
                        val faceRecord = FaceRecord()
                        faceRecord.custId = bean.custId
                        faceRecord.image = bean.image
                        DishesDBHelper.getInstance().insertWaitAddFace(faceRecord)
                    }
                }
                index++
            } while (size >= 100)
            callbackListener?.onOtherListener(0, "生成同步列表")
            delay(10 * 1000)
            processingData()
        }

    }


    @OptIn(ExperimentalTime::class)
    private fun downPerson() {
        mScope.launch {
            val prvKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIGRJ0RqOaaYrem6zmTo" +
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

                val upTime = mv.decodeLong(Constant.PERSONINFO_TIME, 0)
                var timeout = (System.currentTimeMillis() - upTime) >= (TimeUnit.HOURS.toMillis(3))
                timeout = true
                var finish = false
                var currentPage = 1
                var failTime = 0
                if (timeout) {
                    LogUtil.d(TAG, "准备全量更新人员")
                    do {
                        val res = mRespository.downPerson(100, currentPage)
                        try {
                            if (res.code == 200) {
                                val result = DES3CBCUtil.decryptRSA(res.data, prvKey)
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
                delay(Duration.minutes(30))
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }


    override fun onDestroy() {
        release()
        super.onDestroy()
    }


    private fun release() {
        mScope.cancel()
        FaceHandler.getInstance()?.release()
        //取消mqtt监听
        binder.unRegisterListener()
        //断开mqtt
        binder.disconnect()

        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)
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

    /**
     * 上传消费记录到自有平台
     */
    @OptIn(ExperimentalTime::class)
    private fun synConsumerDish() {
        mScope.launch {

            while (isActive) {
                LogUtil.i(TAG, "离线消费上传任务开始...")
                delay(Duration.minutes(30))
                val offline = MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
                if (offline) continue
                //在线模式下
                // 1、先复位上传标志
                do {
                    val dishList = DishesDBHelper.getInstance().extractConsumerOrder(true)
                    dishList.forEach {
                        it.up = false
                    }
                    DishesDBHelper.getInstance().updateConsumerOrders(dishList)
                } while (dishList.size == 100 && runTask)
                //2、上传记录

                val gson = Gson()
                do {
                    val order = DishesDBHelper.getInstance().queryConsumerOrder()
                    order?.also {
                        val js = gson.toJson(order)
                        LogUtil.d(TAG, js)
                        val bean = gson.fromJson(js, SynConsumeRecordBean::class.java)
                        val res = mRespository.synCsRecord(bean)
                        if (res.code == HttpURLConnection.HTTP_OK) {
                            //删除对应的消费记录
                            DishesDBHelper.getInstance().deleteConsumerOrder(it.ordeR_ID)
                            LogUtil.i(TAG, "离线订单${bean.ORDER_ID} 上传成功!")
                        } else {
                            LogUtil.e(TAG, "离线上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                            //修改上传标志
                            it.up = true
                            DishesDBHelper.getInstance().updateConsumerOrder(it)
                        }
                    }
                } while (order != null && runTask)
                LogUtil.i(TAG, "离线消费上传任务结束...")
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
                delay(Duration.hours(1))
                if (runTask && !MMKV.defaultMMKV()
                        .decodeBool(Constant.SWITCH)
                ) { //有网并且不为离线状态 //进行离线补扣
                    LogUtil.i(TAG, "离线订单补扣开始请求...")
                    do {
                        val order = DishesDBHelper.getInstance().queryOffLineOrder()
                        order?.also { it ->
                            val bean =
                                Gson().fromJson(Gson().toJson(order), OffLineTable::class.java)
                            val map = CanteenEncryptionUtil.getScanToPay(bean)
                            val res = mRespository.getCcbData(map).body()?.let { //扫码支付
                                Gson().fromJson(
                                    it.string().replace("\r\n", ""),
                                    ScanQrResultBean::class.java
                                )
                            }
                            if (res?.RESULT.toString() == "Y") {
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

                                //上传消费记录
                                res?.let { mDataPresenter.consumeRecord(bean, it, dishes) }

                                //删除对应离线记录的菜品
                                DishesDBHelper.getInstance()
                                    .deleteOffLineDish(it.offLineDishesList[0].orderid)
                                //删除对应的离线订单记录
                                DishesDBHelper.getInstance().deleteOffLineOrder(it.ordeR_ID)
                                LogUtil.i(TAG, "离线订单${bean.ordeR_ID} 上传成功!")
                            } else {
                                LogUtil.e(TAG, "离线补扣${bean.ordeR_ID} 订单失败==\n${res?.ERRMSG}")
                                //修改请求标志
                                it.postTag = true
                                DishesDBHelper.getInstance().updateOffLineOrder(it)
                            }
                        }
                    } while (order != null && runTask && !MMKV.defaultMMKV()
                            .decodeBool(Constant.SWITCH)
                    )
                    LogUtil.i(TAG, "离线订单补扣请求结束...")
                }
            }
        }
    }

    /**
     * 恢复网络并且不是离线模式离线刷卡补扣
     */
    @OptIn(ExperimentalTime::class)
    private fun cardFillMoney() {
        mScope.launch {
            while (isActive) {
                delay(Duration.hours(1))
                if (runTask && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) { //有网并且不为离线状态
                    LogUtil.i(TAG, "离线刷卡订单请求开始...")
                    do {
                        val order = DishesDBHelper.getInstance().queryCardOrder()
                        order?.also { it ->
                            val bean = Gson().fromJson(Gson().toJson(order), CardPay::class.java)
                            val map = CanteenEncryptionUtil.getCardToPay(bean)
                            val res = mRespository.getCcbData(map).body()?.let { //扫码支付
                                Gson().fromJson(
                                    it.string().replace("\r\n", ""),
                                    ScanQrResultBean::class.java
                                )
                            }
                            if (res?.RESULT.toString() == "Y") {
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

                                //上传消费记录
                                res?.let { mDataPresenter.cardConsumeRecord(bean, it, dishes) }

                                //删除对应离线记录的菜品
                                DishesDBHelper.getInstance()
                                    .deleteCardDish(it.cardDishesList[0].orderid)
                                //删除对应的离线订单记录
                                DishesDBHelper.getInstance().deleteCardOrder(it.order_id)
                                LogUtil.i(TAG, "离线订单${bean.order_id} 上传成功!")
                            } else {
                                LogUtil.e(TAG, "离线补扣${bean.order_id} 订单失败==\n${res?.ERRMSG}")
                                //修改请求标志
                                it.up = true
                                DishesDBHelper.getInstance().updateCardOrder(it)
                            }
                        }
                    } while (order != null && runTask && !MMKV.defaultMMKV()
                            .decodeBool(Constant.SWITCH)
                    )
                    LogUtil.i(TAG, "离线刷卡订单请求结束...")
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): CameraService = this@CameraService
    }

    override fun onBind(intent: Intent): IBinder {
        return mService
    }
}