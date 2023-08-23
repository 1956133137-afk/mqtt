package com.yannuo.dgcanteen.activitys.fragment

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.common.CameraAIDL
import com.yannuo.dgcanteen.databinding.FragmentFacBinding
import com.yannuo.dgcanteen.databinding.FragmentScanBinding
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.*


class FacFragment : Fragment(), IProductsVM {
    private lateinit var binding: FragmentFacBinding
    private val TAG = javaClass.simpleName

    private lateinit var mScope: CoroutineScope
    private var resume = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFacBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
        mScope = CoroutineScope(Dispatchers.IO  )
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        resume = true
        LogUtil.i(TAG,"resume...")
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i(TAG,"onStop...")
        resume = false
    }


    private fun initObject() {
        arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)?.also {
            binding.payTotalMoney.text = "￥${it.payment}"
            CameraAIDL.startCamera(it.payment,this@FacFragment)
        }

    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
//            CommonAndDpToPxUtil.speakWork("取消支付")
            requireActivity().finish()
        }
    }

    override fun onFacePayResult(data: PayResultForUI) {
        val bean = SimpleForUI().apply {
            custName = data.cust_name
            payment = data.payment?.toFloat() ?: 0.0f
            accNo = data.acc_no.toString()
            timestamp = data.timestamp.toString()
            tranId = data.traceid ?: "---"
            orderId = data.orderid.toString()
            errorMsg = data.errormsg.toString()
            acc_bal = data.acc_bal
        }
        mScope.launch {
            repeat(300) {
                if (isStateSaved && !resume) {
                    delay(20)
                    return@repeat
                }
                withContext(Dispatchers.Main){
                    if (data.result == PayResultForUI.Result.SUCCESS) {
                        CommonAndDpToPxUtil.speakWork("支付成功")
                        val action = FacFragmentDirections.actionScanToSuccess(bean)
                        findNavController().navigate(action)
                    } else {
                        val action = FacFragmentDirections.actionScanToFail(bean)

                        CommonAndDpToPxUtil.speakWork("支付失败")
                        findNavController().navigate(action)
                    }
                    LogUtil.d(TAG,"wait isStateSaved")
                    cancel()
                }
            }
        }

    }

}