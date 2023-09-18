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
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.SimpleDisplayBinding
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import org.greenrobot.eventbus.EventBus

/**
 * Author: filowl
 * Description: 简易异现
 * Date: 2023/8/4 15:41
 **/
class SimpleDisplay(context: Context, display: Display) : Presentation(context, display) {
    private val TAG = javaClass.simpleName

    private lateinit var binding: SimpleDisplayBinding
    private lateinit var kv: MMKV
    private var lastTime = 0L  //上次触发时间

    override fun onCreate(savedInstanceState: Bundle?) {
        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = SimpleDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initEvent()
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

    fun enableBtn(money:String?){
        binding?.also {
            if (it.llPay.isVisible.not()){
                it.llPay.visibility = View.VISIBLE
            }
            binding.tvAmount.text = "￥:$money 元"
        }
    }

    private fun initEvent() {
        binding.btnFacePay.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000 )return@setOnClickListener
            lastTime = System.currentTimeMillis()
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY,  Constant.PAY_FACE_TYPE))
        }
        binding.btnIsPay.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000 )return@setOnClickListener
            lastTime = System.currentTimeMillis()
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_OTHER_PAY, Constant.PAY_CODE_IC_TYPE))
        }
    }

    override fun onStop() {
        LogUtil.i(TAG, "stop...")
        super.onStop()
    }
}