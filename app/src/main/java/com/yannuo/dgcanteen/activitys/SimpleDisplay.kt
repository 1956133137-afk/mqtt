package com.yannuo.dgcanteen.activitys

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import com.yannuo.dgcanteen.databinding.SimpleDisplayBinding
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: 简易异现
 * Date: 2023/8/4 15:41
 **/
class SimpleDisplay(context: Context, display: Display) : Presentation(context, display) {
    private val TAG = javaClass.simpleName

    private lateinit var binding: SimpleDisplayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = SimpleDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {

    }

    override fun onStop() {
        LogUtil.i(TAG, "stop...")
        super.onStop()
    }
}