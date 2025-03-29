package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityOrderMealBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil

class OrderMealActivity : BaseActivity<ActivityOrderMealBinding>(), NetworkStateManager.NetWorkListener {
    private val orderMealVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[OrderMealVM::class.java] }
    private val kv = MMKV.defaultMMKV()
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog? = null
    private var awaitingDialog: AwaitingDialog? = null
    private var navigation = true

    override fun bindLayout() {
        binding = ActivityOrderMealBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    private fun initObject() {
        mXService = MyService(this)
        orderMealVM.getAwaitStatus().observe(this) {
            if (awaitingDialog == null) awaitingDialog = AwaitingDialog(this)
            if (it.isNotEmpty()) {
                if (awaitingDialog?.isShowing != true) awaitingDialog?.show()
                awaitingDialog?.updateText(it)
            } else awaitingDialog?.dismiss()
        }
        orderMealVM.getUserName().observe(this) {
            binding.tipsUser.visibility = if (it.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            binding.btnOrderRecord.visibility = if (it.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            binding.btnSetting.visibility = if (it.isNotEmpty()) View.INVISIBLE else View.VISIBLE
            binding.userName.text = it.toString()
        }
        NetworkStateManager.getInstance().registerObserver(this)
    }

    private fun initEvent() {
        binding.tvTitle.setOnLongClickListener {
            navigation = !navigation
            mXService?.hideNavBar = navigation
            if (!navigation) ToastShowUtil.show("导航可用")
            true
        }
        binding.btnOrderRecord.setOnClickListener {
            val intent = Intent(this, OrderRecordActivity::class.java)
            intent.putExtra("custId", orderMealVM.getUserId())
            intent.putExtra("ccbToken", orderMealVM.getCcbToken())
            startActivity(intent)
        }
        binding.btnSetting.setOnClickListener {
            if (passwordDialog == null) passwordDialog = PasswordDialog(this)
            passwordDialog?.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@OrderMealActivity, SettingActivity::class.java))
                    }
                })
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
}