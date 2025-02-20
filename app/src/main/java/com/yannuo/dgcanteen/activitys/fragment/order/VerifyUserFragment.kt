package com.yannuo.dgcanteen.activitys.fragment.order

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.databinding.FragmentVerifyUserBinding
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.views.HintDialog
import java.util.concurrent.TimeUnit

class VerifyUserFragment : BaseFragment<FragmentVerifyUserBinding>() {
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private var hintDialog: HintDialog? = null
    private var countDown: CountDownTimer? = null
    private var orderType: String = "0"

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentVerifyUserBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
//        initObject()
        initEvent()
    }

    private fun initObject() {
        orderMealVM.setOrderListener(object : OrderMealVM.OrderMealListener {
            override fun onOrderResult(type: Int, any: Any) {
                handler.post {
                    endLogin()
                    when (type) {
                        -1 -> orderMealVM.getAwaitStatus().value = "$any"
                        else -> {
                            orderMealVM.getAwaitStatus().value = ""
                            val orderForUI = any as OrderForUI
                            LogUtil.d(TAG, Gson().toJson(orderForUI))
                            when (type) {
                                0 -> {
                                    onCountDownTimer(10L)
                                    binding.errCode.text = orderForUI.errCode
                                    binding.errMsg.text = orderForUI.errMsg
                                    binding.err.visibility = View.VISIBLE
                                }
                                1 -> {
                                    val skipPage = VerifyUserFragmentDirections.verifyUserToUserOrder(orderForUI)
                                    findNavController().navigate(skipPage)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun initEvent() {
        binding.btnFaceScan.setOnClickListener { startLogin("1") }
        binding.btnQrCode.setOnClickListener { startLogin("2") }
        binding.btnIcCard.setOnClickListener { startLogin("3") }
    }

    private fun startLogin(type: String) {
        orderType = type
        btnEnabled(false)
        tipsDialog(orderType)
        orderMealVM.setOrderStatus(OrderMealVM.OrderStatus.AWAIT)
        orderMealVM.open(orderType, true)
    }

    private fun endLogin() {
        btnEnabled(true)
        hintDialog?.dismiss()
        orderMealVM.setOrderStatus(OrderMealVM.OrderStatus.INVALID)
        orderMealVM.close(orderType)
    }

    private fun btnEnabled(boolean: Boolean) {
        binding.btnFaceScan.isEnabled = boolean
        binding.btnQrCode.isEnabled = boolean
        binding.btnIcCard.isEnabled = boolean
    }

    override fun onResume() {
        super.onResume()
        btnEnabled(true)
        initObject()
    }

    private fun tipsDialog(type: String) {
        if (type == "1") return
        if (hintDialog == null) hintDialog = HintDialog(requireContext())
        hintDialog?.setListener(object : CloseEvent {
            override fun onEvent(code: Int, msg: String?) {
                handler.post {
                    if (code == 0) endLogin()
                }
            }
        })
        hintDialog?.show()
        if (type == "2" || type == "3") {
            val str = if (type == "2") "请将二维码对准扫码器" else "请将卡片贴近读卡器"
            hintDialog?.setTipsText(str)
            CommonAndDpToPxUtil.speakWork(str)
        }
    }

    private fun onCountDownTimer(time: Long) {
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
            override fun onTick(mil: Long) {}

            override fun onFinish() {
                binding.err.visibility = View.INVISIBLE
            }
        }
        countDown?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        hintDialog?.cancel()
    }
}