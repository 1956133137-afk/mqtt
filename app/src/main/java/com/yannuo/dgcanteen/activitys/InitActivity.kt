package com.yannuo.dgcanteen.activitys

import android.content.Intent
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.ActivityIntiBinding
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 15:10
 **/
class InitActivity : BaseActivity<ActivityIntiBinding>() {

    private val kv = MMKV.defaultMMKV()

    override fun bindLayout() {
        binding = ActivityIntiBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initView()
        initEvent()
    }

    private fun initView() {
        when (kv.decodeInt(Constant.APP_MODE)) {
            1 -> {
                startActivity(Intent(this, CommodityActivity::class.java))
                finish()
            }
            2 -> {
                startActivity(Intent(this, CalculateActivity::class.java))
                finish()
            }
        }
    }

    private fun initEvent() {

        binding.select.setOnClickListener {
            kv.encode(Constant.APP_MODE, 1)
            startActivity(Intent(this, CommodityActivity::class.java))
            finish()
        }

        binding.pay.setOnClickListener {
            kv.encode(Constant.APP_MODE, 2)
            startActivity(Intent(this, CalculateActivity::class.java))
            finish()
        }
    }

}