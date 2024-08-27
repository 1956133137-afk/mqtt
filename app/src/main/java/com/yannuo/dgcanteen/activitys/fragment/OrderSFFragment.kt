package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.PayResultAdapter
import com.yannuo.dgcanteen.databinding.FragmentOrderSFBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.printer.PrinterOperator
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil

open class OrderSFFragment() : BaseFragment<FragmentOrderSFBinding>() {

    private lateinit var model: ProductsVM
    private var time = 0
    private var mPayResultAdapter: PayResultAdapter? = null
    private var countDownTimer: CountDownTimer? = null
    private val kv = MMKV.defaultMMKV()

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOrderSFBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        initObject()
        initView()
        initEvent()
        loadData()
    }

    private fun loadData() {

    }

    private fun initObject() {
        model = ViewModelProvider(requireActivity()).get(ProductsVM::class.java)
        binding.subS.rvDishList.layoutManager = LinearLayoutManager(context)
        mPayResultAdapter = PayResultAdapter(requireContext())
        binding.subS.rvDishList.adapter = mPayResultAdapter
    }

    private fun initView() {
        LogUtil.i(TAG, "initView")
//        val gridLayoutManager = GridLayoutManager(context,6,)
//        binding.rvManInfo.layoutManager = gridLayoutManager
//        binding.rvManInfo.adapter = adapter
        model.uiData.observe(this) {
            if (it.result != "Y") updateFChange(it) else updateSChange(it)
//            LogUtil.i(TAG,"initView")
        }
    }

    private fun release() {
//        adapter.setListener(null)
//        model.loadDialog.value = false
    }

    private fun initEvent() {
//        if (time < 0) {
        binding.subS.btBack.setOnClickListener { v ->
            back()
        }
        binding.subF.btBack.setOnClickListener { v ->
            back()
        }
//        }
    }

    private fun back() {
        model.tab.postValue(0)
        countDownTimer?.cancel()
        model.getDisplay()?.also {
            if (it.isShowing.not()) {
                it.show()
            }
        }
    }

    private fun updateFChange(payForUI: PayForUI) {
        binding.subF.btBack.isEnabled = true
        binding.subF.root.visibility = View.VISIBLE

        binding.subF.payFailMsg.text = payForUI.errMsg
        binding.subF.payTime.text = payForUI.payTime

        CommonAndDpToPxUtil.speakWork("支付失败")
        startTime(payForUI)
    }

    private fun updateSChange(payForUI: PayForUI) {
        binding.subS.btBack.isEnabled = true
        binding.subF.root.visibility = View.GONE;
        if (binding.subS.root.visibility == View.GONE) {
            binding.subS.root.visibility = View.VISIBLE
        }

        val str = when (payForUI.payType) {
            "1" -> "刷脸支付"
            "2" -> "扫码支付"
            else -> "刷卡支付"
        }
        CommonAndDpToPxUtil.speakWork("${str} ${payForUI.payment} 元")
        mPayResultAdapter!!.data = payForUI.paymentDishes
        binding.subS.tvSum.text = "${payForUI.paymentDishes.size} 件"
        binding.subS.payTotalMoney.text = "￥ ${payForUI.payment} 元"
        binding.subS.tvPayTime.text = payForUI.payTime
        binding.subS.tvTransNumber.text = payForUI.orderId.ifEmpty { payForUI.traceId }

        val persons = DishesDBHelper.getInstance().queryPersonToCustId(payForUI.custId)
        if (persons != null && persons.grade != null) binding.subS.tvClass.text = "${persons.grade}(${persons.userClass})"
        binding.subS.tvName.text = payForUI.username
        binding.subS.tvBalance.text = payForUI.accBal

        PrinterOperator.printerFoodsList(payForUI)
        if (payForUI.result == "Y") USBPrinterHelper.instance.printTicket(payForUI)
        startTime(payForUI)
    }

    fun startTime(payForUI: PayForUI) {
        var totalTime = kv.decodeInt(Constant.SHOW_TIME, 5)
        if (payForUI.payType == "1") {
            if (payForUI.result != "Y") binding.subF.btBack.isEnabled = false
            else binding.subS.btBack.isEnabled = false
            if (totalTime < 3) totalTime = 3
        }
        dida(totalTime, payForUI)
    }

    private fun dida(tm: Int, payForUI: PayForUI) {
        if (tm == 0) return
        countDownTimer = object : CountDownTimer((tm * 1000 + 100).toLong(), 1000) {
            override fun onTick(mil: Long) {
                LogUtil.i("onTick", "返回时间${mil / 1000}")
                if (payForUI.result != "Y") binding.subF.btBack.text = "返回${mil / 1000}秒"
                else binding.subS.btBack.text = "返回${mil / 1000}秒"

                if (mil / 1000 == (tm - 3).toLong()) {
                    if (payForUI.payType == "1") {
                        if (payForUI.result != "Y") binding.subF.btBack.isEnabled = true
                        else binding.subS.btBack.isEnabled = true
                    }
                }
            }

            override fun onFinish() {
                if (time < 0) {
                    if (payForUI.result != "Y") {
                        binding.subF.btBack.isEnabled = true
                        binding.subF.btBack.text = "返回"
                    } else {
                        binding.subS.btBack.isEnabled = true
                        binding.subS.btBack.text = "返回"
                    }
                } else {
                    back()
                }
            }
        }
        countDownTimer?.start()
    }

    override fun onPause() {
        super.onPause()
        back()
    }

    override fun onStop() {
        super.onStop()
        countDownTimer?.cancel()
    }
}