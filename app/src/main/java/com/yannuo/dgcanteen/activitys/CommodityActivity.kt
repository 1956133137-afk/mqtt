package com.yannuo.dgcanteen.activitys

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.proembed.service.MyService
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.ScreenSlidePagerAdapter
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ActivityCommodityBinding
import com.yannuo.dgcanteen.databinding.TableLayoutBinding
import com.yannuo.dgcanteen.interfaces.ImportExportListener
import com.yannuo.dgcanteen.model.Result
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.LoadingDialog
import com.yannuo.dgcanteen.views.LoginPasswordDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


open class CommodityActivity :BaseActivity<ActivityCommodityBinding>(), View.OnClickListener {
    private var permissions = arrayOf(
        Manifest.permission.NFC,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,

        )

    private var value = 0
    private var longArray = LongArray(3)
    private var diffTime = 1000
    private var mXService : MyService ?= null
    private var navigation = true
    private var clickCount = 0
    private var preClickTime = 0
    private var mealIds = 0


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

                    //获取商品类别
//                    val types = DbHelper.getInstance().group
//                    val pagerAdapter = ScreenSlidePagerAdapter(this, types)
//                    val pagerAdapter = ProductFragment()
//                    binding.vpPeopleInfo.adapter = pagerAdapter

                    /*TabLayoutMediator(binding.tbSubTitle, binding.vpPeopleInfo) { tab, position ->
                        val bid = TableLayoutBinding.inflate(layoutInflater, null, false)
                        tab.customView = bid.root
                        bid.tvTableTitle.text = types[position]
                        if (position == 0) {
                            bid.tvTableTitle.setTextColor(resources.getColor(R.color.table_unselet))
                            bid.tvTableTitle.typeface = Typeface.DEFAULT_BOLD
                            bid.tvTableTitle.textSize = 45.0f
                        }else{
                            bid.tvTableTitle.textSize = 30.0f
                            bid.tvTableTitle.setTextColor(resources.getColor(R.color.table_unselet))
                        }
                    }.attach()

                    if (types.size < 0) ToastShowUtil.show("请先导入商品")
                    binding.tbSubTitle.addOnTabSelectedListener(object :
                        TabLayout.OnTabSelectedListener {
                        override fun onTabSelected(tab: TabLayout.Tab?) {
                            tab?.also {
                                val view = it.customView
                                val tv = view!!.findViewById<TextView>(R.id.tv_table_title)
                                tv.textSize = 45.0f
                                tv.setTextColor(resources.getColor(R.color.table_unselet))
                                tv.typeface = Typeface.DEFAULT_BOLD
                            }
                        }

                        override fun onTabUnselected(tab: TabLayout.Tab?) {
                            tab?.also {
                                val view = it.customView
                                val tv = view!!.findViewById<TextView>(R.id.tv_table_title)
                                tv.textSize = 30.0f
                                tv.setTextColor(resources.getColor(R.color.table_unselet))
                                tv.typeface = Typeface.DEFAULT
                            }
                        }

                        override fun onTabReselected(tab: TabLayout.Tab?) {}
                    })*/
                }

//            binding.vpPeopleInfo.isUserInputEnabled = false
            ViewModelProvider(this).get(ProductsVM::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
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
        when(v.id){
            binding.tvTitle.id ->{
                System.arraycopy(longArray,1,longArray,0,longArray.size -1)
                longArray[longArray.size -1] = System.currentTimeMillis()
                val diff =  longArray.get(longArray.size -1) - longArray.get(0)
                if (diff <= diffTime){
                    binding.tvTitle.isEnabled = false
                    LogUtil.d(TAG,"连续点击...")
                    val intent = Intent(this,LoggingDataActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

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
}