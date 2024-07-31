package com.yannuo.dgcanteen.activitys.fragment

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.DisplayCardVerificationBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.FaceResult
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.VerificationUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.Utils
import org.greenrobot.eventbus.EventBus

class CardVerificationFragment : BaseFragment<DisplayCardVerificationBinding>(), CallbackListener {
    private lateinit var verificationVM: VerificationVM
    private val kv by lazy {
        MMKV.defaultMMKV()
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
        binding = DisplayCardVerificationBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        initOpear()
        initEvent()
    }

    private fun initOpear() {
        if (!this::verificationVM.isInitialized) verificationVM = VerificationVM()
        verificationVM.openQrCode()
        verificationVM.setListener(this)
        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        requireContext().bindService(lIntent, mServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
        binding.toFace.setOnClickListener {
            faceVerification()
        }
    }

    /**
     * 通过人脸查询人员信息
     */
    private fun faceVerification() {
        LogUtil.d(TAG, "查询人脸信息~")
        var offline = 0  //在线
        if (kv.decodeBool(Constant.SWITCH)) offline = 1  //离线
        val mPayCfg = verificationVM.getPayCfg()
        val campusId = if (mPayCfg == null) "" else mPayCfg.campusId
        val businessId = if (mPayCfg == null) "" else mPayCfg.businessId
        val sn = Utils.getSN()
        mFacePayService?.startFacePay(
            null,
            offline.toString(),
            object : PayResultListener.Stub() {
                override fun onResult(result: String?) {
                    LogUtil.i(TAG, result)
                    val res = Gson().fromJson(result, FaceResult::class.java)
                    if (res.RESULT == "Y") {
                        verificationVM.verification(campusId, businessId, res.CUST_ID, null, sn, null)
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationVM.closeQrCode()
    }

    override fun onOtherListener(event: Int, any: Any?) {
        handler.post {
            when (event) {
                0 -> {
                    LogUtil.d(TAG, "核销成功")

                    val verificationUI = any as VerificationUI
                    val toSuccess = CardVerificationFragmentDirections.actionCardVerificationFragmentToShowDishFragment(verificationUI)
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_ORDER_VERIFY, null))
                    findNavController().navigate(toSuccess)

                }

                10 -> {
                    LogUtil.d(TAG, "核销失败")
                    val verificationUI = any as VerificationUI
                    val toFail = CardVerificationFragmentDirections.actionCardVerificationFragmentToFailedFragment(verificationUI)
                    findNavController().navigate(toFail)
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_ORDER_VERIFY, null))
                }
            }
        }
    }

}