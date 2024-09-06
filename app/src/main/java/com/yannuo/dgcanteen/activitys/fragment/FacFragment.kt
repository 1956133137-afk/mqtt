package com.yannuo.dgcanteen.activitys.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.HostActivity
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.common.CameraAIDL
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentFacBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import kotlinx.coroutines.*

class FacFragment : BaseFragment<FragmentFacBinding>(), IProductsVM {
    private val productsVM by lazy {
        ViewModelProvider(requireActivity())[ProductsVM::class.java]
    }
    val handler = Handler(MyApplication.applicationContext.mainLooper)
    private var mFacePayService: ZHSTFacePayService? = null

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, "onServiceConnected")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.d(TAG, " onServiceDisconnected")
        }
    }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFacBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
    }

    private fun loadData() {
        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        requireContext().bindService(lIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }


    private fun initObject() {
        loadData()
        productsVM.listener = this
        var payMoney = 0.0f
        arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)?.also {
            binding.payTotalMoney.text = "￥${it.payment}"
            payMoney = it.payment
        }
        handler.post {
            productsVM.startPayWithFace(mFacePayService, payMoney)
        }

    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
//            CommonAndDpToPxUtil.speakWork("取消支付")
            requireActivity().finish()
        }
    }
    override fun onFacePayResult(payForUI: PayForUI) {
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
            } else if (payForUI.result == "N") {
                bean.apply {
                    errorMsg = payForUI.errMsg
                }
                CommonAndDpToPxUtil.speakWork("支付失败")
                val action = FacFragmentDirections.actionScanToFail(bean)
                findNavController().navigate(action)
            }
        }, 300)
    }

}