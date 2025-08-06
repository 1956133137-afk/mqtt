package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.FragmentSuccessBinding
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import java.util.concurrent.TimeUnit

class SuccessFragment : Fragment() {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentSuccessBinding
    private lateinit var kv: MMKV
    private var countDown: CountDownTimer? = null
    private var clickStartTime = 0L
    private var clickTimes = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSuccessBinding.inflate(inflater, container, false)
        kv = MMKV.defaultMMKV()
        initEvent()
        initData()
        return binding.root
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            countDown?.cancel()
            requireActivity().finish()
        }
//        binding.btnClose.setOnClickListener {
//            kv.encode(Constant.AUTO_PAY, false)
//            requireActivity().finish()
//        }
        binding.backAutoPay.setOnClickListener {
            if (clickTimes == 0) clickStartTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - clickStartTime < 1000) clickTimes++ else clickTimes = 0
            if (clickTimes == 3) {
                clickTimes = 0
//                binding.btnClose.visibility = View.VISIBLE
                kv.encode(Constant.AUTO_PAY, false)
                requireActivity().finish()
            }
        }
    }

    private fun initData() {
        val data: SuccessFragmentArgs by navArgs()
        val decodeBool = kv.decodeBool(Constant.WHETHER_SHOW_PAYMENT, true)
        binding.payTotalMoney.visibility = if (decodeBool) View.VISIBLE else View.GONE
        data.simpleForUI.apply {
            when (way!!.toInt()) {
                20, 21 -> {
                    binding.payTotalMoney.text = "￥$payment"
                    binding.tradTime.text = timestamp
                    binding.tradNumber.text = tranId
                }
                else -> {
                    binding.tvName.text = custName
                    binding.payTotalMoney.text = "￥$payment"
                    binding.tvAccount.text = accNo
                    binding.tradTime.text = timestamp
                    binding.tradNumber.text = tranId
                    binding.orderNumber.text = orderId
                    binding.orderBalance.text = if(acc_bal != null && acc_bal!!.isNotEmpty()) "$acc_bal 元" else ""
                }
            }

        }
        onCountDownTimer(binding.btnBack, kv.decodeInt(Constant.SHOW_TIME, 5).toLong())
    }

    private fun onCountDownTimer(btnBack: Button?, time: Long) {
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
            override fun onTick(mil: Long) {
                btnBack?.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
//                countDown?.cancel()
                requireActivity().finish()
            }
        }
        countDown?.start()
    }

    override fun onDestroy() {
        countDown?.cancel()
        super.onDestroy()
    }
}














