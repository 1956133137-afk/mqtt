package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.FragmentFailedBinding
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant

class FailedFragment : BaseFragment<FragmentFailedBinding>() {
    private var countDownTimer: CountDownTimer? = null
    private val kv by lazy { MMKV.defaultMMKV() }
    private var clickStartTime = 0L
    private var clickTimes = 0

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFailedBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        initPrePar()
        initEvent()
    }

    private fun initPrePar() {
        val fail: FailedFragmentArgs by navArgs()
        CommonAndDpToPxUtil.speakWork(fail.verification.errorMsg)
        binding.tvMsg.text = fail.verification.errorMsg
        binding.tvTime.text = fail.verification.time
    }

    private fun initEvent() {
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