package com.yannuo.dgcanteen.activitys.fragment

import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentFacBinding
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FacFragment : BaseFragment<FragmentFacBinding>() {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private val mmkv = MMKV.defaultMMKV()
    private var clickStartTime = 0L
    private var clickTimes = 0

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFacBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
    }

    private fun initObject() {
        EventBus.getDefault().register(this)
        var payMoney = 0.0f
        arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)?.also {
            binding.payTotalMoney.text = "￥${it.payment}"
            payMoney = it.payment
        }
        FaceScanVM.instance.bindService()
        FaceScanVM.instance.startFacePay(false, String.format("%.02f", payMoney))
        FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
            override fun onFacePay(payForUI: PayForUI) {
                onFacePayResult(payForUI)
            }

            override fun onFaceQuery(bean: CcbFacePayResultBean) {

            }
        })
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
//            CommonAndDpToPxUtil.speakWork("取消支付")
            requireActivity().finish()
        }
        binding.backAutoPay.setOnClickListener {
            if (clickTimes == 0) clickStartTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - clickStartTime < 1000) clickTimes++ else clickTimes = 0
            if (clickTimes == 3) {
                clickTimes = 0
//                binding.btnClose.visibility = View.VISIBLE
                mmkv.encode(Constant.AUTO_PAY, false)
                requireActivity().finish()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventCalculate(event: MessageEvent) {
        handler.post {
            when (event.code) {
                Constant.EVENT_QUOTA_CHANGE -> {
                    FaceScanVM.instance.stopScanFace()
                }
            }
        }
    }

    private fun onFacePayResult(payForUI: PayForUI) {
        LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~：$payForUI")
        handler.postDelayed({
            val bean = SimpleForUI().apply {
                way = payForUI.payType
                timestamp = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
            }
            LogUtil.d(TAG, "onFacePayResult: ${Gson().toJson(bean)}")

            if (payForUI.result == "Y") {
                bean.apply {
                    payment = payForUI.actualPayment.ifEmpty { payForUI.payment }
                    custName = payForUI.username
                    accNo = payForUI.accNo
                    acc_bal = payForUI.accBal
                    orderId = payForUI.orderId
                }
                CommonAndDpToPxUtil.speakWork("支付成功")
                val action = FacFragmentDirections.actionScanToSuccess(bean)
                findNavController().navigate(action)
            } else {
                bean.errorMsg = payForUI.errMsg
                CommonAndDpToPxUtil.speakWork("支付失败")
                val action = FacFragmentDirections.actionScanToFail(bean)
                findNavController().navigate(action)
            }
        }, 300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        FaceScanVM.instance.setFaceListener(null)
    }
}