package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
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
import kotlinx.coroutines.withContext


class FacFragment : Fragment(), IProductsVM {
    private lateinit var binding: FragmentFacBinding
    private val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFacBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
        return binding.root
    }

    private fun initObject() {
        arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)?.also {
            CameraAIDL.startCamera(it.payment,this@FacFragment)
        }

    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            CommonAndDpToPxUtil.speakWork("取消支付")
            requireActivity().finish()
        }
    }

    override fun onFacePayResult(data: PayResultForUI) {
        val bean = SimpleForUI().apply {
            custName = data.cust_name.toString()
            payment = data.payment?.toFloat()!!
            accNo = data.acc_no.toString()
            timestamp = data.timestamp.toString()
            tranId = data.traceid ?: "---"
            orderId = data.orderid.toString()
            errorMsg = data.errormsg.toString()
        }
        requireActivity().runOnUiThread {
            if (data.result == PayResultForUI.Result.SUCCESS) {
                CommonAndDpToPxUtil.speakWork("支付成功")
                val action = FacFragmentDirections.actionFacFragmentToSuccessFragment(bean)
                findNavController().navigate(action)
            } else {
                val action = FacFragmentDirections.actionFacFragmentToFailFragment(bean)
                findNavController().navigate(action)
                CommonAndDpToPxUtil.speakWork("支付失败")
            }
        }
    }

}