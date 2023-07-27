package com.yannuo.dgcanteen.activitys

import android.app.job.JobScheduler
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ActivitySettingBinding
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PersonList
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.DES3CBCUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

class SettingActivity : AppCompatActivity() {
    private var binding: ActivitySettingBinding? = null
    private var kv: MMKV? = null
    private lateinit var mScope : CoroutineScope
    private lateinit var mHandle : CoroutineExceptionHandler
    private var loadingDialog: LoadingDialog ?= null
    private var TAG = javaClass.simpleName


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initScreen()
        initObject()
        initView()
        initEvent()
        initData()
    }

    private fun initView() {
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
    }

    private fun initObject() {
        kv = MMKV.defaultMMKV()
    }

    private fun initData() {
        try {
            binding!!.tvVersion.text = packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
        reload()

        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler $e ${e.message}")
        }
        mScope = CoroutineScope (Dispatchers.Default + mHandle)
    }

    private fun initEvent() {
        binding!!.btnVersion.setOnClickListener { view: View? ->  //版本更新
            val work = PeriodicWorkRequest.Builder(
                CheckVersionWorker::class.java,
                15,
                TimeUnit.MINUTES
            ).build()
            ToastShowUtil.show(this, "正在检查版本是否需要更新~")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                Constant.PERIODIC_WORK_KEY,
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
        }
        binding!!.btnSynPerson.setOnClickListener { view: View? ->
            loadingDialog?.cancel()
            loadingDialog = LoadingDialog(this)
            loadingDialog?.show()
            downPerson()
            ToastShowUtil.show("人员信息已同步~")
        }
        binding!!.btnExitAlive.setOnClickListener { view: View? ->  //退出保活
            // 创建 JobScheduler
            val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler?.cancelAll()
            ToastShowUtil.show("已取消保活")
        }
        binding!!.btnSave.setOnClickListener { view: View? ->  //保存信息
            save()
        }
        binding!!.ibtBack.setOnClickListener { view: View? ->  //返回
//            val intent = Intent(this,CommodityActivity::class.java)
//            startActivity(intent)
            finish()
        }
    }

    private fun reload() {
        binding!!.etAddress.setText(kv!!.decodeString(Constant.ADDRESS))
        binding!!.switchLine.isChecked = kv!!.decodeBool(Constant.SWITCH, false)
        binding!!.etMqttAddress.setText(kv!!.decodeString(Constant.MQTT_ADDRESS))
        binding!!.etMqttAccount.setText(kv!!.decodeString(Constant.MQTT_ACCOUNT))
        binding!!.etMqttPassword.setText(kv!!.decodeString(Constant.MQTT_PASSWORD))
        binding!!.tvFinalTime.text = kv!!.decodeString(Constant.FINAL_TIME)
        binding!!.etShowTime.setText("${kv!!.decodeInt(Constant.SHOW_TIME)}")
    }

    private fun save() {
        var change = true
        change = kv!!.decodeString(Constant.ADDRESS) == binding!!.etAddress.text.toString()
        if (!change) {
            kv!!.encode(Constant.ADDRESS, binding!!.etAddress.text.toString())
            RetrofitClient.overLoad() //更新服务器地址
        }
        kv!!.encode(Constant.SWITCH, binding!!.switchLine.isChecked)
        change = true
        if (kv!!.decodeString(Constant.MQTT_ADDRESS) != binding!!.etMqttAddress.text.toString()) {
            change = false
            kv!!.encode(Constant.MQTT_ADDRESS, binding!!.etMqttAddress.text.toString())
        }
        if (kv!!.decodeString(Constant.MQTT_ACCOUNT) != binding!!.etMqttAccount.text.toString()) {
            change = false
            kv!!.encode(Constant.MQTT_ACCOUNT, binding!!.etMqttAccount.text.toString())
        }
        if (kv!!.decodeString(Constant.MQTT_PASSWORD) != binding!!.etMqttPassword.text.toString()) {
            change = false
            kv!!.encode(Constant.MQTT_PASSWORD, binding!!.etMqttPassword.text.toString())
        }
        if (!change) {
            //mqtt配置变更
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_NINTH, null))
        }
        kv!!.encode(Constant.SHOW_TIME, binding!!.etShowTime.text.toString().toInt())
        ToastShowUtil.show(this, "保存成功:" + this.filesDir.absolutePath + "/mmkv")
    }

    private fun initScreen() {
        val params = window.attributes
        params.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.attributes = params
        var uiFlags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN) // hide status bar
        uiFlags = if (Build.VERSION.SDK_INT >= 19) {
            uiFlags or 0x00001000 //SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide navigation bars - compatibility: building API level is lower thatn 19, use magic number directly for higher API target level
        } else {
            uiFlags or View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
        window.decorView.systemUiVisibility = uiFlags
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
            val repository = PayRepositoryOfPay()
            var finish = false
            var currentPage = 1
            var failTime = 0
            LogUtil.d(TAG,"准备全量更新人员")
            do {
                val res = repository.downPerson(100, currentPage)
                try {
                    if (res.code == 200) {
                        val result = DES3CBCUtil.decryptRSA(res.data, prvKey)
                        val bean = Gson().fromJson(result, PersonList::class.java)
                        DishesDBHelper.getInstance().insertPersons(bean.list)
                        if (currentPage == bean.totalPage) {
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
            }while (!finish && (failTime <10) )
            LogUtil.d(TAG,"全量更新人员完成")
            withContext(Dispatchers.Main){
                loadingDialog?.cancel()
                if (!finish)
                    ToastShowUtil.show("同步失败，请重试！")
            }
        }
    }

}