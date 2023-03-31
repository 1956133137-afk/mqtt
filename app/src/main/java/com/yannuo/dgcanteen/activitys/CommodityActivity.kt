package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.proembed.service.MyService
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.databinding.ActivityCommodityBinding
import com.yannuo.dgcanteen.interfaces.ImportExportListener
import com.yannuo.dgcanteen.model.Result
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoadingDialog
import com.yannuo.dgcanteen.views.LoginPasswordDialog
import com.yannuo.dgcanteen.views.PayFinishDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


open class CommodityActivity :BaseActivity<ActivityCommodityBinding>(), View.OnClickListener,
    DifferentDisplay.CallbackListener {
    private var permissions = arrayOf(
        Manifest.permission.NFC,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,

        )
    private lateinit var handler : MyHandler
    private var value = 0
    private var longArray = LongArray(3)
    private var diffTime = 1000
    private var mXService : MyService ?= null
    private var navigation = true
    private var clickCount = 0
    private var preClickTime = 0
    private var mealIds = 0
    private var displays : Display?= null

    private var presentation : DifferentDisplay ?= null


    override fun bindLayout() {
        binding = ActivityCommodityBinding.inflate(layoutInflater)
        scheduledTimer()
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

    @SuppressLint("CheckResult")
    override fun onInit() {
        mXService = MyService(this)

        if (havePermission()) {
            requestPermission()
        }else {
            val dialog = LoadingDialog(this)
            dialog.show()
            Observable.just(1).subscribeOn(Schedulers.io())
                .doOnNext {

                    ExcelUtils.importExc(this, object : ImportExportListener {
                        override fun processState(result: Result) {
                            LogUtil.d(TAG, "code:${result.code}  msg:${result.msg}")
                        }
                    })
                    initEvent()
                }

                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {

                    dialog.cancel()

                    scheduledTimer()

                }
            ViewModelProvider(this).get(ProductsVM::class.java)
            initPresentation()
            handler = MyHandler(this)
        }
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true


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
                 presentation = DifferentDisplay( this, displays)
                presentation?.setSureCallback(this)
                presentation?.show()

            }
        }
    }

    private fun initEvent(){
        binding.tvTitle.setOnClickListener(this)
        binding.tvTitle.setOnLongClickListener {
            navigation  =!navigation
            mXService?.hideNavBar = navigation
            true
        }
    }

//    override fun onBackPressed() {
//        if (binding.vpPeopleInfo.currentItem == 0)
//            super.onBackPressed()
//        else binding.vpPeopleInfo.currentItem = binding.vpPeopleInfo.currentItem - 1
//    }

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


    override fun onClick(v: View) {
        //弹出密码对话框
        clickCount = 0
        binding.tvTitle.setOnClickListener(View.OnClickListener {view ->
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
        })
    }

    //    定时器
    open fun scheduledTimer() {
        val service : ScheduledExecutorService = Executors.newScheduledThreadPool(4)
        service.scheduleAtFixedRate({
            try {
                scheduledTask()
            } catch (e:Throwable) {
                e.printStackTrace()
            }
        }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun scheduledTask(){
        runOnUiThread(Runnable {
            if(NetWorkUtil.isNetWorkConnected(this)){
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
                mealIds = 1
            }else if (TimeUtil.isCurrentInTimeScope(11,30,13,0)){
                binding.mealTime.setText(R.string.lunch_time)
                mealIds = 2
            }else if (TimeUtil.isCurrentInTimeScope(18,30,19,30)){
                binding.mealTime.setText(R.string.dinner_time)
                mealIds = 3
            }else{
                binding.mealTime.setText(R.string.unOpen_meal)
                mealIds = 0
            }
        })
    }

    override fun onSureListener(event: Int, obj: Any?) {
        LogUtil.d(TAG,"event : $event")
        handler.sendMessage(handler.obtainMessage(event,obj))
    }

    fun prest(){

        val prt = ChooseDisplay(this, displays)
//                presentation.set(this)
        prt.show()
        presentation?.cancel()

//        val mediaRouter = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter?
//        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
//        displayManager?.displays?.also {
//            displays =it[1]
//        }
//        val route = mediaRouter!!.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
//        if (route != null) {
//            val presentationDisplay = route.presentationDisplay
//            if (presentationDisplay != null){




//            }
//        }
    }

    inner class MyHandler(context : CommodityActivity) : Handler(){
        private var reference : WeakReference<CommodityActivity> = WeakReference(context)
        private var finishDialog : PayFinishDialog?= null

        override fun handleMessage(msg: Message) {
            val  ref = reference.get() ?: return
            when(msg.what){
                1 ->{ //弹出对话框，等待用户扫码
                    prest()
                }

//                Constant.EVENT_THREE ->{ //1.交易结果通知，展示结果
//
//                    payQrCodeDialog?.cancel() //关闭扫码界面
//                    val  data = msg.obj as PayResultForUI
//                    finishDialog?.cancel()
//                    finishDialog =  PayFinishDialog(ref.requireContext(),data)
//                    finishDialog?.show()
//                    //2.清空购物车
//
//
//
//
//                    CommonAndDpToPxUtil.speakWork("支付失败")
//                }
//                Constant.EVENT_FOUR ->{
//                    //展示扫码界面
//                    val data = msg.obj as Bitmap
//                    ref.startPayQrCodeDialog(data)
//                }
//
//                Constant.EVENT_FIVE ->{
//                    //是商品条码
//                    val product = ref.presenter.getBarcodeProduct(msg.obj as String)?.also {
//                        val path = it.pictureName.let {
//                            "$mProductsDir$it"
//                        }
//                        val data =  ProductInfo (
//                            it.pName,
//                            it.pMoney,
//                            path,
//                            it.type,
//                            it.barCode
//                        )
//                        //1、更新购物车
//                        updateUiItems( data,true)
//                        //2、更新商品选择列表fragment
//                        ref.model.receiveCountNotify.postValue(ref.adapter.getSpecifyBarcode(data.barCode))
//                    }
//                    if (product == null) ToastShowUtil.show("无效商品条码")
//                }

            }
        }
    }



}