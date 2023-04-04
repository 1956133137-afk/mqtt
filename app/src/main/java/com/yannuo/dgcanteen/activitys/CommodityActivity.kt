package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.*
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.ccb.smartcanteen.ZHSTFacePayService
import com.proembed.service.MyService
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.presenters.CommodityPresenter
import com.yannuo.dgcanteen.databinding.ActivityCommodityBinding
import com.yannuo.dgcanteen.interfaces.ICommodityPresenter
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoginPasswordDialog
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CommodityActivity :BaseActivity<ActivityCommodityBinding>(), View.OnClickListener,
    ICommodityPresenter {
    private var permissions = arrayOf(
        Manifest.permission.NFC,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
    )

    private lateinit var mProductsVM :ProductsVM

    private lateinit var handler : MyHandler
    private var value = 0
    private var mXService : MyService ?= null
    private var navigation = true
    private var clickCount = 0
    private var preClickTime = 0
    private var displays : Display?= null
    private var mFacePayService: ZHSTFacePayService? = null

    private var mProductsDisplay : DifferentDisplay ?= null  //点餐界面
    private var mChooseDisplay : ChooseDisplay ?= null  //付款选择界面
    private val messageWhat = 1
    private val messageWhatSecond = 2
    private  var loadingDialog : LoadingDialog? =null //后台加载框

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, " onServiceConnected")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.d(TAG, " onServiceDisconnected")
        }
    }


    override fun bindLayout() {
        binding = ActivityCommodityBinding.inflate(layoutInflater)
    }



    @SuppressLint("CheckResult")
    override fun onInit() {
        mXService = MyService(this)

        if (havePermission()) {
            requestPermission()
        }else {

            initPresentation()
            initObj()
            initView()
            initEvent()

            val timer = Timer()
            timer.schedule(timerTask,0,1000)

        }
    }


    override fun onResume() {
        super.onResume()
//        mXService?.hideNavBar = false

    }

    private fun initPresentation() {
        val mediaRouter = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter?
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        displayManager?.displays?.also {
            displays =it[1]
        }
        val route = mediaRouter!!.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        if (route != null) {
            val presentationDisplay = route.presentationDisplay
            if (presentationDisplay != null){
                mProductsDisplay = DifferentDisplay( this, displays)
                mProductsDisplay?.show()
            }
        }
    }

    private fun initObj(){
        handler = MyHandler(this)
        mProductsVM = ViewModelProvider(this).get(ProductsVM::class.java)
        mProductsVM.listener = this
        EventBus.getDefault().register(this)




        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        bindService(lIntent, mServiceConnection, BIND_AUTO_CREATE)
    }

    private fun initView() {

        //吐司信息显示
        mProductsVM.showToastEvent.observe(this){
            ToastShowUtil.show(it)
        }
        //加载对话框显示
        mProductsVM.loadingEvent.observe(this){
            loadingDialog?.cancel()
            loadingDialog = LoadingDialog(this)
            if (it)loadingDialog?.show()
            else {
                loadingDialog?.cancel()
                loadingDialog = null
            }
        }
        mProductsVM.upDataDishes()
    }


    private fun initEvent(){
        binding.tvTitle.setOnClickListener(this)
        binding.tvTitle.setOnLongClickListener {
            navigation  =!navigation
            mXService?.hideNavBar = navigation
            true
        }


        binding.btBackPay.setOnClickListener {
            mProductsDisplay?.show()
            mChooseDisplay?.cancel()

        }
    }


    //事件监听
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventArrive(event : MessageEvent){
        when(event.code) {
            Constant.EVENT_FIRST -> {
                event.any?.also {
                    val data = it as ProductsDetail
                    val copy = data.copy()
                    LogUtil.d(TAG, "event : ${event.code}")
                    handler.sendMessage(handler.obtainMessage(messageWhat, copy))
                }
            }
            Constant.EVENT_SECOND -> {
                event.any?.also {
                    (it as? ProductsDetail)?.also {iit ->
                        LogUtil.d(TAG, "event : ${event.code}")
                        handler.sendMessage(handler.obtainMessage(messageWhatSecond, iit))
                    }
                }
            }
        }
    }



    override fun onClick(v: View) {
        if (v.id == R.id.tv_title){
            //弹出密码对话框
            if (clickCount == 0){
                preClickTime = System.currentTimeMillis().toInt()
                clickCount++
            } else if (clickCount == 1){
                var curTime = System.currentTimeMillis().toInt()
                if (curTime - preClickTime < 500){
                    val passwordDialog = LoginPasswordDialog()
                    val display = this.windowManager.defaultDisplay
                    passwordDialog.PasswordDialog(this,display)
                }
                clickCount = 0;
                preClickTime = 0;
            }else{
                clickCount = 0;
                preClickTime = 0;
            }
        }
    }

    //    定时器
    private val timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            runOnUiThread(Runnable {
                if(NetWorkUtil.isNetWorkConnected(this@CommodityActivity)){
                    binding.onOffLine.setImageDrawable(getDrawable(R.drawable.ic_drama))
                    binding.server.setImageDrawable(getDrawable(R.drawable.ic_server))
                    binding.network.setImageDrawable(getDrawable(R.drawable.ic_wifi))
                }else {
                    binding.onOffLine.setImageDrawable(getDrawable(R.drawable.ic_drama_no))
                    binding.server.setImageDrawable(getDrawable(R.drawable.ic_server_no))
                    binding.network.setImageDrawable(getDrawable(R.drawable.ic_wifi_no))
                }

                if(TimeUtil.isCurrentInTimeScope(7,30,8,30)){
                    binding.mealTime.setText(R.string.breakfast_time)
                }else if (TimeUtil.isCurrentInTimeScope(11,30,13,0)){
                    binding.mealTime.setText(R.string.lunch_time)
                }else if (TimeUtil.isCurrentInTimeScope(16,30,19,30)){
                    binding.mealTime.setText(R.string.dinner_time)
                }else{
                    binding.mealTime.setText(R.string.unOpen_meal)
                }
            })
        }
    }

    override fun onFacePayResult() {

    }


    fun dealWith(list : ProductsDetail){
        mChooseDisplay = ChooseDisplay(this,list, displays)
        mChooseDisplay!!.show()
        mProductsDisplay!!.cancel()
    }

    inner class MyHandler(context : CommodityActivity) : Handler(){
        private var reference : WeakReference<CommodityActivity> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            val  ref = reference.get() ?: return
            when(msg.what){
                ref.messageWhat ->{
                    dealWith(msg.obj as ProductsDetail)
                }
                ref.messageWhatSecond ->{
                    if (ref.mFacePayService ==null){
                        ToastShowUtil.show("获取不到人脸句柄")
                        LogUtil.e(TAG,"获取不到人脸句柄")
                    }
                    ref.mProductsVM.startPayWithFace(ref.mFacePayService,msg.obj as ProductsDetail)
                    ref.mChooseDisplay?.cancel()
                }
            }
        }
    }


    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release(){
        EventBus.getDefault().unregister(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10086) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) granted = false
            }
            if (!granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    Toast.makeText(applicationContext, "需要开启权限", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun havePermission():Boolean{
        var result = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (str in permissions){
                result = ((checkSelfPermission(str) == PackageManager.PERMISSION_GRANTED) && result)
            }
        }
        return result
    }

    /* 请求程序所需权限 */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions,10086)
        }
    }

}