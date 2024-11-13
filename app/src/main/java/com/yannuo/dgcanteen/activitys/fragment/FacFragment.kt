package com.yannuo.dgcanteen.activitys.fragment

import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentFacBinding
import com.yannuo.dgcanteen.model.CcbFacePayResultBean
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil

class FacFragment : BaseFragment<FragmentFacBinding>() {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFacBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
    }

    private fun initObject() {
        FaceScanVM.instance.bindService()
        var payMoney = 0.0f
        arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)?.also {
            binding.payTotalMoney.text = "￥${it.payment}"
            payMoney = it.payment
        }
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
    }

    private fun onFacePayResult(payForUI: PayForUI) {
        LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~")
        handler.postDelayed({
            val bean = SimpleForUI().apply {
                way = payForUI.payType
                timestamp = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
            }
            LogUtil.d(TAG, "onFacePayResult: ${Gson().toJson(bean)}")

            if (payForUI.result == "Y") {
                bean.apply {
                    payment = payForUI.payment.toFloat()
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
}