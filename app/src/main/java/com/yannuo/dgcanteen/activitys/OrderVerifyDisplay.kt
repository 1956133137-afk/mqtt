package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderVerifyVM
import com.yannuo.dgcanteen.adapters.InfoAdapter
import com.yannuo.dgcanteen.adapters.OrderVerifyAdapter
import com.yannuo.dgcanteen.adapters.VerifyQueryAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.OrderVerifyDisplayBinding
import com.yannuo.dgcanteen.dialogView.ShowTextDailog
import com.yannuo.dgcanteen.model.InfoBean
import com.yannuo.dgcanteen.model.OrderVerify
import com.yannuo.dgcanteen.model.OrderVerifyBean
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.AwaitingDialog
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

/**
 * Author: filowl
 * Description: ***
 * Date: 2025/3/20 11:35
 **/
class OrderVerifyDisplay(context: Context, display: Display) : BaseDisplay(context, display), OrderVerifyVM.VerifyCallBack {
    private val TAG = javaClass.simpleName
    private lateinit var binding: OrderVerifyDisplayBinding
    private val orderVerifyVM by lazy { ViewModelProvider(context as ViewModelStoreOwner)[OrderVerifyVM::class.java] }
    private val mmkv = MMKV.defaultMMKV()
    private val handler = Handler(MyApplication.applicationContext.mainLooper)
    private var countDown: CountDownTimer? = null
    private val verifyQueryAdapter by lazy { VerifyQueryAdapter(getContext()) }
    private val orderVerifyAdapter by lazy { OrderVerifyAdapter(getContext()) }

    private val infoAdapter by lazy { InfoAdapter() }

    private var awaitingDialog: AwaitingDialog? = null
    private var showTextDailog: ShowTextDailog? = null
    private var cardType = false
    private var scanType = false

    override fun onCreate(savedInstanceState: Bundle?) {
//        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = OrderVerifyDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initObject()
        initEvent()
    }

    private fun initObject() {
        binding.orderQuery.layoutManager = LinearLayoutManager(getContext())
        binding.orderQuery.adapter = verifyQueryAdapter

        binding.orderVerify.layoutManager = LinearLayoutManager(getContext())
        binding.orderVerify.adapter = orderVerifyAdapter

        binding.payRecyclerView.layoutManager = LinearLayoutManager(getContext())
        binding.payRecyclerView.adapter = infoAdapter

        orderVerifyVM.setVerifyListener(this)

        binding.btnBack.setOnClickListener { changeView(0) }
    }

    private fun initEvent(){
        binding.codeVerify.setOnClickListener {
            if(orderVerifyVM.getPayState()) return@setOnClickListener
            if(!scanType){
                orderVerifyVM.openScan()
                scanType = true
            }
            showText("请出示二维码")
        }
        binding.cardVerify.setOnClickListener {
            if(orderVerifyVM.getPayState()) return@setOnClickListener
            if(!cardType){
                orderVerifyVM.openIcCard()
                cardType = true
            }
            showText("请出示实体卡")
        }
        binding.faceVerify.setOnClickListener {
            if(!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)) {
                ToastShowUtil.show("无网络，无法使用人脸")
                return@setOnClickListener
            }
            if(orderVerifyVM.getPayState()) return@setOnClickListener
            orderVerifyVM.faceVerification()
            hide()
        }
    }

    private fun showText(text: String){
        Log.d(TAG, "showText: 显示弹窗")
        if (showTextDailog != null && showTextDailog?.isShowing == true) showTextDailog?.dismiss()
        if (showTextDailog == null) showTextDailog = ShowTextDailog(context)
        showTextDailog?.show()
        showTextDailog?.showText(text)
    }

    fun reset(){
        if (showTextDailog != null && showTextDailog?.isShowing == true) showTextDailog?.dismiss()
        if(!mmkv.decodeBool(Constant.ORDER_VERIFY_IC_CARD,false) && cardType) orderVerifyVM.closeIcCard()
        if(scanType) orderVerifyVM.closeScan()
        cardType = false
        scanType = false
    }

    override fun onVerifyResult(type: Int, orderVerifyBean: OrderVerifyBean, errMsg: String) {
        handler.post {
            if (awaitingDialog != null && awaitingDialog?.isShowing == true) awaitingDialog?.dismiss()
            when (type) {
                -1 -> {
                    if (awaitingDialog == null) awaitingDialog = AwaitingDialog(context)
                    awaitingDialog?.show()
                    awaitingDialog?.updateText("加载中")
                }
                else -> {
                    reset()
                    binding.btnBack.visibility = View.VISIBLE
                    onCountDownTimer()
                    if (type == 0) { /*成功*/
                        changeView(1)
                        binding.successName.text = orderVerifyBean.personName
                        if (mmkv.decodeBool(Constant.ORDER_QUERY, false)) {
                            binding.orderQueryView.visibility = View.VISIBLE
                            verifyQueryAdapter.data = orderVerifyVM.getOrderQuery(orderVerifyBean.verify)
                            if (verifyQueryAdapter.data.size < 1) {
                                binding.orderQueryMsg.visibility = View.VISIBLE
                                binding.orderQueryMsg.text = "未查询到用户订餐信息"
                            } else binding.orderQueryMsg.visibility = View.GONE
                        } else {
                            binding.orderVerifyView.visibility = View.VISIBLE
                            orderVerifyAdapter.data = orderVerifyVM.getOrderVerify(orderVerifyBean)
                            var bigDecimal = BigDecimal(0)
                            orderVerifyBean.verifySuccessDishes.forEach {
                                bigDecimal = bigDecimal.add(BigDecimal(it.price).multiply(BigDecimal(it.dishesNum)))
                            }
                            CommonAndDpToPxUtil.speakWork("核销成功${bigDecimal}元")
                            binding.allMoney.text = "总消费金额\n$bigDecimal"
                            //打印
                            printVerify(orderVerifyBean)
                        }
                    } else { /*失败*/
                        changeView(2)
                        binding.failureMsg.text = "失败原因：$errMsg"
                        binding.failureTime.text = "失败时间：${TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())}"
                    }
                    show()
                }
            }
        }
    }

    private fun printVerify(bean: OrderVerifyBean){
        USBPrinterHelper.instance.printTicket("4", bean)
    }

    override fun onPayResult(type: Int, payForUI: PayForUI) {
        reset()
        handler.post {
            if (awaitingDialog != null && awaitingDialog?.isShowing == true) awaitingDialog?.dismiss()
            when (type) {
                -1 -> {
                    if (awaitingDialog == null) awaitingDialog = AwaitingDialog(context)
                    awaitingDialog?.show()
                    awaitingDialog?.updateText("加载中")
                }
                else -> {
                    binding.btnBack.visibility = View.VISIBLE
                    onCountDownTimer()
                    if (payForUI.result == "Y") {
                        changeView(1)
                        binding.successName.text = payForUI.username
                        binding.orderPayView.visibility = View.VISIBLE
                        val infoList = mutableListOf<InfoBean>()
                        infoList.add(InfoBean("账户余额: ", payForUI.accBal))
                        infoList.add(InfoBean("支付时间: ", payForUI.payTime))
                        infoList.add(InfoBean("订单金额: ", payForUI.payment))
                        payForUI.accList.forEach {
                            val payType = when(it.ACC_TYPE){
                                "01" ->"现金账户"
                                "02" ->"账户1"
                                "03" ->"账户2"
                                "04" ->"账户3"
                                "05" ->"账户4"
                                "06" ->"账户5"
                                else -> "其他账户"
                            }
                            infoList.add(InfoBean(payType, it.PAYMENT))
                        }
                        infoList.add(InfoBean("实付金额: ", payForUI.actualPayment))
                        infoList.add(InfoBean("订单编号: ", payForUI.orderId))
                        CommonAndDpToPxUtil.speakWork("支付成功,${DecimalFormat("#0.##").format(payForUI.actualPayment.toDouble())}元}")
                        if(payForUI.offline == "1") CommonAndDpToPxUtil.speakWork("当前为离线订单后续补扣")
                        infoAdapter.data = infoList
                    } else {
                        changeView(2)
                        binding.failureMsg.text = "失败原因：${payForUI.errMsg}"
                        binding.failureTime.text = "失败时间：${TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())}"
                    }
                }
            }
        }
    }

    fun changeView(viewId: Int) {
        /*默认全部不显示*/
        binding.initView.visibility = View.GONE
        binding.successView.visibility = View.GONE
        binding.failureView.visibility = View.GONE

        binding.orderPayView.visibility = View.GONE
        binding.orderQueryView.visibility = View.GONE
        binding.orderVerifyView.visibility = View.GONE
        /*选择订餐核销或者订餐查询*/
        val verifyQueryMode = mmkv.decodeBool(Constant.ORDER_QUERY, false)
        /*对应界面显示*/
        when (viewId) {
            0 -> {
                binding.initView.visibility = View.VISIBLE
                binding.btnBack.visibility = View.GONE
                binding.tvOrderPay.visibility = View.GONE
                if (orderVerifyVM.payMode) {
                    binding.tvOrderPay.visibility = View.VISIBLE
                    binding.initImg.setImageResource(R.drawable.ic_order_pay)
                    binding.tvOrderPay.text = if (orderVerifyVM.payAmount.isNotEmpty()) "¥${orderVerifyVM.payAmount}" else ""
                } else binding.initImg.setImageResource(if (!verifyQueryMode) R.drawable.ic_order_verify else R.drawable.ic_order_query)
                countDown?.cancel()
                /*是否需要确认*/
                if (!mmkv.decodeBool(Constant.ORDER_VERIFY_CONFIRM, false)) orderVerifyVM.mOrderVerify.postValue(OrderVerify())
                binding.orderVerifyType.visibility = if(mmkv.decodeBool(Constant.ORDER_VERIFY_IC_CARD,false)) View.GONE else View.VISIBLE
                if(mmkv.decodeBool(Constant.ORDER_VERIFY_FACE,false)){
                    binding.codeVerify.visibility = View.GONE
                    binding.cardVerify.visibility = View.GONE
                }else{
                    binding.codeVerify.visibility = View.VISIBLE
                    binding.cardVerify.visibility = View.VISIBLE
                }
            }
            1 -> {
                binding.successView.visibility = View.VISIBLE
                if (orderVerifyVM.payMode) {
                    binding.successTips.text = "支付成功"
                    binding.allMoney.text = ""
                }
                else binding.successTips.text = if (!verifyQueryMode) "订餐核销" else "订餐查询"
            }
            2 -> {
                binding.failureView.visibility = View.VISIBLE
                if (orderVerifyVM.payMode) {
                    binding.failureTips.text = "支付失败"
                    binding.allMoney.text = ""
                }
                else binding.failureTips.text = if (!verifyQueryMode) "核销失败" else "查询失败"
            }
        }
    }

    private fun onCountDownTimer() {
        countDown?.cancel()
        val backTime = mmkv.decodeInt(Constant.MEAL_TIME, 10).toLong()
        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(backTime), 1000) {
            override fun onTick(mil: Long) {
                binding.btnBack.text = "返回 ( ${TimeUnit.MILLISECONDS.toSeconds(mil)} )"
            }

            override fun onFinish() {
                changeView(0)
            }
        }.start()
    }

    override fun cancel() {
        super.cancel()
        countDown?.cancel()
        awaitingDialog?.cancel()
    }
}