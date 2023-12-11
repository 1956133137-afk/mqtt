package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.size
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.SimpleDisplayBinding
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import org.greenrobot.eventbus.EventBus
import java.io.File

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
    private var havePic = false
    private var atv : AppCompatActivity ?= null


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
        try {
            val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (file.exists()) {
                val fil = File(file, "轮播图片")
                if (fil.exists()) {
                    val files = fil.listFiles()
//                    val length = Math.min(files.size, 5)
                    val length = files.size
                    for (i in 0 until length) {
                        val img = ImageView(context)
                        img.scaleType = ImageView.ScaleType.FIT_XY
                        img.layoutParams =
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        Glide.with(context).load(files[i]).diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(img)
                        binding.vfSlidesshow.addView(img)
                    }
                    havePic = files.isNotEmpty()
                }
            }
                kv = MMKV.defaultMMKV()
                if (kv.decodeString(Constant.TITLE_CONTENT) == null || kv.decodeString(Constant.TITLE_CONTENT) == "") {
                    when(havePic){
                        true ->{
                            binding.tvFpTitle.visibility = View.VISIBLE
                            binding.vfSlidesshow.visibility = View.VISIBLE
                            binding.tvZ.visibility = View.GONE
                            binding.tvFpTitle.text = "智慧食堂"
                        }
                        false ->{
                            binding.tvZ.text = "智慧食堂"
                        }
                    }
                } else {
                    var strText = kv.decodeString(Constant.TITLE_CONTENT, "")
                    when(havePic){
                        true ->{
                            binding.tvFpTitle.visibility = View.VISIBLE
                            binding.vfSlidesshow.visibility = View.VISIBLE
                            binding.tvZ.visibility = View.GONE
                            binding.tvFpTitle.text = "$strText•智慧食堂"
                        }
                        false ->{
                            strText = "${strText}\n智慧食堂"
                            val str = SpannableString(strText)
                            str.setSpan(AbsoluteSizeSpan(180), 0, strText.length - 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            binding.tvZ.text = str
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
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
        binding.vSetting.setOnLongClickListener {iit ->
            atv?.also {
                val intent = Intent(it, SettingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                it.startActivity(intent)
//                it.finish()
//                cancel()
            }
            true
        }
    }


    fun setActivity(  atv : AppCompatActivity){
        this.atv = atv
    }


    override fun show() {
        super.show()
        if (havePic && binding.vfSlidesshow.size > 1) binding.vfSlidesshow.startFlipping()
    }


    override fun cancel() {
        super.cancel()
        if (havePic && binding.vfSlidesshow.size > 1) binding.vfSlidesshow.stopFlipping()

    }

    override fun onStop() {
        LogUtil.i(TAG, "stop...")
        super.onStop()
    }
}