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
import java.util.concurrent.TimeUnit

class SuccessFragment : Fragment() {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentSuccessBinding
    private lateinit var kv: MMKV
    private var countDown: CountDownTimer? = null

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
            requireActivity().finish()
        }
    }

    private fun initData() {
        val data: SuccessFragmentArgs by navArgs()
        data.simpleForUI.apply {
            binding.tvName.text = custName
            binding.payTotalMoney.text = "￥$payment"
            binding.tvAccount.text = accNo
            binding.tradTime.text = timestamp
            binding.tradNumber.text = tranId
            binding.orderNumber.text = orderId
        }
        onCountDownTimer(binding.btnBack, kv.decodeInt(Constant.SHOW_TIME, 2).toLong())
    }

    private fun onCountDownTimer(btnBack: Button?, time: Long) {
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
            override fun onTick(mil: Long) {
                btnBack?.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
                countDown?.cancel()
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














