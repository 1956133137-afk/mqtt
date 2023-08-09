package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.Display
import android.view.WindowManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.SimpleDisplayBinding
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: 简易异现
 * Date: 2023/8/4 15:41
 **/
class SimpleDisplay(context: Context, display: Display) : Presentation(context, display) {
    private val TAG = javaClass.simpleName

    private lateinit var binding: SimpleDisplayBinding
    private lateinit var kv: MMKV

    override fun onCreate(savedInstanceState: Bundle?) {
        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = SimpleDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        kv = MMKV.defaultMMKV()
        if (kv.decodeString(Constant.TITLE_CONTENT) == null || kv.decodeString(Constant.TITLE_CONTENT) == "") {
            binding.tvZ.text = "智慧食堂"
        } else {
            val strText = "${kv.decodeString(Constant.TITLE_CONTENT, "")}\n智慧食堂"
            val str = SpannableString(strText)
            str.setSpan(
                AbsoluteSizeSpan(180),
                0,
                strText.length - 4,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvZ.text = str
        }
    }

    override fun onStop() {
        LogUtil.i(TAG, "stop...")
        super.onStop()
    }
}