package com.yannuo.dgcanteen.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.FaceTokens
import com.yannuo.dgcanteen.dao.Persons
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.facepass.AuthFace
import com.yannuo.dgcanteen.facepass.FaceHandler
import com.yannuo.dgcanteen.facepass.FacePassUtils
import com.yannuo.dgcanteen.facepass.SDKInitResult
import com.yannuo.dgcanteen.interfaces.IMqttConnectState
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.MqttAddFaceCallback
import com.yannuo.dgcanteen.model.PeopleBean
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CameraService : Service(), NetworkStateManager.NetWorkListener {
    private val TAG = javaClass.simpleName

    private lateinit var initcallback : SDKInitResult
    private lateinit var mScope : CoroutineScope
    private lateinit var binder : InteractionBinder
    private lateinit var mqttStateListener : MqttConnectState
    //订阅的主题
    private var TOPIC_TITLE = "ccb/pub/dishes/${CommonAndDpToPxUtil.getDeviceSerial()}" //订阅本机专属主题
    //订阅的主题
    private var TOPIC_LOCAL = "device/"+ CommonAndDpToPxUtil.getDeviceSerial() //订阅本机专属主题
    private lateinit var mRespository : PayRepositoryOfPay
    private var runTask = true  //控制任务，无网络将睡眠
    private var faceAddTaskRunning = AtomicBoolean(false)  //人脸添加线程启动
    private var stopAddPeopleTask = false  //停止人员添加任务


    override fun onCreate() {
        super.onCreate()
        val job = SupervisorJob()
        mScope = CoroutineScope( Dispatchers.IO + job)
        if (applicationInfo != null) {
            if((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0)
                LogUtil.currentLev = 3
        }

        mScope.launch {

            mRespository = PayRepositoryOfPay()
            binder = InteractionBinder(this@CameraService)
            mqttStateListener = MqttConnectState()
            binder.registerListener(mqttStateListener)
            binder.connect()    //开启mqtt连接

            //新版本检查任务
            checkNewAppAndKeepAlive()
            //网络状态监听
            NetworkStateManager.getInstance().registerObserver(this@CameraService)
            //获取配置
            getPayCfg()
        }

        val authFace = AuthFace(this) //算法授权检查
        initcallback  = object : SDKInitResult {
            override fun faceInitResult(code: Int, message: String) {
                when(code){
                    0 -> {
                        LogUtil.d(TAG,"初始化 version: ${FacePassHandler.getVersion()} 算法完成")
                        FaceHandler.getFaceHInstance().isFaceInit  = true
                    }
                    else -> {
                        //todo 算法初始化失败
                        LogUtil.e(TAG,"算法初始化失败")
                        ToastShowUtil.showt("算法初始化失败")
                        CommonAndDpToPxUtil.speakWork("算法初始化失败")
                        FaceHandler.getFaceHInstance().isFaceInit  = false
                    }
                }
            }

            override fun faceLicenseResult(code: Int, message: String) {
                LogUtil.i(TAG,"算法授权: $message")
                when(code){
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


    private fun deviceInit(){

        //下载人员
//        downPerson()

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeepAliveJobService.startJob(this)
            LogUtil.i(TAG,"开启软件保活设置")
        }
    }




    inner class MqttConnectState : IMqttConnectState {
        override fun onConnectSuccess() {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH,true))
            //订阅主题
//            binder.subscribe(TOPIC_TITLE,1)
        }

        override fun onConnectFail(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH,false))
        }

        override fun onTopicArrive(topic: String?, message: MqttMessage?) {
            LogUtil.d(TAG,"topic $topic  ,message${message}")
//            val payload = String(message.toString(), Charset.forName("utf-8"))

            when(topic){
                TOPIC_LOCAL ->{
                    val bean = Gson().fromJson(message.toString(), PeopleBean::class.java)
                    if (bean.type == 1) { //人员数据
                        LogUtil.i(TAG, "下发 ${bean.data.size} 条人员记录")
                        DishesDBHelper.getInstance().insertPersons(bean.data) //插入并替换
                        //启动处理任务
//                        processingData()
                    }else{
                        //命令
                        controlDevice(bean)
                    }
                }
            }
        }

        override fun onConnectLost(reason: String?) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_TENTH,false))
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
        writeFile2Sd(pic, picPath.substring(picPath.lastIndexOf("/")+1))

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

            var info: Persons
            var downloadSuccess = 0   //成功下载条数
            var downloadFailed = 0    //失败下载条数
            var addLibSuccess = 0   //成功入库条数
            var addLibFailed = 0    //失败入库条数

            while (((DishesDBHelper.getInstance().queryOnePerson()
                    .also { info = it }) != null) && !stopAddPeopleTask
            ) {

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
                if (pic != null) {
                    //下载成功
                    downloadSuccess++
                    val bitmap = BitmapFactory.decodeFile(pic.absolutePath)
                    val faceTokens = FaceTokens()
                    faceTokens.number = info.custId

                    val handler = FaceHandler.getInstance().ksHandler
                    //获取旧记录
                    DishesDBHelper.getInstance().searchFaceToken(faceTokens.number)?.also {
                        //解绑原有人脸
                        FacePassUtils.unbindFaceFromGroup(handler, it.token)
                        DishesDBHelper.getInstance().deleteFaceToken(it.number) //清除记录
                    }
                    //提取特征并入库人脸
                    val success = FacePassUtils.registerFaceForOne(handler, bitmap, faceTokens)
                    LogUtil.d(TAG, "图片入库 $success")

//                    deviceCallback(info.messageId,info.name,info.number) //回调通知添加结果
                    DishesDBHelper.getInstance().getsession().callInTx {
                        //修改更新标志
                        info.update = true
                        DishesDBHelper.getInstance().updatePeopleInfo(info)
                        when (success) {
                            true -> {
                                DishesDBHelper.getInstance().insertFaceToken(faceTokens)//特征保存
                                addLibSuccess++
                            }
                            else -> {
                                FacePassUtils.unbindFaceFromGroup(handler, faceTokens.token)//删除入库人脸
                                addLibFailed++
                            }
                        }
                        return@callInTx true
                    }
                } else {
                    //下载失败
                    downloadFailed++
                }

                LogUtil.i(
                    TAG, "内部人员添加任务结束，成功下载$downloadSuccess 张图片，下载失败" +
                            "$downloadFailed 张图片，$addLibSuccess 张添加到人脸库，$addLibFailed 张添加人脸库失败"
                )

            }
        }
    }


    /**
     * 设备回调mqtt服务器
     * @param visitorInfo VisitorPeopleRecord
     */
    private fun deviceCallback(messageId :String ,name :String,number :String) {
        val callback = MqttAddFaceCallback()
        callback.code = FacePassUtils.getCode()
        callback.msg = FacePassUtils.getMsg()
        callback.messageId = messageId
        callback.data = MqttAddFaceCallback.DataBeanZ(name,number,CommonAndDpToPxUtil.getDeviceSerial())

        binder.publish("deviceCallback",Gson().toJson(callback),1)
    }

    /**
     * 命令操作
     * @param bean PeopleBean
     */
    private fun controlDevice( bean: PeopleBean){
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


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }





    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
        else{
            //修改更新标志
            binder.changeNetwork(false)
            binder.disconnect()
            false
        }
    }


}