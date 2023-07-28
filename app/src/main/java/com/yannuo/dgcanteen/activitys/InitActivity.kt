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
        binding.order.text = Constant.ORDERING_FOOD_MODE
        binding.collection.text = Constant.PROCEEDS_MODE

        when (kv.decodeString(Constant.APP_MODE)) {
            Constant.ORDERING_FOOD_MODE -> {
                startActivity(Intent(this, CommodityActivity::class.java))
                finish()
            }
            Constant.PROCEEDS_MODE -> {
                startActivity(Intent(this, CalculateActivity::class.java))
                finish()
            }
        }
    }

    private fun initEvent() {

        binding.order.setOnClickListener {
            kv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
            startActivity(Intent(this, CommodityActivity::class.java))
            finish()
        }

        binding.collection.setOnClickListener {
            kv.encode(Constant.APP_MODE, Constant.PROCEEDS_MODE)
            startActivity(Intent(this, CalculateActivity::class.java))
            finish()
        }
    }

}