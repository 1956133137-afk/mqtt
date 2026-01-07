package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.View
import android_serialport_api.SerialPort
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.adapters.InitModeAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityIntiBinding
import com.yannuo.dgcanteen.facepass.AuthFace
import com.yannuo.dgcanteen.facepass.FaceInitListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.service.CameraService
import com.yannuo.dgcanteen.service.FaceService
import com.yannuo.dgcanteen.service.MyMqttService
import com.yannuo.dgcanteen.service.MyService
import com.yannuo.dgcanteen.util.BytesUtils
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.*
import mcv.facepass.FacePassHandler
import java.io.File


class InitActivity : BaseActivity<ActivityIntiBinding>(), FaceInitListener {
    private val groupName = "facePass"
    // Android 11 请求文件写入权限
    private val ANDROID_11_REQUEST_CODE = 0
    private val PERMISSIONS_REQUEST = 1

    // 请求权限的标识码
    private val REQUEST_PERMISSIONS_CODE = 1001

    private val downloadVM by lazy { DownloadVM() }

    @RequiresApi(Build.VERSION_CODES.R)
    private val PERMISSION_R: Array<String> = arrayOf(
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )

    private val PERMISSION_GROUP: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.WRITE_SETTINGS
    )

    private val kv = MMKV.defaultMMKV()
    private var displayManager: DisplayManager? = null
    private var secondDisplays: Display? = null
    private lateinit var settingDisplay: SettingDisplay

    private var mode: String? = null
    private var loading: LoadingDialog? = null
    private lateinit var scope: CoroutineScope
    private val initModeAdapter by lazy { InitModeAdapter() }

    override fun bindLayout() {
        binding = ActivityIntiBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        //申请权限
        if (!hasPermission()) requestPermission()
        requestAlertWindowPermission()

        loading = LoadingDialog(this)
        scope = CoroutineScope(Dispatchers.IO)

        if(kv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
            //初始化SDK
            FacePassHandler.initSDK(MyApplication.applicationContext, "")
            //人脸授权
            val authFace = AuthFace(this)
            authFace.authCheck(this)
        }else{
            Handler(MyApplication.applicationContext.mainLooper).post {
                binding.initText.text = "请选择模式初始化"
                binding.initModeView.visibility = View.VISIBLE
            }
            initMode()
        }

        // 初始化服务
        startService(Intent(this, MyService::class.java))

        initPresentation()

        FaceScanVM.instance.bindService()
        initObject()
    }

    //检测权限是否获得
    private fun hasPermission(): Boolean {
        for (permission in PERMISSION_GROUP) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    // 请求程序所需权限
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            PERMISSION_GROUP,
            REQUEST_PERMISSIONS_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST || requestCode == ANDROID_11_REQUEST_CODE) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) granted = false
            }
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PERMISSION_GROUP.forEach {
                    if (!shouldShowRequestPermissionRationale(it)) ToastShowUtil.show("需要开启摄像头网络文件存储权限")
                }
            }
        }
    }

    // 请求悬浮窗权限，用于接收开机广播后启动应用，具体参考：https://stars-one.site/2022/05/31/android-boot-app-start
    private fun requestAlertWindowPermission() {
        // 检查是否已经授予权限，大于6.0的系统适用，小于6.0系统默认打开，无需理会
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Settings.canDrawOverlays(this)) {
            // 没有权限，须要申请权限，由于是打开一个受权页面，因此拿不到返回状态的，因此建议是在onResume方法中重新执行一次校验
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
//        initMode()
    }

    private fun initPresentation() {
        secondDisplays = displayAvailable()
        if (secondDisplays != null) {
            settingDisplay = SettingDisplay(this, secondDisplays!!)
            settingDisplay.show()
        }
    }

    private fun displayAvailable(): Display? {
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        if (displayManager == null) return null
        val displays = displayManager!!.displays
        /*无副屏*/
        if (displays == null || displays.size < 2) return null
        val defaultDisplay = displayManager!!.getDisplay(Display.DEFAULT_DISPLAY)
        displays.forEach { display ->
            if (display.displayId == defaultDisplay.displayId) return@forEach
            /*有效 激活 适合扩展*/
            if (display.isValid && display.state == Display.STATE_ON && (display.flags and Display.FLAG_PRESENTATION !== 0)) return display
        }
        return null
    }

    override fun onStop() {
        settingDisplay.safeCancel()
        super.onStop()
    }

    private fun initObject() {
        binding.initModeView.layoutManager = GridLayoutManager(this, 4)
        binding.initModeView.adapter = initModeAdapter
        initModeAdapter.addData(
            mutableListOf(
                Constant.ORDERING_FOOD_MODE,
                Constant.NO_PIC_MODE,
                Constant.PROCEEDS_MODE,
                Constant.ORDERING_MEAL_MODE,
                Constant.ORDERING_VERIFY_MODE,
                Constant.ORDERING_TWO_MODE,
                Constant.PROCEEDS_TWO_MODE,
                Constant.MEAL_PREPARATION_MODE,
                Constant.LAUNDRY_SERVICE_MODE
            )
        )
        initModeAdapter.setItemListener(object : InitModeAdapter.OnItemClickListener {
            override fun onItemClick(modeName: String) {
                kv.encode(Constant.APP_MODE, modeName)
                initMode()
            }
        })
    }


    private fun initMode() {
        scope.launch {
            val serial = SerialPort(File("/dev/ttyS4"), 9600, 0)
            serial.getOutputStream()?.also {
                it.write(BytesUtils.hex2Bytes("AABB0600000001060304"))
                it.flush()
            }
            delay(500)
            serial.getOutputStream()?.close()
            serial.close()
            mode = kv.decodeString(Constant.APP_MODE)

            when (mode) {
                Constant.LAUNDRY_SERVICE_MODE,
                Constant.ORDERING_FOOD_MODE, Constant.ORDERING_TWO_MODE, Constant.PROCEEDS_MODE, Constant.PROCEEDS_TWO_MODE, Constant.ORDERING_MEAL_MODE, Constant.ORDERING_VERIFY_MODE, Constant.MEAL_PREPARATION_MODE, Constant.NO_PIC_MODE -> {
                    // 启动服务
                    withContext(Dispatchers.Main) { loading?.show("启动相关服务") }
                    when (mode) {
                        Constant.PROCEEDS_MODE, Constant.PROCEEDS_TWO_MODE -> startService(Intent(this@InitActivity, CameraService::class.java))
                        else -> startService(Intent(this@InitActivity, MyMqttService::class.java))
                    }
                    // 跳转界面
                    val intent = when {
                        kv.decodeBool(Constant.QUERY_VERIFY, false) -> Intent(this@InitActivity, CheckVerifyActivity::class.java)
                        kv.decodeBool(Constant.BALANCE_SWITCH, false) -> Intent(this@InitActivity, BalanceActivity::class.java)
                        else -> {
                            when (mode) {
                                Constant.ORDERING_FOOD_MODE -> Intent(this@InitActivity, CommodityActivity::class.java)
                                Constant.NO_PIC_MODE -> Intent(this@InitActivity, CommodityOnActivity::class.java)
                                Constant.ORDERING_TWO_MODE -> Intent(this@InitActivity, OrderMenuActivity::class.java)
                                Constant.PROCEEDS_MODE -> Intent(this@InitActivity, CalculateActivity::class.java)
                                Constant.PROCEEDS_TWO_MODE -> Intent(this@InitActivity, CalculateTwoActivity::class.java)
                                Constant.ORDERING_MEAL_MODE -> Intent(this@InitActivity, OrderMealActivity::class.java)
                                Constant.MEAL_PREPARATION_MODE -> Intent(this@InitActivity, MealPreparationActivity::class.java)
                                Constant.LAUNDRY_SERVICE_MODE -> Intent(this@InitActivity, LaundryActivity::class.java)
                                else -> Intent(this@InitActivity, OrderVerifyActivity::class.java)
                            }
                        }
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    // 杀掉界面
                    delay(50)
                    finish()
                }
                else -> {
                    val result = withTimeoutOrNull(1000 * 60 * 5) {
                        repeat(10) {
                            delay(1000 * 60)
                            val md = kv.decodeString(Constant.APP_MODE)
                            if (md.isNullOrEmpty().not()) {
                                return@withTimeoutOrNull 1
                            }
                        }
                    }
                    if (result == null) {
                        kv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
                        LogUtil.i(TAG, "超时未选择模式...")
                        initMode()
                    }
                }
            }
        }
    }

    private fun uploadLocalFace(){
        val intent = Intent(this@InitActivity, FaceService::class.java)
        startService(intent)
    }

    private fun initLocalFaceGroup(){
        lifecycleScope.launch(Dispatchers.IO){
            val facePass = FaceSDKHelper.getInstance().getCameraManager()!!.getFacePass()!!
            //删除底库
            facePass.deleteFaceLocalGroup()
            //新建底库
            val createFaceGroup = facePass.createFaceGroup(groupName)
            if(createFaceGroup){
                //初始化本地脸库
                val queryFaceAll = DishesDBHelper.getInstance().queryFaceAll()
                queryFaceAll.forEach {
                    val bindFaceToGroup = facePass.bindFaceToGroup(it.eigenvalue)
                    if(bindFaceToGroup) Log.d("TAG", "initLocalFaceGroup: ${it.custId} 初始化绑定成功")
                }
                val initFaceLocalGroup = facePass.initFaceLocalGroup(groupName)
                Log.d("TAG", "initLocalFaceGroup: 初始化底库人数：$initFaceLocalGroup")
            }
        }
    }

    override fun onDestroy() {
        loading?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun faceInitResult(code: Int, message: String) {

    }

    override fun faceLicenseResult(code: Int, message: String) {
        LogUtil.i(TAG, "人脸算法授权结果 code= $code message = $message")
        when(code){
            0 -> {
                FaceSDKHelper.getInstance().initAlgorithm()
                Handler(MyApplication.applicationContext.mainLooper).post {
                    binding.initText.text = "请选择模式初始化"
                    binding.initModeView.visibility = View.VISIBLE
                }
                initMode()
                // 同步本地人脸信息
                uploadLocalFace()
            }
            else -> {
                Handler(MyApplication.applicationContext.mainLooper).post {
                    binding.initText.text = "人脸算法授权失败"
                    binding.initModeView.visibility = View.GONE
                }
            }
        }
    }
}