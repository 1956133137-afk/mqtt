package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.FaceVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.DisplayCardVerificationBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.CcbFacePayResultBean
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.VerificationUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class CardVerificationFragment : BaseFragment<DisplayCardVerificationBinding>(), CallbackListener {
    private lateinit var verificationVM: VerificationVM
    private val kv by lazy { MMKV.defaultMMKV() }
    private var countDown: CountDownTimer? = null
    val handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = DisplayCardVerificationBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        EventBus.getDefault().register(this)
        initOpear()
        initEvent()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventArrive(event: MessageEvent) {
        when (event.code) {
            Constant.EVENT_LOCAL_FACE -> {
                Log.d(TAG, "eventCalculate: 接收到本地脸库人脸识别结果")
                val eventFaceBean = event.any as EventFaceBean
                when (eventFaceBean.code) {
                    -1 -> {
                        failure(eventFaceBean.msg)
                    }

                    else -> {
                        val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
                        if(payCfg == null){
                            failure("未获取到配置信息")
                            return
                        }
                        //识别成功
                        val bean = DishesDBHelper.getInstance().queryFaceByEigenvalue(eventFaceBean.msg)
                        if(bean == null) {
                            failure("查询不到人员信息")
                            return
                        }
                        verificationVM.verification(payCfg.campusId, payCfg.businessId, bean.custId, null, Utils.getSN(), null, 0)
                    }
                }
            }
        }
    }

    /**
     * 支付失败跳转
     */
    private fun failure(msg: String){
        handler.postDelayed({
            val verificationUI = VerificationUI().apply {
                errorMsg = msg
                time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
            }
            val toFail = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToFailedFragment(verificationUI)
            findNavController().navigate(toFail)
        }, 300)
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
        if(kv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
            val intent = Intent(requireContext(), FacialRecognitionActivity::class.java)
            requireContext().startActivity(intent)
        }else{
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
                    } else {
                        handler.postDelayed({
                            val verificationUI = VerificationUI().apply {
                                errorMsg = bean.ERRMSG
                                time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                            }
                            val toFail = CardVerificationFragmentDirections.actionCardVerificationFragmentToFailedFragment(verificationUI)
                            if (judgeIsAdded()) findNavController().navigate(toFail)
                        }, 300)
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        FaceScanVM.instance.setFaceListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationVM.closeQrCode()
        countDown?.cancel()
        countDown = null
        EventBus.getDefault().unregister(this)
    }

    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            when (event) {
                -1 -> countDown?.cancel()
                0 -> {
                    LogUtil.d(TAG, "核销成功")

                    val verificationUI = any as VerificationUI
                    val toSuccess = CardVerificationFragmentDirections.actionCardVerificationFragmentToShowDishFragment(verificationUI)
                    if (judgeIsAdded()) findNavController().navigate(toSuccess)

                }

                10 -> {
                    LogUtil.d(TAG, "核销失败")
                    val verificationUI = any as VerificationUI
                    val toFail = CardVerificationFragmentDirections.actionCardVerificationFragmentToFailedFragment(verificationUI)
                    if (judgeIsAdded()) findNavController().navigate(toFail)
                }
            }
        }
    }

    private fun judgeIsAdded(): Boolean = isAdded && findNavController().currentDestination?.id == R.id.cardVerificationFragment

}