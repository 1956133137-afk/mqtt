package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentFaceVerificationBinding
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

class FaceVerificationFragment : BaseFragment<FragmentFaceVerificationBinding>(), CallbackListener {
    private val kv by lazy { MMKV.defaultMMKV() }
    private val verificationVM by lazy { VerificationVM() }
    private val handler = Handler(MyApplication.applicationContext.mainLooper)

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFaceVerificationBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        EventBus.getDefault().register(this)
        verificationVM.setListener(this)
        initObject()
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

    private fun initObject() {
        if(kv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
            val intent = Intent(requireContext(), FacialRecognitionActivity::class.java)
            requireContext().startActivity(intent)
        }else{
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
        }
    }

    private fun initEvent(){
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
    }

    override fun onOtherListener(event: Int, any: Any?) {
        handler.postDelayed({
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
        }, 300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        FaceScanVM.instance.setFaceListener(null)
    }
}