package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ccb.smartcanteen.ZHSTFacePayService
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.adapters.FoodsAdapter
import com.yannuo.dgcanteen.adapters.HostPayResultAdapter
import com.yannuo.dgcanteen.databinding.ActivityCommodityBinding
import com.yannuo.dgcanteen.databinding.PayFailureHostBinding
import com.yannuo.dgcanteen.databinding.PaySuccessHostBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.interfaces.FoodsCallback
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.printer.PrinterOperator
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference

class CommodityActivity : BaseActivity<ActivityCommodityBinding>(), IProductsVM, NetworkStateManager.NetWorkListener, FoodsCallback {
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

    @Volatile
    private var mProductsDisplay: DifferentDisplay? = null  //点餐界面

    @Volatile
    private var mChooseDisplay: ChooseDisplay? = null  //付款选择界面

    @Volatile
    private var mPayResultDisplay: PayResultDisplay? = null  //支付结果界面
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    private val messageWhat = 1
    private val messageWhatSecond = 2
    private val messageWhatThird = 3
    private var loadingDialog: LoadingDialog? = null //后台加载框
    private lateinit var passwordDialog: PasswordDialog

    //    private var timer: Timer? = null
    private var mMealId = 0
    private var mealId = 0
    private var successBinding: PaySuccessHostBinding? = null
    private var failBinding: PayFailureHostBinding? = null
    private var mScope: CoroutineScope? = null
    private var adapterDishes: FoodsAdapter? = null
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
        binding = ActivityCommodityBinding.inflate(layoutInflater)

    }


    override fun onInit() {
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)
        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        bindService(lIntent, mServiceConnection, BIND_AUTO_CREATE)
        if (havePermission()) {
            requestPermission()
        } else {

            initPresentation()
            initObj()
            initView()
            initEvent()
            mScope = CoroutineScope(Dispatchers.IO)
            checkTime()


        }
    }


    private fun initPresentation() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        displayManager?.displays?.also {
            secondDisplays = it[1]
//            mProductsDisplay?.cancel()
            mProductsDisplay?.safeCancel()
            mProductsDisplay = DifferentDisplay(this, secondDisplays)
            mProductsDisplay?.setFoodsCallback(this)
            clearFoods()
            mProductsDisplay?.show()
//            mPayResultDisplay?.cancel()
            mPayResultDisplay?.safeCancel()
            mChooseDisplay?.safeCancel()
            mPayResultDisplay = null
            mChooseDisplay = null
        }
    }


    private fun initObj() {
        handler = MyHandler(this)
        mProductsVM = ViewModelProvider(this).get(ProductsVM::class.java)
        mProductsVM.listener = this
        EventBus.getDefault().register(this)

        //注册网络状态监听
        NetworkStateManager.getInstance().registerObserver(this)

    }

    private fun initView() {

        //吐司信息显示
        mProductsVM.showToastEvent.observe(this) {
            ToastShowUtil.show(it)
        }
        //加载对话框显示
        mProductsVM.loadingEvent.observe(this) {
            loadingDialog?.cancel()
            loadingDialog = LoadingDialog(this)
            if (it) loadingDialog?.show()
            else {
                loadingDialog?.cancel()
                loadingDialog = null
            }
        }
        mProductsVM.upDataDishes()
        if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) {
            binding.onOffLine.setImageResource(R.drawable.ic_drama_no)
        }

        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }
        val gridLayoutManager = GridLayoutManager(this, 2)
        adapterDishes = FoodsAdapter(this)
        binding.rvFoods.layoutManager = gridLayoutManager
        binding.rvFoods.adapter = adapterDishes
//        adapterDishes?.setImgSize(gridLayoutManager)
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
//        mProductsDisplay?.cancel()
        mProductsDisplay?.safeCancel()
        mProductsDisplay = DifferentDisplay(this, secondDisplays)
        mProductsDisplay?.show()
    }

    private fun initEvent() {

        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }

        //退出支付，回到选餐界面
        binding.btBackPay.setOnClickListener {
            mProductsDisplay.also {
                if (it != null && it.isShowing) {
                    return@also
                }
//                mProductsDisplay?.cancel()
                mProductsDisplay?.safeCancel()
                mProductsDisplay = DifferentDisplay(this, secondDisplays)
                mProductsDisplay?.setFoodsCallback(this)
                clearFoods()
                mProductsDisplay?.show()

            }

            if (mChooseDisplay != null) {
                CommonAndDpToPxUtil.speakWork("取消支付");
//                mChooseDisplay?.cancel()
                mChooseDisplay?.safeCancel()
            }
            mChooseDisplay = null
//            mPayResultDisplay?.cancel()
            mPayResultDisplay?.safeCancel()
            mPayResultDisplay = null
            binding.btBackPay.text = "支付解锁\n(选餐页面)"
        }

        //设置界面
        binding.btnSetting.setOnClickListener {
            if (mChooseDisplay != null || mPayResultDisplay != null) {
                ToastShowUtil.show("请完成支付后再操作!")
                CommonAndDpToPxUtil.speakWork("请完成支付后再操作!")
                return@setOnClickListener
            }
            passwordDialog.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@CommodityActivity, SettingActivity::class.java))
                    }
                })
            }
        }

        //菜品管理界面
        binding.btnDishMenu.setOnClickListener {
            if (mChooseDisplay != null || mPayResultDisplay != null) {
                ToastShowUtil.show("请完成支付后再操作!")
                CommonAndDpToPxUtil.speakWork("请完成支付后再操作!")
                return@setOnClickListener
            }
            val intent = Intent(this, DishManageActivity::class.java)
            startActivity(intent)
            finish()
        }

        //菜品同步
        binding.btnSynDishes.setOnClickListener {
            mProductsVM.upDataDishes(true)

        }

    }


    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventArrive(event: MessageEvent) {
        when (event.code) {
            Constant.EVENT_FIRST -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收取餐事件~")
                event.any?.also {
                    val data = it as ProductsDetail
                    val copy = data.copy()
                    runOnUiThread {
                        dealWith(copy)
                        binding.btBackPay.text = "支付解锁\n(支付页面)"
                    }
                }
            }
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
//                        mPayResultDisplay?.cancel()
                        mPayResultDisplay?.safeCancel()
                        mPayResultDisplay = null
//                        mChooseDisplay?.cancel()
                        mChooseDisplay?.safeCancel()
                        mChooseDisplay = null
                        mProductsVM.startPayWithFace(mFacePayService, iit)
                    }
                }
            }
            Constant.EVENT_THIRD -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收返回点餐页面事件~")
                runOnUiThread {
                    binding.btBackPay.text = "支付解锁\n(选餐页面)"
                    startDishDisplay()
                }

            }

            Constant.EVENT_FOURTH -> {
                LogUtil.d(TAG, "EventBus : ${event.code} 接收扫码/IC支付完事件,将跳转结果展示~")
                event.any?.also {
                    (it as? PayForUI)?.also { fit ->
                        handler.postDelayed({
//                            mChooseDisplay?.cancel()
                            mChooseDisplay?.safeCancel()
                            mChooseDisplay = null
//                            mProductsDisplay?.cancel()
                            mProductsDisplay?.safeCancel()
                            mProductsDisplay = null
                            binding.btBackPay.text = "支付解锁\n(结果页面)"
                        }, delayTime)
                        updatePayResult(fit)
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

//            Constant.EVENT_FACE -> {
//                LogUtil.d(TAG, "EventBus : ${event.code} 接收开启刷脸核销事件")
//                runOnUiThread {
//                    faceVerification()
//                }
//            }

            Constant.EVENT_CODE -> {
//                mProductsDisplay?.cancel()
                mProductsDisplay?.safeCancel()
                LogUtil.d(TAG, "EventBus : ${event.code} 接收开启二维码、刷卡核销事件")
                CommonAndDpToPxUtil.speakWork("请出示核销码或者刷卡")
                val i = Intent(this, CardVerificationActivity::class.java)
                startActivity(i)
            }
        }
    }

//
//    private fun updatePayState(data: PayResultForUI) {
//        val count = binding.flPayResult.childCount
//        when (data.result) {
//            PayResultForUI.Result.SUCCESS -> {
//                if (count == 1) {
//                    if ((binding.flPayResult.getChildAt(0) is LinearLayout).not()) {
//                        binding.flPayResult.removeAllViews()
//                        initSuccessBinding()
//                        binding.flPayResult.addView(successBinding?.root)
//                    }
//                } else {
//                    initSuccessBinding()
//                    binding.flPayResult.addView(successBinding?.root)
//                }
//                refreshSuccessState(data)
//            }
//            else -> {
//                if (count == 1) {
//                    if ((binding.flPayResult.getChildAt(0) is ConstraintLayout).not()) {
//                        binding.flPayResult.removeAllViews()
//                        initFailBinding()
//                        binding.flPayResult.addView(failBinding?.root)
//                    }
//                } else {
//                    initFailBinding()
//                    binding.flPayResult.addView(failBinding?.root)
//                }
//                refreshFailState(data)
//            }
//        }
//        handler.postDelayed({
//            binding.mvControl.text = "支付数据更新啦"
//        }, delayTime)
//
//    }

    private fun updatePayState(payForUI: PayForUI) {
        try {
            binding.flPayResult.removeAllViews()
            when (payForUI.result) {
                "Y" -> {
                    initSuccessBinding()
                    binding.flPayResult.addView(successBinding?.root)
                    refreshSuccessState(payForUI)
                }
                else -> {
                    initFailBinding()
                    binding.flPayResult.addView(failBinding?.root)
                    refreshFailState(payForUI)
                }
            }
        } catch (e: Exception) {
            ToastShowUtil.show("${e.message}")
            CommonAndDpToPxUtil.speakWork("页面更新异常!")
        }

        handler.postDelayed({
            binding.mvControl.text = "支付数据更新啦"
        }, delayTime)

    }


    private fun initFailBinding() {
        if (failBinding != null) return
        failBinding = PayFailureHostBinding.inflate(layoutInflater, null, false)
    }


    private fun initSuccessBinding() {
        if (successBinding != null) return
        successBinding = PaySuccessHostBinding.inflate(layoutInflater, null, false)
        val payResultAdapter = HostPayResultAdapter(this)
        successBinding!!.rvDishList.layoutManager = LinearLayoutManager(this)
        successBinding!!.rvDishList.adapter = payResultAdapter

    }

    @SuppressLint("SetTextI18n")
    private fun refreshSuccessState(payForUI: PayForUI) {

        CommonAndDpToPxUtil.speakWork(payForUI.payment + "元")
        successBinding!!.tvTransNumber.text = payForUI.traceId.ifEmpty { payForUI.orderId }
        val persons = DishesDBHelper.getInstance().queryPersonToCustId(payForUI.custId)
        if (persons != null && persons.grade != null) successBinding!!.tvClass.text = "${persons.grade}(${persons.userClass})"
        successBinding!!.tvName.text = payForUI.username
        successBinding!!.tvBalance.text = payForUI.accBal + "元"
        successBinding!!.tvPayTime.text = payForUI.payTime

        //更新数据
        (successBinding!!.rvDishList.adapter as HostPayResultAdapter).data = payForUI.paymentDishes
        successBinding!!.tvSum.text = " ${payForUI.paymentDishes.size} 件"
        successBinding!!.payTotalMoney.text = "￥ ${payForUI.payment} 元"

        PrinterOperator.printerFoodsList(payForUI)
        if (payForUI.result == "Y") USBPrinterHelper.instance.printTicket(payForUI)
    }


    private fun refreshFailState(payForUI: PayForUI) {
        //更新数据
        failBinding!!.payFailMsg.text = payForUI.errMsg
        failBinding!!.payTime.text = payForUI.payTime
    }


    private fun checkTime() {
        mScope?.launch {
            while (isActive) {
                if (mProductsDisplay != null) {
                    mMealId = TimeUtil.CurrentTimeSection()
                    if (mMealId != mealId) {
                        mealId = mMealId
                        val str = StringBuilder()
                        when (mealId) {
                            0 -> str.append(resources.getString(R.string.unOpen_meal))
                            else -> {
                                val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
                                str.append(meal.mealName + " ")
                                str.append(
                                    DateFormat.format("HH:mm", meal.startTime).toString() + "~"
                                )
                                str.append(DateFormat.format("HH:mm", meal.endTime).toString())
                            }
                        }
                        withContext(Dispatchers.Main) {
                            binding.mealTime.text = str
                            mProductsDisplay?.subScreenView(mealId, str)
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    /**
     * 刷脸结果回调
     * @param data PayResultForUI
     */
    override fun onFacePayResult(payForUI: PayForUI) {
        LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~")
        updatePayResult(payForUI)
    }

    private fun updatePayResult(payForUI: PayForUI) {
        runOnUiThread {
//            mPayResultDisplay?.cancel()
//            mPayResultDisplay = PayResultDisplay(this, payForUI, secondDisplays)
//            mPayResultDisplay?.cancel()
            mPayResultDisplay?.safeCancel()
            mPayResultDisplay = PayResultDisplay(this, data, secondDisplays)
//            mPayResultDisplay = PayResultDisplay(this, data, secondDisplays)
            mPayResultDisplay?.show()
            updatePayState(payForUI)
        }
    }

//    inner class MyHandler(context : CommodityActivity) : Handler(){
//        private var reference : WeakReference<CommodityActivity> = WeakReference(context)
//
//        override fun handleMessage(msg: Message) {
//            val  ref = reference.get() ?: return
//            when(msg.what){
//                ref.messageWhat ->{
////                    dealWith(msg.obj as ProductsDetail)
//                }
//                ref.messageWhatThird ->{
//                    startDishDisplay()
//                }
//            }
//        }
//    }


    /**
     * 取餐处理
     * @param list ProductsDetail
     */
//    private fun dealWith(list : ProductsDetail){
//
//        mScope?.launch {
//            withContext(Dispatchers.Main){
//                mChooseDisplay = ChooseDisplay(this@CommodityActivity,list, secondDisplays)
//                mChooseDisplay?.show()
//            }
//            delay(50)
//            withContext(Dispatchers.Main){
//                mProductsDisplay?.cancel()
//                mProductsDisplay = null
//            }
//        }
//    }

    /**
     * 取餐处理
     * @param list ProductsDetail
     */
    private fun dealWith(list: ProductsDetail) {
        mChooseDisplay?.safeCancel()
        mChooseDisplay = ChooseDisplay(this, list, secondDisplays)
        mChooseDisplay?.show()
//        mPayResultDisplay?.cancel()
        mPayResultDisplay?.safeCancel()
        mPayResultDisplay = null
        handler.postDelayed({
//            mProductsDisplay?.cancel()
            mProductsDisplay?.safeCancel()
            mProductsDisplay = null
        }, delayTime)

    }

    //刷脸核销
//    private fun faceVerification() {
//        mChooseDisplay?.cancel()
//        mChooseDisplay = null
//        queryFaceInfo()
//    }

    /**
     * 通过人脸查询人员信息
     */
//    private fun queryFaceInfo() {
//        LogUtil.d(TAG,"查询人脸信息~")
//        var offline = 0  //在线
//        if (kv.decodeBool(Constant.SWITCH)) offline = 1  //离线
//        val mPayCfg = viewModel.getPayCfg()
//        val campusId = if (mPayCfg == null) "" else mPayCfg.campusId
//        val businessId = if (mPayCfg == null) "" else mPayCfg.businessId
//        val sn = Utils.getSN()
//        mFacePayService?.startFacePay(
//            null,
//            offline.toString(),
//            object : PayResultListener.Stub() {
//                override fun onResult(result: String?) {
//                    LogUtil.i(TAG, result)
//                    val res = Gson().fromJson(result, FaceResult::class.java)
//                    if (res.RESULT == "Y") {
//                        viewModel.verification(campusId, businessId, res.CUST_ID, null, sn, null)
//                    }
//                }
//            }
//        )
//    }


    /**
     * 打开选餐界面
     */
//    private fun startDishDisplay(){
//        mScope?.launch {
//            withContext(Dispatchers.Main){
//                mProductsDisplay = DifferentDisplay( this@CommodityActivity, secondDisplays)
//                mProductsDisplay?.show()
//                delay(50)
//                mPayResultDisplay?.cancel()
//                mPayResultDisplay = null
//            }
//        }
//    }


    /**
     * 打开选餐界面
     */
    private fun startDishDisplay() {
//        mProductsDisplay?.cancel()
        mProductsDisplay?.safeCancel()
        mProductsDisplay = DifferentDisplay(this, secondDisplays)
        mProductsDisplay?.setFoodsCallback(this)
        clearFoods()
        mProductsDisplay?.show()

        mChooseDisplay?.safeCancel()

        mChooseDisplay = null
        handler.postDelayed({
//            mPayResultDisplay?.cancel()
            mPayResultDisplay?.safeCancel()
            mPayResultDisplay = null
        }, delayTime)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()

    }


    inner class MyHandler(context: CommodityActivity) : Handler() {
        private var reference: WeakReference<CommodityActivity> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            val ref = reference.get() ?: return
            when (msg.what) {
//                ref.messageWhat ->{
//                    dealWith(msg.obj as ProductsDetail)
//                }
//                ref.messageWhatThird ->{
//                    startDishDisplay()
//                }
            }
        }
    }


    private fun release() {
        mScope?.cancel()
//        mProductsDisplay?.cancel()
        mProductsDisplay?.safeCancel()
        mProductsDisplay = null
        mChooseDisplay?.safeCancel()
        mChooseDisplay = null
//        mPayResultDisplay?.cancel()
        mPayResultDisplay?.safeCancel()
        mPayResultDisplay = null
        passwordDialog.cancel()
        unbindService(mServiceConnection)
        EventBus.getDefault().unregister(this)
        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)
        binding.mvControl.stopAnima()
        LogUtil.i(TAG, "release...")
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
                    binding.network.setImageResource(R.drawable.ic_wifi)
                }
                else -> {
                    binding.network.setImageResource(R.drawable.ic_wifi_no)
                }
            }
        }
    }

    override fun onFoodsUpdate(foods: Any?) {
        val list = foods as? MutableList<DishesInfo>
        val foodsList = mutableListOf<DishesInfo>()
        if (list.isNullOrEmpty()) {
            clearFoods()
            return
        }
        val bg = binding.rvFoods.background
        if (bg == null) binding.rvFoods.setBackgroundResource(R.drawable.shape_btn_bg_white)
        list?.forEach {
            foodsList?.add(it.copy())
            adapterDishes?.data = foodsList
        }

    }

    private fun clearFoods() {
        adapterDishes?.clear()
        binding.rvFoods.background = null
    }

}