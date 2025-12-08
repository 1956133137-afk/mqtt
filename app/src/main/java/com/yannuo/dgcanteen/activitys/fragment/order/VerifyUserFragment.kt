package com.yannuo.dgcanteen.activitys.fragment.order

import android.content.Intent
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.OrderRecordActivity
import com.yannuo.dgcanteen.activitys.viewModel.FaceVM
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentVerifyUserBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.CcbFacePayResultBean
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.views.HintDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class VerifyUserFragment : BaseFragment<FragmentVerifyUserBinding>() {
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private var hintDialog: HintDialog? = null
    private var countDown: CountDownTimer? = null
    private var orderType: String = "0"
    private val mmkv = MMKV.defaultMMKV()

    private val faceVM by lazy { FaceVM() }

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentVerifyUserBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        EventBus.getDefault().register(this)
//        initObject()
        initEvent()
    }

    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun eventArrive(event: MessageEvent) {
        when (event.code) {
            Constant.EVENT_LOCAL_FACE -> {
                Log.d(TAG, "eventCalculate: 接收到本地脸库人脸识别结果")
                val eventFaceBean = event.any as EventFaceBean
                when(eventFaceBean.code){
                    -1 -> {
                        handler.post {
                            onCountDownTimer(10L)
                            binding.errCode.text = eventFaceBean.code.toString()
                            binding.errMsg.text = eventFaceBean.msg
                            binding.err.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        val person = DishesDBHelper.getInstance().queryFaceByEigenvalue(eventFaceBean.msg)
//                        val person = DishesDBHelper.getInstance().queryPersonToCustId(eventFaceBean.msg)
                        if(person == null){
                            handler.post {
                                onCountDownTimer(10L)
                                binding.errCode.text = "0x000001"
                                binding.errMsg.text = "未查询到人员信息"
                                binding.err.visibility = View.VISIBLE
                            }
                        }else{
                            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
                            if(payCfg == null){
                                handler.post {
                                    onCountDownTimer(10L)
                                    binding.errCode.text = "0x000001"
                                    binding.errMsg.text = "未获取到配置信息"
                                    binding.err.visibility = View.VISIBLE
                                }
                                return
                            }
                            val ccbBean = CcbFacePayResultBean().apply {
                                RESULT = "Y"
                                CUST_ID = person.custId
//                                CUST_NAME = person.personName
                                BUSINESS_NAME = payCfg.businessName
                            }
                            orderMealVM.loginHandler("1",Gson().toJson(ccbBean))
                        }
                    }
                }
            }
        }
    }

    private fun initObject() {
        binding.btnFaceScan.visibility = if (mmkv.decodeBool(Constant.ORDER_LOGIN_FACE, true)) View.VISIBLE else View.GONE
        binding.btnQrCode.visibility = if (mmkv.decodeBool(Constant.ORDER_LOGIN_CODE, true)) View.VISIBLE else View.GONE
        binding.btnIcCard.visibility = if (mmkv.decodeBool(Constant.ORDER_LOGIN_CARD, true)) View.VISIBLE else View.GONE
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
                                    if (kv.decodeBool(Constant.ORDER_QUERY, false)) {
                                        val intent = Intent(requireActivity(), OrderRecordActivity::class.java)
                                        intent.putExtra("custId", orderMealVM.getUserId())
                                        intent.putExtra("ccbToken", orderMealVM.getCcbToken())
                                        startActivity(intent)
                                    } else {
                                        val skipPage = VerifyUserFragmentDirections.verifyUserToUserOrder(orderForUI)
                                        findNavController().navigate(skipPage)
                                    }
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
        if(type == "1" && kv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
            //跳转本地脸库刷脸
            val intent = Intent(requireContext(), FacialRecognitionActivity::class.java)
            startActivity(intent)
        }else{
            orderMealVM.open(orderType, true)
        }
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
        EventBus.getDefault().unregister(this)
    }
}