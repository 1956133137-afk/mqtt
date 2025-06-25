package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import com.yannuo.dgcanteen.databinding.SettingDisplayBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/22 17:02
 **/
class SettingDisplay(context: Context, display: Display) : BaseDisplay(context, display) {
    private lateinit var binding: SettingDisplayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
//        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = SettingDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}