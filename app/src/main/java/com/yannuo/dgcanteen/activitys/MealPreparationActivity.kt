package com.yannuo.dgcanteen.activitys

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.proembed.service.MyService
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.MealPreparationVM
import com.yannuo.dgcanteen.adapters.DishVerifyCountAdapter
import com.yannuo.dgcanteen.databinding.ActivityMealPreparationBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MealPreparationActivity : BaseActivity<ActivityMealPreparationBinding>(), NetworkStateManager.NetWorkListener {
    private lateinit var passwordDialog: PasswordDialog
    private var mXService: MyService? = null
    private var navigation = true
    private val dishVerifyCountAdapter by lazy { DishVerifyCountAdapter() }
    private val mealPreparationVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[MealPreparationVM::class.java] }
    override fun bindLayout() {
        binding = ActivityMealPreparationBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initData()
        initObj()
        initEvent()
    }

    private fun initObj() {
        passwordDialog = PasswordDialog(this)
        //注册网络状态监听
        NetworkStateManager.getInstance().registerObserver(this)
        //显示版本号和序列号
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}\nv${packageManager.getPackageInfo(packageName, 0).versionName}"
        binding.dishStatsCount.layoutManager = LinearLayoutManager(this)
        binding.dishStatsCount.adapter = dishVerifyCountAdapter
        //订阅订单列表
        mealPreparationVM.orderDishCount.observe(this) {
            val newList = it.sortedByDescending { i -> i.content.toInt() }
            dishVerifyCountAdapter.data = newList
        }
    }

    private fun initData() {
        mXService = MyService(this)
    }

    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventArrive(event: MessageEvent) {
        when (event.code) {

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
        }
    }

    private fun release() {
        //取消网络状态监听
        NetworkStateManager.getInstance().unRegisterObserver(this)
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }
        binding.btnSetting.setOnClickListener {
            passwordDialog.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@MealPreparationActivity, SettingActivity::class.java))
                    }
                })
            }
        }
        binding.btnFinish.setOnClickListener {
            var intent = Intent(this, MealPreparationFinishActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    override fun netWorkStatus(statue: String?) {
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

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: 接收到返回")
        }
    }
}