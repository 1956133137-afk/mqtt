package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
import com.yannuo.dgcanteen.databinding.FragmentScanBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.KeyboardListener
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.KeyboardUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 */
class ScanFragment : Fragment(), CallbackListener, KeyboardListener {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private lateinit var binding: FragmentScanBinding
    private val payViewModel by lazy { ViewModelProvider(requireActivity())[PayViewModel::class.java] }
    private lateinit var awaitPayDialog: AwaitingDialog
    private var countDown: CountDownTimer? = null
    private val handler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScanBinding.inflate(inflater, container, false)
        initEvent()
        initData()
        return binding.root
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            CommonAndDpToPxUtil.speakWork("取消支付")
            payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
            requireActivity().finish()
        }
    }

    private fun initData() {
        val data = arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)
        onCountDownTimer(binding.btnBack, kv.decodeInt(Constant.AWAIT_PAY_TIME, 30).toLong())
        binding.payTotalMoney.text = "￥${String.format(Locale.CHINA, "%.02f", data?.payment)}"
        when (data?.type) {
            Constant.PAY_IC_TYPE -> {
                CommonAndDpToPxUtil.speakWork("请刷卡支付")
                binding.payTitle.text = "请刷卡支付"
            }
            Constant.PAY_CODE_TYPE -> {
                CommonAndDpToPxUtil.speakWork("请出示二维码支付")
                binding.payTitle.text = "请出示二维码支付"
            }
            Constant.PAY_CODE_IC_TYPE -> {
                CommonAndDpToPxUtil.speakWork("请出示二维码或刷卡支付")
                binding.payTitle.text = "请出示二维码或刷卡支付"
            }
        }
        payViewModel.openPayStatus()
        payViewModel.listener = this
        payViewModel.mDishes = ProductsDetail(mutableListOf(), data?.payment.toString())
        binding.animationView.playAnimation()
        KeyboardUtil.instance.addObserver(this)
    }

    //刷卡返回数据
    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            try {

                if (this::awaitPayDialog.isInitialized && awaitPayDialog.isShowing) awaitPayDialog.dismiss()
                when (event) {
                    1 -> { //开始支付
                        if (!this::awaitPayDialog.isInitialized) awaitPayDialog = AwaitingDialog(requireActivity())
                        awaitPayDialog.show()
                        awaitPayDialog.updateText("支付中")
                    }
                    2 -> { //异常
                        ToastShowUtil.show("支付异常：$any")
                        LogUtil.d(TAG, "支付异常：$any")
                    }
                    3, 4 -> { //支付结果
                        countDown?.cancel()
                        val payForUI = any as PayForUI
                        LogUtil.d(TAG, Gson().toJson(payForUI))
                        val bean = SimpleForUI().apply {
                            custName = payForUI.username
                            payment = payForUI.actualPayment.ifEmpty { payForUI.payment }.toFloat()
                            accNo = payForUI.accNo
                            timestamp = payForUI.payTime
                            tranId = payForUI.traceId
                            orderId = payForUI.orderId
                            errorMsg = payForUI.errMsg
                            acc_bal = payForUI.accBal
                        }
                        if (payForUI.result == "Y") {
                            CommonAndDpToPxUtil.speakWork("支付成功")
                            val action = ScanFragmentDirections.actionScanToSuccess(bean)
                            findNavController().navigate(action)
                        } else {
                            val action = ScanFragmentDirections.actionScanToFail(bean)
                            findNavController().navigate(action)
                            CommonAndDpToPxUtil.speakWork("支付失败")
                        }
                    }
                    5 -> { //无效码
                        when (any as Int) {
                            1 -> {
                                ToastShowUtil.show("请刷新付款码再支付")
                                CommonAndDpToPxUtil.speakWork("无效码，请刷新付款码再支付")
                            }
                            2 -> {
                                ToastShowUtil.show("请检查网络,不支持离线聚合支付!")
                                CommonAndDpToPxUtil.speakWork("不支持离线聚合支付")
                            }
                            else -> {
                                ToastShowUtil.show("请切换离线码再支付")
                                CommonAndDpToPxUtil.speakWork("请切换离线码再支付")
                            }
                        }
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    6 -> { //异常
                        ToastShowUtil.show("支付异常：${any as? String}")
                        LogUtil.d(TAG, "支付异常：$any")
                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                    }
                    7 -> {
                        countDown?.cancel()
                        val bean = any as SimpleForUI
                        val str = when (bean.way!!.toInt()) {
                            20 -> "微信"
                            else -> "支付宝"
                        }
                        if (bean.state == 0) {
                            CommonAndDpToPxUtil.speakWork("${str}收款${bean.payment}元")
                            val action = ScanFragmentDirections.actionScanToSuccess(bean)
                            findNavController().navigate(action)
                        } else {
                            val action = ScanFragmentDirections.actionScanToFail(bean)
                            findNavController().navigate(action)
                            CommonAndDpToPxUtil.speakWork("${str}支付失败了")
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "${e.cause} ${e.message}")
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
//                countDown?.cancel()
                CommonAndDpToPxUtil.speakWork("支付超时")
                requireActivity().finish()
            }
        }
        countDown?.start()
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        binding.animationView.pauseAnimation();
        binding.animationView.cancelAnimation()
        LogUtil.d(TAG, "release")
        if (this::awaitPayDialog.isInitialized) awaitPayDialog.cancel()
        payViewModel.closePayStatus()
        countDown?.cancel()
        countDown = null
        KeyboardUtil.instance.removeObserver(this)
    }

    override fun keyboardMode(keyCode: Int, keyName: String) {
        when (keyCode) {
            42 -> {
                countDown?.cancel()
                countDown = null
                requireActivity().finish()
            }
        }
    }

    override fun computerMode(value: Double) {

    }
}