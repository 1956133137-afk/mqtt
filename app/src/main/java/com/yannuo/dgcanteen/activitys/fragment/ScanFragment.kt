package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.presenters.CardPresenter
import com.yannuo.dgcanteen.databinding.FragmentScanBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 */
class ScanFragment : Fragment(), CallbackListener {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentScanBinding
    private lateinit var cardPresenter: CardPresenter
    private lateinit var kv: MMKV
    private var countDown: CountDownTimer? = null
    private val handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScanBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
        initData()
        return binding.root
    }

    private fun initObject() {
        if (!this::cardPresenter.isInitialized) cardPresenter = CardPresenter()
        kv = MMKV.defaultMMKV()
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            CommonAndDpToPxUtil.speakWork("取消支付")
            requireActivity().finish()
        }
    }

    private fun initData() {
        val data = arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)
        onCountDownTimer(binding.btnBack, kv.decodeInt(Constant.AWAIT_PAY_TIME, 30).toLong())
        if (data?.type == Constant.PAY_IC_TYPE) {
            CommonAndDpToPxUtil.speakWork("请刷卡支付")
            binding.payTitle.text = "请刷卡支付"
            binding.payTotalMoney.text = "￥${String.format(Locale.CHINA, "%.02f", data.payment)}"
            scanCardPay(data.payment)
        }
    }

    private fun scanCardPay(payment: Float) {
        cardPresenter.initCard()
        cardPresenter.listener = this
        cardPresenter.openIcCard(payment)
    }

    //刷卡返回数据
    override fun onOtherListener(event: Int, any: Any?) {
        when (event) {
            3, 4 -> { //
                handler.post {
                    countDown?.cancel()
                    val data = any as PayResultForUI
                    LogUtil.d(TAG, Gson().toJson(data))
                    val bean = SimpleForUI().apply {
                        custName = data.cust_name.toString()
                        payment = data.payment?.toFloat()!!
                        accNo = data.acc_no.toString()
                        timestamp = data.timestamp.toString()
                        tranId = data.traceid.toString()
                        orderId = data.orderid.toString()
                        errorMsg = data.errormsg.toString()
                    }
                    if (data.result == PayResultForUI.Result.SUCCESS) {
                        CommonAndDpToPxUtil.speakWork("支付成功")
                        val action = ScanFragmentDirections.actionScanToSuccess(bean)
                        findNavController().navigate(action)
                    } else {
                        val action = ScanFragmentDirections.actionScanToFail(bean)
                        findNavController().navigate(action)
                        CommonAndDpToPxUtil.speakWork("支付失败")
                    }
                }
            }
        }
    }

    private fun onCountDownTimer(btnBack: Button?, time: Long) {
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
            override fun onTick(mil: Long) {
                btnBack?.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
                countDown?.cancel()
                CommonAndDpToPxUtil.speakWork("支付超时")
                requireActivity().finish()
            }
        }
        countDown?.start()
    }

    override fun onDestroy() {
        cardPresenter.closeIcCard()
        countDown?.cancel()
        super.onDestroy()
    }
}