package com.yannuo.dgcanteen.activitys.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.FaceVM
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
import com.yannuo.dgcanteen.databinding.FragmentScanBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.KeyboardListener
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit

/**
 */
class ScanFragment : Fragment(), CallbackListener, KeyboardListener, FaceVM.OnListener {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private lateinit var binding: FragmentScanBinding
    private val payViewModel by lazy { ViewModelProvider(requireActivity())[PayViewModel::class.java] }
    private lateinit var awaitPayDialog: AwaitingDialog
    private var countDown: CountDownTimer? = null
    private val handler = Handler()
    private val confirmDialog by lazy { ConfirmDialog(requireContext()) }
    private var payData: PayForUI = PayForUI()
    private var clickStartTime = 0L
    private var clickTimes = 0
    private var payMoney = ""

    private val faceVM by lazy { FaceVM() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScanBinding.inflate(inflater, container, false)
        initEvent()
        initData()
        LogUtil.d(TAG, "开始 onCreateView")
        return binding.root
    }

    private fun initEvent() {
        EventBus.getDefault().register(this)
        binding.btnBack.setOnClickListener {
            CommonAndDpToPxUtil.speakWork("取消支付")
            payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
            requireActivity().finish()
        }
        binding.backAutoPay.setOnClickListener {
            if (clickTimes == 0) clickStartTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - clickStartTime < 1000) clickTimes++ else clickTimes = 0
            if (clickTimes == 3) {
                clickTimes = 0
//                binding.btnClose.visibility = View.VISIBLE
                kv.encode(Constant.AUTO_PAY, false)
                payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
                requireActivity().finish()
            }
        }
        binding.btnFacePay.setOnClickListener {
            if (kv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
                val intent = Intent(requireContext(), FacialRecognitionActivity::class.java)
                startActivity(intent)
                faceVM.setListener(this, payMoney)
            }else{
                facePay()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        val data = arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)
        if (!kv.decodeBool(Constant.AUTO_PAY, false)) onCountDownTimer(binding.btnBack, kv.decodeInt(Constant.AWAIT_PAY_TIME, 30).toLong())
        payMoney = String.format(Locale.CHINA, "%.02f", data?.payment)
        binding.payTotalMoney.text = "￥$payMoney"
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
        payViewModel.openPayStatus(data?.type ?: Constant.PAY_CODE_IC_TYPE)
        payViewModel.listener = this
        payViewModel.mDishes = ProductsDetail(mutableListOf(), data?.payment.toString())
        binding.animationView.playAnimation()
        KeyboardUtil.instance.addObserver(this)

        confirmDialog.setListener(object : ConfirmDialog.OnConfirmCallback {
            override fun confirmCallback(flag: Boolean) {
                if (flag) payViewModel.confirmPay(payData) else payViewModel.setPayState(PayViewModel.PayStatus.PAY)
                if (confirmDialog.isShowing) confirmDialog.dismiss()
            }
        })
    }

    private fun facePay() {
        LogUtil.d(TAG, "刷脸支付~")
        countDown?.cancel()
        FaceScanVM.instance.bindService()
        FaceScanVM.instance.startFacePay(false, payMoney)
        FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
            override fun onFacePay(payForUI: PayForUI) {
                LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~：$payForUI")
                handler.postDelayed({
                    val bean = SimpleForUI().apply {
                        custName = payForUI.username
                        payment = payForUI.actualPayment.ifEmpty { payForUI.payment }
                        accNo = payForUI.accNo
                        timestamp = payForUI.payTime
                        tranId = payForUI.traceId
                        orderId = payForUI.orderId
                        errorMsg = payForUI.errMsg
                        acc_bal = payForUI.accBal
                        accList = payForUI.accList
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
                }, 300)
            }

            override fun onFaceQuery(bean: CcbFacePayResultBean) {

            }
        })
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventCalculate(event: MessageEvent) {
        handler.post {
            when (event.code) {
                Constant.EVENT_QUOTA_CHANGE -> {
                    payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
                    requireActivity().finish()
                }
                Constant.EVENT_LOCAL_FACE -> {
                    Log.d(TAG, "eventCalculate: 接收到本地脸库人脸识别结果")
                    val eventFaceBean = event.any as EventFaceBean
                    when(eventFaceBean.code){
                        -1 -> {
                            handler.postDelayed({
                                val bean = SimpleForUI().apply {
                                    timestamp = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss",System.currentTimeMillis())
                                    errorMsg = eventFaceBean.msg
                                }
                                val action = ScanFragmentDirections.actionScanToFail(bean)
                                findNavController().navigate(action)
                                CommonAndDpToPxUtil.speakWork("支付失败")
                            }, 300)
                        }
                        else -> {
                            //识别成功
                            faceVM.localFacePay(eventFaceBean.msg,faceVM.getPayment(),eventFaceBean.img,eventFaceBean.searchScore.toString())
                        }
                    }
                }
            }
        }
    }

    //刷卡扫码返回数据
    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            try {
                payData = PayForUI()
                if (this::awaitPayDialog.isInitialized && awaitPayDialog.isShowing) awaitPayDialog.dismiss()
                when (event) {
                    1 -> { //开始支付
                        if (!this::awaitPayDialog.isInitialized) awaitPayDialog = AwaitingDialog(requireActivity())
                        awaitPayDialog.show()
                        awaitPayDialog.updateText("支付中")
                        countDown?.cancel()
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
                            payment = payForUI.actualPayment.ifEmpty { payForUI.payment }
                            accNo = payForUI.accNo
                            timestamp = payForUI.payTime
                            tranId = payForUI.traceId
                            orderId = payForUI.orderId
                            errorMsg = payForUI.errMsg
                            acc_bal = payForUI.accBal
                            accList = payForUI.accList
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
                    8 -> {
                        payData = any as PayForUI
                        if (!confirmDialog.isShowing) confirmDialog.show()
                        confirmDialog.setTextMsg("重复支付，您是否确定继续支付？")
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
                payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
                requireActivity().finish()
            }
        }
        countDown?.start()
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d(TAG, "停止 onPause")
        release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        FaceScanVM.instance.setFaceListener(null)
    }

    private fun release() {
        EventBus.getDefault().unregister(this)
        binding.animationView.pauseAnimation();
        binding.animationView.cancelAnimation()
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

    override fun uploadResult(code: Int, type: Boolean, msg: String) {

    }

    override fun facePayResult(payForUI: PayForUI) {
        handler.postDelayed({
            val bean = SimpleForUI().apply {
                custName = payForUI.username
                payment = payForUI.actualPayment.ifEmpty { payForUI.payment }
                accNo = payForUI.accNo
                timestamp = payForUI.payTime
                tranId = payForUI.traceId
                orderId = payForUI.orderId
                errorMsg = payForUI.errMsg
                acc_bal = payForUI.accBal
                accList = payForUI.accList
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
        }, 300)
    }
}