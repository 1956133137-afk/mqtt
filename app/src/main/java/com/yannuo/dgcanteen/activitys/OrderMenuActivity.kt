package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.format.DateFormat
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.adapters.ScreenSlidePagerAdapter
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ActivityOrderMenueBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.dialogView.ShowDishDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.interfaces.FoodsCallback
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.FaceResult
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference


class OrderMenuActivity : BaseActivity<ActivityOrderMenueBinding>(), IProductsVM,
    NetworkStateManager.NetWorkListener, FoodsCallback {
    private var permissions = arrayOf(
        Manifest.permission.NFC,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
    )

    private lateinit var mProductsVM: ProductsVM
    private lateinit var handler: MyHandler
    private var value = 0
    private var mXService: MyService? = null
    private var navigation = true
    private var secondDisplays: Display? = null
    private var mFacePayService: ZHSTFacePayService? = null
    private var delayTime = 20L


    private var loadingDialog: LoadingDialog? = null //后台加载框
    private lateinit var passwordDialog: PasswordDialog
    private lateinit var mDishDisplay :DishesDisplay
    private lateinit var kv: MMKV

    private var mMealId = 0
    private var mealId = 0

    private var mScope: CoroutineScope? = null
    private lateinit var mAdapter:ScreenSlidePagerAdapter
    @Volatile
    private var mCardVerificationDisplay: CardVerificationDisplay? = null //刷卡/扫码核销界面
    private lateinit var displayManager: DisplayManager
    private var showDishDialog: ShowDishDialog? = null
    private val viewModel by lazy {
        VerificationVM()
    }
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, "onServiceConnected")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.d(TAG, " onServiceDisconnected")
        }
    }


    override fun bindLayout() {
        binding = ActivityOrderMenueBinding.inflate(layoutInflater)
    }


    override fun onInit() {
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)
        showDishDialog = ShowDishDialog(this)
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        if (havePermission()) {
            requestPermission()
        } else {

            initObj()
            initView()
            initEvent()
            mScope = CoroutineScope(Dispatchers.IO)
            checkTime()
        }
    }




    private fun initObj() {
        handler = MyHandler(this)
        mProductsVM = ViewModelProvider(this).get(ProductsVM::class.java)
        mProductsVM.listener = this
        EventBus.getDefault().register(this)
        kv = MMKV.defaultMMKV()
        initPresentation()

        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        bindService(lIntent, mServiceConnection, BIND_AUTO_CREATE)

        //注册网络状态监听
        NetworkStateManager.getInstance().registerObserver(this)
        mAdapter = ScreenSlidePagerAdapter(this)
    }

    private fun initView() {
        binding.vpMenuCt.adapter = mAdapter
        binding.vpMenuCt.offscreenPageLimit = 1
        mAdapter.addData(mutableListOf(0,1))
        binding.vpMenuCt.currentItem = 0
        binding.vpMenuCt.isUserInputEnabled = false
        //吐司信息显示
        mProductsVM.showToastEvent.observe(this) {
            ToastShowUtil.show(it)
        }
        //加载对话框显示
        mProductsVM.loadingEvent.observe(this) {
            if (it){
                if (loadingDialog==null) loadingDialog = LoadingDialog(this)
                loadingDialog?.show()
            }
            else loadingDialog?.dismiss()
//            LogUtil.d(TAG,"SHOW $it")
        }

        mProductsVM.tab.observe(this){
            binding.vpMenuCt.setCurrentItem(it,false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProductsVM.upDataDishes()
        }
        if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) {
            binding.onOffLine.setImageResource(R.drawable.ic_drama_no)
        }

        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }

        mDishDisplay.show()
    }


    private fun initPresentation() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        displayManager?.displays?.also {
            secondDisplays = it[1]
            mDishDisplay = DishesDisplay(this, secondDisplays!!)
            mProductsVM.setDisplay(mDishDisplay)
        }
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
        mDishDisplay.cancel()
        mDishDisplay = DishesDisplay(this, secondDisplays!!)
        mDishDisplay.show()
    }

    private fun initEvent() {

        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }

        //设置界面
        binding.btnSetting.setOnClickListener {

            passwordDialog.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@OrderMenuActivity, SettingActivity::class.java))
                    }
                })
            }
        }

        //菜品管理界面
        binding.btnDishMenu.setOnClickListener {
            val intent = Intent(this, DishManageActivity::class.java)
            startActivity(intent)
            finish()
        }

        //菜品同步
        binding.btnSynDishes.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mProductsVM.upDataDishes(true)
            }

        }

    }


    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventArrive(event: MessageEvent) {
        when (event.code) {

            Constant.EVENT_SECOND -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收开启人脸支付事件~")
                event.any?.also {
                    (it as? ProductsDetail)?.also { iit ->
                        if (mFacePayService == null) {
                            runOnUiThread {
                                ToastShowUtil.show("人脸服务连接异常")
                            }
                            //重新连接服务
                            CommonAndDpToPxUtil.speakWork("人脸服务连接异常")
                            LogUtil.e(TAG, "获取不到人脸句柄")
                            return
                        }
                        mProductsVM.startPayWithFace(mFacePayService, iit)
                    }
                }
            }


            Constant.EVENT_TENTH -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收mqtt状态变更事件~")
                runOnUiThread {
                    val connect = event.any as Boolean
                    if (connect) {
                        binding.server.setImageResource(R.drawable.ic_server)
                    } else {
                        binding.server.setImageResource(R.drawable.ic_server_no)
                    }
                }
            }

            Constant.EVENT_CODE -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收开启二维码、刷卡核销事件")
                CommonAndDpToPxUtil.speakWork("请出示核销码或者刷卡")
                runOnUiThread {
                    cardCodeVerification()
                }
            }

            Constant.EVENT_FACE -> handler.post {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收开启刷脸核销事件")
                CommonAndDpToPxUtil.speakWork("请刷脸进行核销")
                runOnUiThread {
                    faceVerification()
                }
            }

            Constant.EVENT_THIRTY_ONE -> {
                runOnUiThread {
                    if (event.any != null) {
                        LogUtil.i(TAG, "核销的菜品：${Gson().toJson(event.any)}")
                        showDishDialog?.showDishes(Gson().toJson(event.any))
                        showDishDialog?.show()
                    }
                    mDishDisplay = DishesDisplay(this, secondDisplays!!)
                    mDishDisplay.show()
                }
            }
        }
    }




    //扫码核销
    private fun cardCodeVerification() {
        mCardVerificationDisplay = secondDisplays?.let { CardVerificationDisplay(this, it) }
        mCardVerificationDisplay?.show()
        mDishDisplay.cancel()
    }

    //刷脸核销
    private fun faceVerification() {
        queryFaceInfo()
        mDishDisplay.cancel()
    }

    /**
     * 通过人脸查询人员信息
     */
    private fun queryFaceInfo() {
        LogUtil.d(TAG,"查询人脸信息~")
        var offline = 0  //在线
        if (kv.decodeBool(Constant.SWITCH)) offline = 1  //离线
        val mPayCfg = viewModel.getPayCfg()
        val campusId = if (mPayCfg == null) "" else mPayCfg.campusId
        val businessId = if (mPayCfg == null) "" else mPayCfg.businessId
        val sn = Utils.getSN()
        mFacePayService?.startFacePay(
            null,
            offline.toString(),
            object : PayResultListener.Stub() {
                override fun onResult(result: String?) {
                    LogUtil.i(TAG, result)
                    val res = Gson().fromJson(result, FaceResult::class.java)
                    if (res.RESULT == "Y") {
                        viewModel.verification(campusId, businessId, res.CUST_ID, null, sn, null)
                    }
                }
            }
        )
    }



    private fun checkTime() {
        mScope?.launch {
            while (isActive) {
                mMealId = TimeUtil.CurrentTimeSection()
                if (mMealId != mealId) {
                    mealId = mMealId
                    val str = StringBuilder()
                    when (mealId) {
                        0 -> str.append(resources.getString(R.string.unOpen_meal))
                        else -> {
                            val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
                            str.append(meal.mealName + " ")
                            str.append(DateFormat.format("HH:mm", meal.startTime).toString() + "~")
                            str.append(DateFormat.format("HH:mm", meal.endTime).toString())
                        }
                    }
                    withContext(Dispatchers.Main) {
                        binding.mealTime.text = str
                    }
                    mProductsVM.menuChange.postValue(mealId)
                }

                delay(5000)
            }
        }
    }

    /**
     * 刷脸结果回调
     * @param data PayResultForUI
     */
    override fun onFacePayResult(data: PayResultForUI) {
        LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~")

    }









    override fun onDestroy() {
        release()
        super.onDestroy()

    }


    inner class MyHandler(context: OrderMenuActivity) : Handler() {
        private var reference: WeakReference<OrderMenuActivity> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            val ref = reference.get() ?: return
            when (msg.what) {

            }
        }
    }


    private fun release() {
        mScope?.cancel()
        passwordDialog.cancel()
        mDishDisplay.cancel()
        loadingDialog?.cancel()
//        unbindService(mServiceConnection)
        EventBus.getDefault().unregister(this)
        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)

        LogUtil.i(TAG,"release...")
//        timer?.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
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


    private fun havePermission(): Boolean {
        var result = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (str in permissions) {
                result = ((checkSelfPermission(str) == PackageManager.PERMISSION_GRANTED) && result)
            }
        }
        return result
    }

    /* 请求程序所需权限 */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 10086)
        }
    }

    override fun netWorkStatus(statue: String) {
        runOnUiThread {
            when (statue) {
                "0" -> {
                    if (kv.decodeBool(Constant.SWITCH, false))
                        kv.encode(Constant.SWITCH, false)
                    binding.network.setImageResource(R.drawable.ic_wifi)
                }
                else -> {
                    if (kv.decodeBool(Constant.SWITCH, false).not())
                        kv.encode(Constant.SWITCH, true)
                    binding.network.setImageResource(R.drawable.ic_wifi_no)
                }
            }
        }
    }

    override fun onFoodsUpdate(foods: Any?) {

    }


}