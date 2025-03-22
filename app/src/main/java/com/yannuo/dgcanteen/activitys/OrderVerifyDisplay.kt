package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderVerifyVM
import com.yannuo.dgcanteen.adapters.OrderVerifyAdapter
import com.yannuo.dgcanteen.adapters.VerifyQueryAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.OrderVerifyDisplayBinding
import com.yannuo.dgcanteen.model.OrderVerify
import com.yannuo.dgcanteen.model.OrderVerifyBean
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.views.AwaitingDialog
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

    private var awaitingDialog: AwaitingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = OrderVerifyDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initObject()
    }

    private fun initObject() {
        binding.orderQuery.layoutManager = LinearLayoutManager(getContext())
        binding.orderQuery.adapter = verifyQueryAdapter

        binding.orderVerify.layoutManager = LinearLayoutManager(getContext())
        binding.orderVerify.adapter = orderVerifyAdapter

        orderVerifyVM.setVerifyListener(this)

        binding.btnBack.setOnClickListener { changeView(0) }
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
                        }
                    } else { /*失败*/
                        changeView(2)
                        binding.failureMsg.text = "失败原因：$errMsg"
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

        binding.orderQueryView.visibility = View.GONE
        binding.orderVerifyView.visibility = View.GONE
        /*选择订餐核销或者订餐查询*/
        val verifyQueryMode = mmkv.decodeBool(Constant.ORDER_QUERY, false)
        /*对应界面显示*/
        when (viewId) {
            0 -> {
                binding.initView.visibility = View.VISIBLE
                binding.btnBack.visibility = View.GONE
                binding.initImg.setImageResource(if (!verifyQueryMode) R.drawable.ic_order_verify else R.drawable.ic_order_query)
                countDown?.cancel()
                /*是否需要确认*/
                if (!mmkv.decodeBool(Constant.ORDER_VERIFY_CONFIRM, false)) orderVerifyVM.mOrderVerify.postValue(OrderVerify())
            }
            1 -> {
                binding.successView.visibility = View.VISIBLE
                binding.successTips.text = if (!verifyQueryMode) "核销成功" else "查询成功"
            }
            2 -> {
                binding.failureView.visibility = View.VISIBLE
                binding.failureTips.text = if (!verifyQueryMode) "核销失败" else "查询失败"
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
        }
        countDown?.start()
    }

    override fun cancel() {
        super.cancel()
        countDown?.cancel()
        awaitingDialog?.cancel()
    }
}