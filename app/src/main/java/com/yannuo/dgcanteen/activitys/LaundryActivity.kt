package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.LaundryModeVM
import com.yannuo.dgcanteen.adapters.LaundryRecordAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityLaundryBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.LaundryRecord
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil

class LaundryActivity : BaseActivity<ActivityLaundryBinding>(), NetworkStateManager.NetWorkListener {
    private val laundryModeVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[LaundryModeVM::class.java] }
    private val kv = MMKV.defaultMMKV()
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog? = null
    private var awaitingDialog: AwaitingDialog? = null
    private var clickTimes: Int = 0
    private var currentTime: Long = 0L
    private val laundryRecordAdapter by lazy { LaundryRecordAdapter() }

    override fun bindLayout() {
        binding = ActivityLaundryBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    private fun initObject() {
        mXService = MyService(this)
        laundryModeVM.getAwaitStatus().observe(this) {
            if (awaitingDialog == null) awaitingDialog = AwaitingDialog(this)
            if (it.isNotEmpty()) {
                if (awaitingDialog?.isShowing != true) awaitingDialog?.show()
                awaitingDialog?.updateText(it)
            } else awaitingDialog?.dismiss()
        }
        laundryModeVM.getUserName().observe(this) {
            binding.tipsUser.visibility = if (it.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            binding.btnLaundryRecord.visibility = if (it.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            if (it.isNotEmpty()) {
                binding.btnSetting.visibility = View.GONE
            } else {
                binding.btnSetting.visibility = View.VISIBLE
            }
            binding.userName.text = it.toString()
        }
        NetworkStateManager.getInstance().registerObserver(this)

        // Init RecyclerView for laundry records
        binding.rvLaundryRecord.layoutManager = LinearLayoutManager(this)
        binding.rvLaundryRecord.adapter = laundryRecordAdapter
        val records = getLaundryRecords().toMutableList()
        records.sortBy { it.index }
        laundryRecordAdapter.setData(records)
    }

    private fun initEvent() {
        binding.btnLaundryRecord.setOnClickListener {
            binding.laundryNavHost.visibility = View.GONE
            binding.laundryRecordView.visibility = View.VISIBLE
            binding.btnLaundryRecord.visibility = View.GONE
        }

        binding.btnLaundryRecordBack.setOnClickListener {
            binding.laundryNavHost.visibility = View.VISIBLE
            binding.laundryRecordView.visibility = View.GONE
            binding.btnLaundryRecord.visibility = View.VISIBLE
        }

        binding.imServer.setOnClickListener {
            if (System.currentTimeMillis() - currentTime < 1000) {
                clickTimes++
                if (clickTimes == 4) {
                    mXService?.hideNavBar = false
                    ToastShowUtil.show("导航可用")
                }
            } else {
                currentTime = System.currentTimeMillis()
                clickTimes = 0
            }
        }
        binding.tvTitle.setOnLongClickListener {
            val currentHideStatus = mXService?.hideNavBar ?: true
            val navigation = !currentHideStatus
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }
        binding.btnSetting.setOnClickListener { 
            if (laundryModeVM.getUserName().value.isNullOrEmpty()) {
                if (passwordDialog == null) passwordDialog = PasswordDialog(this)
                passwordDialog?.apply {
                    show()
                    // This line is problematic, as tvBack is not a member of ActivityLaundryBinding
                    // binding.tvBack.text = "输入密码"
                    setListener(object : CloseEvent {
                        override fun onEvent(code: Int, msg: String?) {
                            startActivity(Intent(this@LaundryActivity, SettingActivity::class.java))
                        }
                    })
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            val inputMethodManager = MyApplication.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager != null && view != null) inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
        return super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
    }

    override fun netWorkStatus(statue: String?) {
        runOnUiThread {
            when (statue) {
                "0" -> {
                    kv.encode(Constant.SWITCH, false)
                    binding.imServer.setImageResource(R.drawable.ic_server)
                }
                else -> {
                    kv.encode(Constant.SWITCH, true)
                    binding.imServer.setImageResource(R.drawable.ic_server_no)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        passwordDialog?.cancel()
        awaitingDialog?.cancel()
        NetworkStateManager.getInstance().unRegisterObserver(this)
    }

    private fun getLaundryRecords(): List<LaundryRecord> {
        // This is mock data. Replace with actual data from your source.
        return listOf(
            LaundryRecord(3, "2023-10-27 12:00", "2023-10-28 14:00", "毛巾, 床单"),
            LaundryRecord(1, "2023-10-27 10:00", "2023-10-28 12:00", "衬衫, 裤子"),
            LaundryRecord(2, "2023-10-27 11:00", "2023-10-28 13:00", "外套, T恤")
        )
    }
}
