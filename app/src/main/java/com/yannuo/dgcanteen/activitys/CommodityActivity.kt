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
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Display
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ccb.smartcanteen.ZHSTFacePayService
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.PayResultAdapter
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ActivityCommodityBinding
import com.yannuo.dgcanteen.databinding.PayFailureHostBinding
import com.yannuo.dgcanteen.databinding.PaySuccessHostBinding
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoadingDialog
import com.yannuo.dgcanteen.views.LoginPasswordDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.*


class CommodityActivity :BaseActivity<ActivityCommodityBinding>(),IProductsVM,
    NetworkStateManager.NetWorkListener {
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
    private var secondDisplays : Display?= null
    private var mFacePayService: ZHSTFacePayService? = null
    private var mProductsDisplay : DifferentDisplay ?= null  //点餐界面
    private var mChooseDisplay : ChooseDisplay ?= null  //付款选择界面
    private var mPayResultDisplay : PayResultDisplay ?= null  //支付结果界面
    private val messageWhat = 1
    private val messageWhatSecond = 2
    private val messageWhatThird = 3
    private var loadingDialog : LoadingDialog? =null //后台加载框
    private var timer: Timer? = null
    private var mMealId = 0
    private var mealId = 0
    private var successBinding : PaySuccessHostBinding ?= null
    private var failBinding : PayFailureHostBinding ?= null
    private var mScope : CoroutineScope ?=null


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
            mScope = CoroutineScope(Dispatchers.Default)
            checkTime()
        }
    }


    private fun initPresentation() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        displayManager?.displays?.also {
            secondDisplays = it[1]
        }
        secondDisplays?.also {
            mProductsDisplay = DifferentDisplay( this, secondDisplays)
            mProductsDisplay?.show()
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
        //注册网络状态监听
        NetworkStateManager.getInstance().registerObserver(this)

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
        if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) {
            binding.onOffLine.setImageResource(R.drawable.ic_drama_no)
        }

        if (NetworkStateManager.getInstance().isOnline(this).not()) {
            binding.network.setImageResource(R.drawable.ic_wifi_no)
        }
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
    }
    private fun initEvent(){

        binding.tvTitle.setOnLongClickListener {
            navigation  =!navigation
            mXService?.hideNavBar = navigation
            true
        }

        //退出支付，回到选餐界面
        binding.btBackPay.setOnClickListener {

            if (mChooseDisplay != null){
                CommonAndDpToPxUtil.speakWork("取消支付");
                mChooseDisplay?.cancel()
            }
            mChooseDisplay = null
            mPayResultDisplay?.cancel()
            mPayResultDisplay = null
            mProductsDisplay.also {
                if (it !=null && it.isShowing) {
                    return@also
                }
                mProductsDisplay = DifferentDisplay( this, secondDisplays)
                mProductsDisplay?.show()
            }
        }

        //设置界面
        binding.btnSetting.setOnClickListener{
            val passwordDialog = LoginPasswordDialog()
            val display = this.windowManager.defaultDisplay
            passwordDialog.PasswordDialog(this,display)
            passwordDialog.setListener(object : CloseEvent {
                override fun onEvent(code: Int, msg: String?) {
                    finish()
                }
            })
        }

        //菜品管理界面
        binding.btnDishMenu.setOnClickListener {
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
    fun eventArrive(event : MessageEvent){
        LogUtil.d(TAG, "event : ${event.code}")
        when(event.code) {
            Constant.EVENT_FIRST -> {
                event.any?.also {
                    val data = it as ProductsDetail
                    val copy = data.copy()
                    handler.removeMessages(messageWhat)
                    handler.sendMessage(handler.obtainMessage(messageWhat, copy))
                }
            }
            Constant.EVENT_SECOND -> {
                event.any?.also {
                    (it as? ProductsDetail)?.also {iit ->
                        if (mFacePayService ==null){
                            handler.post {
                                ToastShowUtil.show("获取不到人脸句柄")
                                //重新连接服务
                                val lIntent = Intent()
                                lIntent.action = "com.ccb.smartcanteen.FacePayService"
                                lIntent.setPackage("com.ccb.smartcanteen")
                                bindService(lIntent, mServiceConnection, BIND_AUTO_CREATE)
                            }
                            LogUtil.e(TAG,"获取不到人脸句柄")
                            return
                        }
                        mChooseDisplay?.cancel()
                        mChooseDisplay = null
                        mProductsVM.startPayWithFace(mFacePayService,iit)
                    }
                }
            }
            Constant.EVENT_THIRD -> {
                handler.removeMessages(messageWhatThird)
                handler.sendMessage(handler.obtainMessage(messageWhatThird))
            }
            Constant.EVENT_FOURTH ->{
                event.any?.also {
                    (it as? PayResultForUI)?.also {fit ->
                        mChooseDisplay?.cancel()
                        mChooseDisplay = null
                        updatePayResult(fit)
                    }
                }
            }

            Constant.EVENT_TENTH ->{
                runOnUiThread {
                    val connect = event.any as Boolean
                    if (connect){
                        binding.server.setImageResource(R.drawable.ic_server)
                    }else{
                        binding.server.setImageResource(R.drawable.ic_server_no)
                    }
                }
            }
        }
    }


    private fun updatePayState(data : PayResultForUI){
        val count = binding.flPayResult.childCount
        when(data.result){
            PayResultForUI.Result.SUCCESS ->{
                if (count == 1){
                    if ((binding.flPayResult.getChildAt(0) is LinearLayout).not()){
                        binding.flPayResult.removeAllViews()
                        initSuccessBinding()
                        binding.flPayResult.addView(successBinding?.root)
                    }
                }else{
                    initSuccessBinding()
                    binding.flPayResult.addView(successBinding?.root)
                }
                refreshSuccessState(data)
            }
            else ->{
                if (count == 1){
                    if ((binding.flPayResult.getChildAt(0) is ConstraintLayout).not()){
                        binding.flPayResult.removeAllViews()
                        initFailBinding()
                        binding.flPayResult.addView(failBinding?.root)
                    }
                }else{
                    initFailBinding()
                    binding.flPayResult.addView(failBinding?.root)
                }
                refreshFailState(data)
            }
        }
        handler.postDelayed(Runnable {
            binding.mvControl.text = "支付数据更新啦"
        },200)

    }


    private fun initFailBinding(){
        if (failBinding != null)return
        failBinding = PayFailureHostBinding.inflate(layoutInflater,null,false)
    }


    private fun initSuccessBinding(){
        if (successBinding != null)return
        successBinding = PaySuccessHostBinding.inflate(layoutInflater,null,false)
        val payResultAdapter =  PayResultAdapter()
        successBinding!!.rvDishList.layoutManager = LinearLayoutManager(this)
        successBinding!!.rvDishList.adapter = payResultAdapter

    }

    private fun refreshSuccessState(data : PayResultForUI){
        val persons = DishesDBHelper.getInstance().queryPersonToCustId(data.custId)
        var cls = "***"
        if (persons != null) {
            cls = persons.grade + persons.userClass
        }
        //更新数据
        (successBinding!!.rvDishList.adapter as PayResultAdapter).data = data.dishes
        successBinding!!.tvSum.text = " ${data.piece} 件"
        successBinding!!.payTotalMoney.text = "￥ ${data.payment} 元"
        successBinding!!.tvName.text = data.cust_name
        successBinding!!.tvClass.text = cls
        successBinding!!.tvPayTime.text = data.timestamp
        successBinding!!.tvTransNumber.text = data.orderid
    }

    private fun refreshFailState(data : PayResultForUI){
        //更新数据
        if (!TextUtils.isEmpty(data.errormsg)) {
            failBinding!!.payFailMsg.text = data.errormsg
        }
        if (!TextUtils.isEmpty(data.timestamp)) {
            failBinding!!.payTime.text = data.timestamp
        }

    }



    private fun checkTime(){
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
                                str.append(DateFormat.format("HH:mm", meal.startTime).toString() + "~")
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
    override fun onFacePayResult(data : PayResultForUI) {
        updatePayResult(data)

    }

    private fun updatePayResult(data : PayResultForUI){
        runOnUiThread {
            mPayResultDisplay = PayResultDisplay(this,data, secondDisplays)
            mPayResultDisplay?.show()
            updatePayState(data )
        }
    }

    inner class MyHandler(context : CommodityActivity) : Handler(){
        private var reference : WeakReference<CommodityActivity> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            val  ref = reference.get() ?: return
            when(msg.what){
                ref.messageWhat ->{
                    dealWith(msg.obj as ProductsDetail)
                }
                ref.messageWhatThird ->{
                    startDishDisplay()
                }
            }
        }
    }


    /**
     * 取餐处理
     * @param list ProductsDetail
     */
    private fun dealWith(list : ProductsDetail){
        mProductsDisplay?.cancel()
        mProductsDisplay = null
        mChooseDisplay = ChooseDisplay(this,list, secondDisplays)
        mChooseDisplay?.show()

    }

    /**
     * 打开选餐界面
     */
    private fun startDishDisplay(){
        mPayResultDisplay?.cancel()
        mPayResultDisplay = null
        mProductsDisplay = DifferentDisplay( this, secondDisplays)
        mProductsDisplay?.show()
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release(){
        mScope?.cancel()
        mProductsDisplay?.cancel()
        mPayResultDisplay = null
        mChooseDisplay?.cancel()
        mChooseDisplay = null
        mPayResultDisplay?.cancel()
        mPayResultDisplay = null

        EventBus.getDefault().unregister(this)
        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)
        binding.mvControl.stopAnima()
        timer?.cancel()
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

    override fun netWorkStatus(statue: String) {
        runOnUiThread {
            when(statue){
                "0" ->{
                    binding.network.setImageResource(R.drawable.ic_wifi)
                }
                else ->{
                    binding.network.setImageResource(R.drawable.ic_wifi_no)
                }
            }
        }
    }

}