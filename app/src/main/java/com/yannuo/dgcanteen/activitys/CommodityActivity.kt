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
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Display
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ccb.smartcanteen.ZHSTFacePayService
import com.proembed.service.MyService
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.presenters.ScanPayPresenter
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.PayResultAdapter
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ActivityCommodityBinding
import com.yannuo.dgcanteen.databinding.PayFailureHostBinding
import com.yannuo.dgcanteen.databinding.PaySuccessHostBinding
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoadingDialog
import com.yannuo.dgcanteen.views.LoginPasswordDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.*

class CommodityActivity :BaseActivity<ActivityCommodityBinding>(),IProductsVM {
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
    private var displays : Display?= null
    private var mFacePayService: ZHSTFacePayService? = null

    private var mProductsDisplay : DifferentDisplay ?= null  //点餐界面
    private var mChooseDisplay : ChooseDisplay ?= null  //付款选择界面
    private var mPayResultDisplay : PayResultDisplay ?= null  //支付结果界面
    private val messageWhat = 1
    private val messageWhatSecond = 2
    private val messageWhatThird = 3
    private val messageWhatFourth = 4
    private  var loadingDialog : LoadingDialog? =null //后台加载框
    private var timer: Timer? = null

    private var mMealId = 0
    private var mealId = 0
    private var mNetWork = false
    private var netWork = false
    private var mCurrentTime = 1681101000000


    private var successBinding : PaySuccessHostBinding ?= null
    private var failBinding : PayFailureHostBinding ?= null


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

            timer = Timer()
            timer!!.schedule(timerTask,0,1000)

        }
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
        binding.tvTitle.setOnLongClickListener {
            navigation  =!navigation
            mXService?.hideNavBar = navigation
            true
        }


        //退出支付，回到选餐界面
        binding.btBackPay.setOnClickListener {
            mChooseDisplay?.cancel()
            mChooseDisplay = null
            mPayResultDisplay?.cancel()
            mPayResultDisplay = null

            mProductsDisplay?.also {
                if (it.isShowing) {
                    return@also
                }
                mProductsDisplay = DifferentDisplay( this, displays)
                mProductsDisplay?.show()
            }
        }

        //设置界面
        binding.btnSetting.setOnClickListener{
            val passwordDialog = LoginPasswordDialog()
            val display = this.windowManager.defaultDisplay
            passwordDialog.PasswordDialog(this,display)
        }

        //菜品管理界面
        binding.btnDishMenu.setOnClickListener {
            val intent = Intent(this, DishManageActivity::class.java)
            startActivity(intent)
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
                    handler.sendMessage(handler.obtainMessage(messageWhat, copy))
                }
            }
            Constant.EVENT_SECOND -> {
                event.any?.also {
                    (it as? ProductsDetail)?.also {iit ->
                        handler.sendMessage(handler.obtainMessage(messageWhatSecond, iit))
                    }
                }
            }
            Constant.EVENT_THIRD -> {
                handler.sendMessage(handler.obtainMessage(messageWhatThird))
            }
            Constant.EVENT_FOURTH ->{
                event.any?.also {
                    (it as? PayResultForUI)?.also {fit ->
                        updatePayResult(fit)
                    }
                }
            }

            Constant.EVENT_TENTH ->{
                runOnUiThread {
                    val connect = event.any as Boolean
                }
            }
        }
    }


    private fun updatePayState(data : PayResultForUI){
        val count = binding.flPayResult.childCount
        binding.mvControl.text = "支付数据更新啦"
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
//        successBinding!!.rvDishList.addItemDecoration(
//            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
//        )

    }

    private fun refreshSuccessState(data : PayResultForUI){
        //更新数据
        (successBinding!!.rvDishList.adapter as PayResultAdapter).data = data.dishes
        successBinding!!.tvSum.text = " ${data.piece} 件"
        successBinding!!.payTotalMoney.text = "￥ ${data.payment} 元"
        successBinding!!.tvName.text = data.cust_name
        successBinding!!.tvClass.text = "20(15)班"
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


    //    定时器
    private val timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            mNetWork = NetWorkUtil.isNetWorkConnected(this@CommodityActivity)
            if (mNetWork != netWork){
                runOnUiThread(Runnable {
                    netWork = mNetWork
                    if (netWork){
                        binding.onOffLine.setImageDrawable(getDrawable(R.drawable.ic_drama))
                        binding.server.setImageDrawable(getDrawable(R.drawable.ic_server))
                        binding.network.setImageDrawable(getDrawable(R.drawable.ic_wifi))
                    }else{
                        binding.onOffLine.setImageDrawable(getDrawable(R.drawable.ic_drama_no))
                        binding.server.setImageDrawable(getDrawable(R.drawable.ic_server_no))
                        binding.network.setImageDrawable(getDrawable(R.drawable.ic_wifi_no))
                    }
                })
            }
            if (System.currentTimeMillis() - mCurrentTime >= 60000){
                runOnUiThread(Runnable {
                    mCurrentTime = System.currentTimeMillis()
                    binding.tvCurrentTime.text = DateFormat.format("yyyy-MM-dd HH:mm",mCurrentTime).toString()
                })
            }
            mMealId = TimeUtil.CurrentTimeSection()
            if (mMealId != mealId){
                runOnUiThread(Runnable {
                    mealId = mMealId
                    var str = StringBuilder()
                    when(mealId){
                        0 -> str.append(resources.getString(R.string.unOpen_meal))
                        else ->{
                            val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
                            str.append(meal.mealName + " ")
                            str.append(DateFormat.format("HH:mm",meal.startTime).toString() + "~")
                            str.append(DateFormat.format("HH:mm",meal.endTime).toString())
                        }
                    }
                    binding.mealTime.text = str
                })
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
            mPayResultDisplay = PayResultDisplay(this,data, displays)
            mPayResultDisplay?.show()
            updatePayState(data )
        }
    }

    /**
     * 被扫结果回调
     * @param data PayResultForUI
     */
    override fun onScanPayResult(data: PayResultForUI) {

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
                    ref.mChooseDisplay = null
                }

                ref.messageWhatThird ->{
                    startDishDisplay();
                }
                ref.messageWhatFourth ->{

                }
            }
        }
    }



    /**
     * 取餐处理
     * @param list ProductsDetail
     */
    private fun dealWith(list : ProductsDetail){
        mChooseDisplay = ChooseDisplay(this,list, displays)
        mChooseDisplay!!.show()
        mProductsDisplay!!.cancel()
        mProductsDisplay = null
    }

    /**
     * 打开选餐界面
     */
    private fun startDishDisplay(){
        mPayResultDisplay = null
        mProductsDisplay = DifferentDisplay( this, displays)
        mProductsDisplay?.show()
    }










    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release(){
        EventBus.getDefault().unregister(this)
        binding.mvControl.stopAnima()
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