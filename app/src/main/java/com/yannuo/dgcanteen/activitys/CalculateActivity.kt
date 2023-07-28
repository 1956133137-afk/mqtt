package com.yannuo.dgcanteen.activitys

import android.content.Intent
import com.proembed.service.MyService
import com.yannuo.dgcanteen.databinding.ActivityCalculateBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.util.ToastShowUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:46
 **/
class CalculateActivity : BaseActivity<ActivityCalculateBinding>() {

    private var mXService: MyService? = null
    private var navigation = true
    private lateinit var passwordDialog: PasswordDialog

    override fun bindLayout() {
        binding = ActivityCalculateBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)

        initEvent()
    }

    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
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
                        startActivity(Intent(this@CalculateActivity, SettingActivity::class.java))
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        passwordDialog.cancel()
    }

}