package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.presenters.CardPresenter
import com.yannuo.dgcanteen.activitys.presenters.CodePresenter
import com.yannuo.dgcanteen.databinding.FragmentScanBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 */
class ScanFragment : Fragment(), CallbackListener {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentScanBinding
    private lateinit var cardPresenter: CardPresenter
    private lateinit var codePresenter: CodePresenter
    private lateinit var awaitPayDialog: AwaitingDialog
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
        if (!this::codePresenter.isInitialized) codePresenter = CodePresenter()
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
        binding.payTotalMoney.text = "￥${String.format(Locale.CHINA, "%.02f", data?.payment)}"
        when (data?.type) {
            Constant.PAY_IC_TYPE -> {
                CommonAndDpToPxUtil.speakWork("请刷卡支付")
                binding.payTitle.text = "请刷卡支付"
                scanCardPay(data.payment)
            }
            Constant.PAY_CODE_TYPE -> {
                CommonAndDpToPxUtil.speakWork("请出示二维码支付")
                binding.payTitle.text = "请出示二维码支付"
                scanCodePay(data.payment)
            }
            Constant.PAY_CODE_IC_TYPE -> {
                CommonAndDpToPxUtil.speakWork("请出示二维码或刷卡支付")
                binding.payTitle.text = "请出示二维码或刷卡支付"
                scanCardPay(data.payment)
                scanCodePay(data.payment)
            }
        }
    }

    private fun scanCardPay(payment: Float) {
        cardPresenter.initCard()
        cardPresenter.listener = this
        cardPresenter.openIcCard(payment)
    }

    private fun scanCodePay(payment: Float) {
        codePresenter.initCode()
        codePresenter.listener = this
        codePresenter.openQrCode(payment)
    }

    //刷卡返回数据
    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            when (event) {
                1 -> { //开始支付
                    if (!this::awaitPayDialog.isInitialized) awaitPayDialog =
                        AwaitingDialog(requireActivity())
                    awaitPayDialog.show()
                    awaitPayDialog.updateText("支付中")
                }
                2 -> { //异常
                    awaitPayDialog.dismiss()
                    ToastShowUtil.show("支付异常：$any")
                    LogUtil.d(TAG, "支付异常：$any")
                }
                3, 4 -> { //支付结果
                    awaitPayDialog.dismiss()
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
                5 -> { //无效码
                    awaitPayDialog.dismiss()
                    when (any as Int) {
                        1 -> {
                            ToastShowUtil.show("请刷新付款码再支付")
                            CommonAndDpToPxUtil.speakWork("请刷新付款码再支付")
                        }
                        else -> {
                            ToastShowUtil.show("请刷新付款码再支付")
                            CommonAndDpToPxUtil.speakWork("请切换离线码再支付")
                        }
                    }
                    codePresenter.setPayStatus()
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
        if (this::awaitPayDialog.isInitialized) awaitPayDialog.cancel()
        cardPresenter.closeIcCard()
        codePresenter.closeQrCode()
        countDown?.cancel()
        super.onDestroy()
    }
}