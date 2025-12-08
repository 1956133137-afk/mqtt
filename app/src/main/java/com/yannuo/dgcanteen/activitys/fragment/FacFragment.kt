package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.viewModel.FaceScanVM
import com.yannuo.dgcanteen.activitys.viewModel.FaceVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentFacBinding
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.Locale


class FacFragment : BaseFragment<FragmentFacBinding>(),FaceVM.OnListener {
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private val faceVM: FaceVM by lazy { FaceVM() }
    private val mmkv = MMKV.defaultMMKV()

    private var clickStartTime = 0L
    private var clickTimes = 0
    private var payMoney = 0.0f

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFacBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
    }

    private fun initObject() {
        EventBus.getDefault().register(this)
        arguments?.getParcelable<OrderPayInfo>(Constant.PAY_DATE)?.also {
            binding.payTotalMoney.text = "￥${it.payment}"
            payMoney = it.payment
        }

        if(mmkv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
            //跳转本地脸库刷脸
            val intent = Intent(requireContext(), FacialRecognitionActivity::class.java)
            requireContext().startActivity(intent)
            faceVM.setListener(this)
        }else{
            FaceScanVM.instance.bindService()
            FaceScanVM.instance.startFacePay(false, String.format("%.02f", payMoney))
            FaceScanVM.instance.setFaceListener(object : FaceScanVM.FaceResultListener {
                override fun onFacePay(payForUI: PayForUI) {
                    onFacePayResult(payForUI)
                }

                override fun onFaceQuery(bean: CcbFacePayResultBean) {

                }
            })
        }
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
//            CommonAndDpToPxUtil.speakWork("取消支付")
            requireActivity().finish()
        }
        binding.backAutoPay.setOnClickListener {
            if (clickTimes == 0) clickStartTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - clickStartTime < 1000) clickTimes++ else clickTimes = 0
            if (clickTimes == 3) {
                clickTimes = 0
//                binding.btnClose.visibility = View.VISIBLE
                mmkv.encode(Constant.AUTO_PAY, false)
                requireActivity().finish()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventCalculate(event: MessageEvent) {
        handler.post {
            when (event.code) {
                Constant.EVENT_QUOTA_CHANGE -> {
                    FaceScanVM.instance.stopScanFace()
                }
                Constant.EVENT_LOCAL_FACE -> {
                    Log.d(TAG, "eventCalculate: 接收到本地脸库人脸识别结果")
                    val eventFaceBean = event.any as EventFaceBean
                    when(eventFaceBean.code){
                        -1 -> {
                            payError(eventFaceBean.msg)
                        }
                        else -> {
                            faceVM.localFacePay(eventFaceBean.msg,payMoney.toString(),eventFaceBean.img,eventFaceBean.searchScore.toString())
                        }
                    }
                }
            }
        }
    }

    /**
     * 支付失败
     */
    private fun payError(msg: String){
        handler.postDelayed (
            {
                val bean = SimpleForUI().apply {
                    way = "1"
                    timestamp = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                    errorMsg = msg
                }
                CommonAndDpToPxUtil.speakWork("支付失败")
                val action = FacFragmentDirections.actionScanToFail(bean)
                findNavController().navigate(action)
            },300)
    }

    /**
     * 支付成功
     */
    private fun onFacePayResult(payForUI: PayForUI) {
        LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~：$payForUI")
        handler.postDelayed({
            val bean = SimpleForUI().apply {
                way = payForUI.payType
                timestamp = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
            }
            LogUtil.d(TAG, "onFacePayResult: ${Gson().toJson(bean)}")

            if (payForUI.result == "Y") {
                bean.apply {
                    payment = payForUI.actualPayment.ifEmpty { payForUI.payment }
                    custName = payForUI.username
                    accNo = payForUI.accNo
                    acc_bal = payForUI.accBal
                    orderId = payForUI.orderId
                    accList = payForUI.accList
                }
                CommonAndDpToPxUtil.speakWork("支付成功")
                val action = FacFragmentDirections.actionScanToSuccess(bean)
                findNavController().navigate(action)
            } else {
                bean.errorMsg = payForUI.errMsg
                CommonAndDpToPxUtil.speakWork("支付失败")
                val action = FacFragmentDirections.actionScanToFail(bean)
                findNavController().navigate(action)
            }
        }, 300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        FaceScanVM.instance.setFaceListener(null)
    }

    override fun uploadResult(code: Int, type: Boolean, msg: String) {

    }

    override fun facePayResult(payForUI: PayForUI) {
        onFacePayResult(payForUI)
    }
}