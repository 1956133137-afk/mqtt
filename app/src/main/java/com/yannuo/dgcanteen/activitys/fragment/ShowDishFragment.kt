package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.DialogShowDishBinding
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil

class ShowDishFragment : BaseFragment<DialogShowDishBinding>() {
    private var countDownTimer: CountDownTimer? = null
    private val kv by lazy { MMKV.defaultMMKV() }
    private var clickStartTime = 0L
    private var clickTimes = 0

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = DialogShowDishBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        val success: ShowDishFragmentArgs by navArgs()
        val dish = StringBuilder()
        for (s in success.verification.dish!!) {
            dish.append("$s\n")
        }
        binding.tvDish.text = dish
        val window = StringBuilder()
        for (w in success.verification.window!!) {
            window.append("$w\n")
        }
        binding.tvWindow.text = window
        val unVerfyDish = StringBuilder()
        for (un in success.verification.unDish!!) {
            unVerfyDish.append("$un\n")
        }
        CommonAndDpToPxUtil.speakWork("${success.verification.personName}核销成功")
        binding.tvNotDish.text = unVerfyDish
        binding.tvTime.text = success.verification.time
        binding.personName.text = success.verification.personName
        binding.btnBack.setOnClickListener { requireActivity().finish() }
//        binding.btnClose.setOnClickListener {
//            kv.encode(Constant.AUTO_VERIFY, false)
//            requireActivity().finish()
//        }
        binding.backAutoVerify.setOnClickListener {
            if (clickTimes == 0) clickStartTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - clickStartTime < 1000) clickTimes++ else clickTimes = 0
            if (clickTimes == 3) {
                clickTimes = 0
//                binding.btnClose.visibility = View.VISIBLE
                kv.encode(Constant.AUTO_VERIFY, false)
                requireActivity().finish()
            }
        }
    }

    private fun startCountDown() {
        countDownTimer = object : CountDownTimer(kv.decodeInt(Constant.MEAL_TIME, 10) * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.btnBack.text = "返回(${millisUntilFinished / 1000}秒)"
            }

            override fun onFinish() {
                requireActivity().finish()
            }
        }.start()
    }

    private fun stopCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    override fun onStart() {
        super.onStart()
        if (countDownTimer == null) {
            startCountDown()
        }
        kv.encode(Constant.VERIFY_CHANGE, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountDown()
    }

}