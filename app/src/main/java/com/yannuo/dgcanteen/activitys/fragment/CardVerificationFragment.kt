package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.DisplayCardVerificationBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.CcbFacePayResultBean
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.VerificationUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.Utils
import java.util.concurrent.TimeUnit

class CardVerificationFragment : BaseFragment<DisplayCardVerificationBinding>(), CallbackListener {
    private lateinit var verificationVM: VerificationVM
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    private var countDown: CountDownTimer? = null
    val handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = DisplayCardVerificationBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        initOpear()
        initEvent()
    }

    private fun initOpear() {
        if (!this::verificationVM.isInitialized) verificationVM = VerificationVM()
        onCountDownTimer(binding.btnBack, kv.decodeInt(Constant.AWAIT_PAY_TIME, 30).toLong())
        verificationVM.openQrCode()
        verificationVM.setListener(this)
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
        binding.toFace.setOnClickListener {
            faceVerification()
        }
    }

    private fun onCountDownTimer(btnBack: Button?, time: Long) {
        countDown?.cancel()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(time) + 200, 1000) {
            override fun onTick(mil: Long) {
                btnBack?.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
                requireActivity().finish()
            }
        }
        countDown?.start()
    }

    /**
     * 通过人脸查询人员信息
     */
    private fun faceVerification() {
        LogUtil.d(TAG, "查询人脸信息~")
        FaceScanVM.instance.bindService()
        FaceScanVM.instance.startFacePay(true)
        FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
            override fun onFacePay(payForUI: PayForUI) {

            }

            override fun onFaceQuery(bean: CcbFacePayResultBean) {
                val payCfg = verificationVM.getPayCfg()
                if (bean.RESULT == "Y") {
                    verificationVM.verification(payCfg.campusId, payCfg.businessId, bean.CUST_ID, null, Utils.getSN(), null, 0)
                }else {
                    handler.postDelayed({
                        val verificationUI = VerificationUI().apply {
                            errorMsg = bean.ERRMSG
                            time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                        }
                        val toFail = CardVerificationFragmentDirections.actionCardVerificationFragmentToFailedFragment(verificationUI)
                        findNavController().navigate(toFail)
                    }, 300)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationVM.closeQrCode()
        countDown?.cancel()
        countDown = null
    }

    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            when (event) {
                0 -> {
                    LogUtil.d(TAG, "核销成功")

                    val verificationUI = any as VerificationUI
                    val toSuccess = CardVerificationFragmentDirections.actionCardVerificationFragmentToShowDishFragment(verificationUI)
                    findNavController().navigate(toSuccess)

                }

                10 -> {
                    LogUtil.d(TAG, "核销失败")
                    val verificationUI = any as VerificationUI
                    val toFail = CardVerificationFragmentDirections.actionCardVerificationFragmentToFailedFragment(verificationUI)
                    findNavController().navigate(toFail)
                }
            }
        }
    }

}