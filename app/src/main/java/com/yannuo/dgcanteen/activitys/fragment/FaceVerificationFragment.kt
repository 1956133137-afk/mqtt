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
import com.yannuo.dgcanteen.databinding.FragmentFaceVerificationBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.model.FaceResult
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.VerificationUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.Utils
import org.greenrobot.eventbus.EventBus

class FaceVerificationFragment : BaseFragment<FragmentFaceVerificationBinding>(), CallbackListener {
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    private val viewModel by lazy {
        VerificationVM()
    }
    private val handler = Handler()
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
        binding = FragmentFaceVerificationBinding.inflate(inflater, container, false)
        initObject()
    }

    private fun initObject() {
        viewModel.setListener(this)
        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        requireActivity().bindService(lIntent, mServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)
        handler.post {
            faceVerification()
        }
    }

    //刷脸核销
    private fun faceVerification() {
        LogUtil.d(TAG,"查询人脸信息~")
        var offline = 0  //在线
        if (kv.decodeBool(Constant.SWITCH)) offline = 1  //离线
        val mPayCfg = viewModel.getPayCfg()
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
                        viewModel.verification(campusId, businessId, res.CUST_ID, null, sn, null)
                    }
                }
            }
        )
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
                    val toFail = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToFailedFragment(verificationUI)
                    findNavController().navigate(toFail)
                }
            }
        }
    }
}