package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.View
import android.widget.Toast
import android_serialport_api.SerialPort
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.ActivityIntiBinding
import com.yannuo.dgcanteen.service.CameraService
import com.yannuo.dgcanteen.service.MyMqttService
import com.yannuo.dgcanteen.util.BytesUtils
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.*
import java.io.File

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:10
 **/
class InitActivity : BaseActivity<ActivityIntiBinding>() {


    private val kv = MMKV.defaultMMKV()
    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private lateinit var simpleDisplay: SimpleDisplay

    private var mode: String? = null
    private val PERMISSIONS_REQUEST = 1
    private val Permission = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,  //            Manifest.permission.SET_TIME,
        //            Manifest.permission.CHANGE_CONFIGURATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE
    )

    private var loading: LoadingDialog? = null
    private lateinit var scope: CoroutineScope


    override fun bindLayout() {
        binding = ActivityIntiBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initPresentation()
        loading = LoadingDialog(this)
        scope = CoroutineScope(Dispatchers.IO)
//        initView()

        initEvent()
    }

    /* 判断程序是否有所需权限 android22以上需要自申请权限 */
    private fun hasPermission(): Boolean {
        var result = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (pms in Permission) {
                result = result and (checkSelfPermission(pms) == PackageManager.PERMISSION_GRANTED)
            }
        }
        return result
    }


    /* 请求程序所需权限 */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(
            Permission,
            PERMISSIONS_REQUEST
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) granted = false
            }
            if (!granted) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//                    if (!shouldShowRequestPermissionRationale(PERMISSION_CAMERA)
//                            || !shouldShowRequestPermissionRationale(PERMISSION_READ_STORAGE)
//                            || !shouldShowRequestPermissionRationale(PERMISSION_WRITE_STORAGE)
//                            || !shouldShowRequestPermissionRationale(PERMISSION_INTERNET)
//                            || !shouldShowRequestPermissionRationale(PERMISSION_ACCESS_NETWORK_STATE)) {
                Toast.makeText(applicationContext, "需要开启摄像头网络文件存储权限", Toast.LENGTH_SHORT).show()
                //                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initMode()
    }

    private fun initPresentation() {
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        simpleDisplay = SimpleDisplay(this, secondDisplays)
        simpleDisplay.setActivity(this)
        simpleDisplay.show()
    }

    override fun onStop() {
        simpleDisplay.cancel()
        super.onStop()
    }

    private fun initEvent() {
        binding.order.setOnClickListener {
            binding.order.isEnabled =false
            kv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
            initMode()
        }
        binding.collection.setOnClickListener {
            binding.collection.isEnabled =false
            kv.encode(Constant.APP_MODE, Constant.PROCEEDS_MODE)
            initMode()
        }

        binding.collectionTwo.setOnClickListener {
            binding.collectionTwo.isEnabled =false
            kv.encode(Constant.APP_MODE, Constant.ORDERING_TWO_MODE)
            initMode()
        }
    }


    private fun initMode() {
        scope.launch {
            val serial = SerialPort(File("/dev/ttyS4"),  9600, 0)
            serial.outputStream?.also {
                it.write(BytesUtils.hex2Bytes("AABB0600000001060304"))
                it.flush()
            }
            delay(500)
            serial.outputStream?.close()
            serial.close()
            mode = kv.decodeString(Constant.APP_MODE)
            when (mode) {
                Constant.ORDERING_FOOD_MODE -> {
                    //TODO 初始化相关服务
                    withContext(Dispatchers.Main) {
                        loading?.show("启动相关服务")
                    }

                    val intent = Intent(this@InitActivity, MyMqttService::class.java)
                    startService(intent)
                    when (kv.decodeBool(Constant.BALANCE_SWITCH, false)){
                        true ->{
                            val intent = Intent(this@InitActivity, BalanceActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                        }
                        else ->{
                            val intent = Intent(this@InitActivity, CommodityActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                        }
                    }
                    delay(50)
                    finish()
                }
                Constant.PROCEEDS_MODE -> {
                    if (!hasPermission()) {
                        requestPermission()
                    } else {
                        withContext(Dispatchers.Main) {
                            loading?.show("启动相关服务")
                        }
                        val intent = Intent(this@InitActivity, CameraService::class.java)
                        startService(intent)
                        when (kv.decodeBool(Constant.BALANCE_SWITCH, false)){
                            true ->{
                                val intent = Intent(this@InitActivity, BalanceActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                startActivity(intent)
                            }
                            else ->{
                                val intent = Intent(this@InitActivity, CalculateActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                startActivity(intent)
                            }
                        }
                        delay(50)
                        finish()
                    }
                }
                Constant.ORDERING_TWO_MODE ->{
                    withContext(Dispatchers.Main) {
                        loading?.show("启动相关服务")
                    }
                    val intent = Intent(this@InitActivity, MyMqttService::class.java)
                    startService(intent)
                    when (kv.decodeBool(Constant.BALANCE_SWITCH, false)){
                        true ->{
                            val intent = Intent(this@InitActivity, BalanceActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                        }
                        else ->{
                            val intent = Intent(this@InitActivity, OrderMenuActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(intent)
                        }
                    }
                    delay(50)
                    finish()
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        binding.initFrame.visibility = View.VISIBLE
                        binding.awaitFrame.visibility = View.INVISIBLE
                    }

                    val result = withTimeoutOrNull(1000 * 60 * 5) {
                        repeat(10) {
                            delay(1000 * 60)
                            val md = kv.decodeString(Constant.APP_MODE)
                            if (md.isNullOrEmpty().not()) {
                                return@withTimeoutOrNull 1
                            }
                        }
                    }
                    if (result==null){
                        kv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
                        LogUtil.i(TAG,"超时未选择模式...")
                        initMode()
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        loading?.cancel()
        scope.cancel()
        super.onDestroy()
    }

}