package com.yannuo.dgcanteen.activitys.fragment

import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentFaceVerificationBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.CcbFacePayResultBean
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.VerificationUI
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.Utils

class FaceVerificationFragment : BaseFragment<FragmentFaceVerificationBinding>(), CallbackListener {
    private val kv by lazy { MMKV.defaultMMKV() }
    private val verificationVM by lazy { VerificationVM() }
    private val handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFaceVerificationBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        verificationVM.setListener(this)
        initObject()
    }

    private fun initObject() {
        FaceScanVM.instance.bindService()
        FaceScanVM.instance.startFacePay(true)
        FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
            override fun onFacePay(payForUI: PayForUI) {

            }

            override fun onFaceQuery(bean: CcbFacePayResultBean) {
                val payCfg = verificationVM.getPayCfg()
                if (bean.RESULT == "Y") {
                    verificationVM.verification(payCfg.campusId, payCfg.businessId, bean.CUST_ID, null, Utils.getSN(), null, 0)
                } else {
                    handler.postDelayed({
                        val verificationUI = VerificationUI().apply {
                            errorMsg = bean.ERRMSG
                            time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                        }
                        val toFail = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToFailedFragment(verificationUI)
                        findNavController().navigate(toFail)
                    }, 300)
                }
            }
        })
//        if (requireActivity().intent.extras?.getString("verify") != null) {
//            LogUtil.d(TAG, "进入核销结果界面")
//            val verify = requireActivity().intent.extras?.getString("verify")
//            val verificationUI = Gson().fromJson(verify, VerificationUI::class.java)
//            val skip = requireActivity().intent.extras?.getInt("id")
//            if (skip == 0) {
//                val toSuccess = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToShowDishFragment(verificationUI)
//                findNavController().navigate(toSuccess)
//            }else {
//                val toFailed = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToFailedFragment(verificationUI)
//                findNavController().navigate(toFailed)
//            }
//        }
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
    }

    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            when (event) {
                0 -> {
                    LogUtil.d(TAG, "核销成功")
                    val verificationUI = any as VerificationUI
                    val toSuccess = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToShowDishFragment(verificationUI)
                    findNavController().navigate(toSuccess)
                }

                10 -> {
                    LogUtil.d(TAG, "核销失败")
                    val verificationUI = any as VerificationUI
                    val toFailed = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToFailedFragment(verificationUI)
                    findNavController().navigate(toFailed)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        FaceScanVM.instance.setFaceListener(null)
    }
}