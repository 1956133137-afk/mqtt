package com.yannuo.dgcanteen.activitys.fragment.order

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.adapters.InfoAdapter
import com.yannuo.dgcanteen.adapters.ListDishAdapter
import com.yannuo.dgcanteen.databinding.FragmentOrderSettleBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.InfoBean
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.printer.USBPrinterHelper
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.text.DecimalFormat

class OrderSettleFragment : BaseFragment<FragmentOrderSettleBinding>() {
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private val listDishAdapter by lazy { ListDishAdapter(requireContext()) }
    private val infoAdapter by lazy { InfoAdapter() }
    private val dbHelper = DishesDBHelper.getInstance()
    private var orderForUI: OrderForUI = OrderForUI()

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOrderSettleBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        val directions: OrderSettleFragmentArgs by navArgs()
        orderForUI = directions.orderForUI
        LogUtil.d(TAG, Gson().toJson(orderForUI))
        showPayResult(orderForUI)

        binding.dishListView.layoutManager = LinearLayoutManager(requireContext())
        binding.dishListView.adapter = listDishAdapter
        listDishAdapter.data = orderForUI.dishList
        totalMoneyCompute()

        val person = dbHelper.queryPersonToCustId(orderForUI.custId)
        if (person != null) {
            if (orderForUI.custName.isEmpty()) orderForUI.custName = person.personName
            binding.inputPhone.setText(person.phone)
            binding.inputAddress.setText("${person.grade}${person.userClass}")
        }

        binding.payResView.layoutManager = LinearLayoutManager(requireContext())
        binding.payResView.adapter = infoAdapter

        initEvent()
    }

    private fun initEvent() {
        if (orderForUI.distribute != "0") binding.radioGroup.check(if (orderForUI.distribute == "1") R.id.btn_one else R.id.btn_two)
        binding.radioGroup.setOnCheckedChangeListener { radioGroup, checkId ->
            when (checkId) {
                R.id.btn_one -> orderForUI.distribute = "1"
                R.id.btn_two -> orderForUI.distribute = "2"
            }
        }

        //确定支付
        binding.btnConfirm.setOnClickListener {
            if (!judgePayStatus()) return@setOnClickListener
            orderMealVM.placeAnOrder(orderForUI) { type ->
                handler.post {
                    when (type) {
                        1 -> orderMealVM.getAwaitStatus().value = "订餐下单中"
                        2 -> orderMealVM.getAwaitStatus().value = "订餐支付中"
                        3 -> {
                            orderMealVM.getAwaitStatus().value = ""
                            if (orderForUI.result == "Y") {
                                USBPrinterHelper.instance.printTicket("1", orderForUI)
                                showPayResult(orderForUI, "支付成功", "#82D582")
                            } else showPayResult(orderForUI, "支付失败", "#FF5252")
                        }
                    }
                }
            }
        }
        //继续订餐
        binding.btnReorder.setOnClickListener {
            orderMealVM.getReorderStatus().value = true
            orderForUI.result = "N"
            orderForUI.orderId = ""
            if (findNavController().previousBackStackEntry != null) findNavController().popBackStack()
        }
        //取消订餐
        binding.btnCancel.setOnClickListener {
            orderMealVM.getUserName().value = ""
            findNavController().navigate(R.id.orderSettle_to_verifyUser)
        }
    }

    private fun totalMoneyCompute() {
        var totalCount: Int = 0
        var totalMoney: Double = 0.0
        orderForUI.dishList.forEach {
            totalCount += it.dishCount
            totalMoney += it.dishPrice.toDouble() * it.dishCount
        }
        orderForUI.payment = String.format("%.02f", totalMoney)
        orderForUI.actualPayment = String.format("%.02f", totalMoney)
        binding.tvTotalCount.text = "$totalCount"
        binding.tvTotalMoney.text = String.format("%.02f元", totalMoney)
    }

    private fun judgePayStatus(): Boolean {
        orderForUI.phone = binding.inputPhone.text.toString()
        if (orderForUI.distribute == "1") orderForUI.address = binding.inputAddress.text.toString()
        orderForUI.remark = binding.inputRemark.text.toString()

        var flag = 0
        if (orderForUI.distribute == "0") flag = 1
        if (flag == 0 && orderForUI.phone.length != 11) flag = 2
        if (flag == 0 && !isValidPhone(orderForUI.phone)) flag = 3
        if (flag == 0 && orderForUI.address.isEmpty() && orderForUI.distribute == "1") flag = 4
        if (flag == 0 && orderForUI.result == "Y") flag = 5
        val tipsStr = when (flag) {
            1 -> "请先选择配送方式"
            2 -> "联系电话位数不足"
            3 -> "联系电话格式不对"
            4 -> "配送地址不可为空"
            5 -> "不可重复支付"
            else -> ""
        }
        if (flag != 0) {
            ToastShowUtil.show(tipsStr)
            CommonAndDpToPxUtil.speakWork(tipsStr)
            return false
        } else return true
    }

    // 绿:#82D582 红:#FF5252
    private fun showPayResult(orderForUI: OrderForUI, resultStr: String = "等待支付", colorStr: String = "#4F4F4F") {
        binding.payResult.text = resultStr
        binding.payResult.setTextColor(Color.parseColor(colorStr))
        val infoList = mutableListOf<InfoBean>()
        when (resultStr) {
            "等待支付" -> infoAdapter.clear()
            "支付成功" -> {
                infoList.add(InfoBean("账户余额: ", orderForUI.accBal))
                infoList.add(InfoBean("支付时间: ", orderForUI.payTime))
                infoList.add(InfoBean("实付金额: ", orderForUI.actualPayment))
                infoList.add(InfoBean("订单编号: ", orderForUI.orderId))
                CommonAndDpToPxUtil.speakWork("${resultStr},${DecimalFormat("#0.##").format(orderForUI.actualPayment.toDouble())}元}")
            }
            else -> {
                infoList.add(InfoBean("错误代码: ", orderForUI.errCode))
                infoList.add(InfoBean("错误信息: ", orderForUI.errMsg))
                CommonAndDpToPxUtil.speakWork(resultStr)
            }
        }
        infoAdapter.data = infoList
    }

    private fun isValidPhone(phone: String): Boolean {
        val regex = Regex("""^1[3-9]\d{9}$""", RegexOption.IGNORE_CASE)
        return regex.matches(phone)
    }
}