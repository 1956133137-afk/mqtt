package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.adapters.MealTimePayOrderAdapter
import com.yannuo.dgcanteen.databinding.FragmentMealTimePayOrderBinding
import com.yannuo.dgcanteen.dialogView.ShowAmountDetailDialog
import com.yannuo.dgcanteen.dialogView.ShowTimeDetailDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.MealTable
import com.yannuo.dgcanteen.greendao.entity.SwPayOrderTable
import com.yannuo.dgcanteen.model.AmountDetail
import com.yannuo.dgcanteen.model.TimeDetail
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil

/**
 * @dsc     餐次消费记录
 * @Author  LiWeiZhong
 * @Date    2024/12/16 18:10
 * @Version 1.0
 */
class MealTimePayOrderFragment: BaseFragment<FragmentMealTimePayOrderBinding>() {

    private lateinit var mealTimePayOrderAdapter: MealTimePayOrderAdapter
    private val showTimeDetailDialog by lazy { ShowTimeDetailDialog(requireContext()) }
    private val showAmountDetailDialog by lazy { ShowAmountDetailDialog(requireContext()) }

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentMealTimePayOrderBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        mealTimePayOrderAdapter = MealTimePayOrderAdapter(requireContext())
        binding.recyclerView.adapter = mealTimePayOrderAdapter
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        val payOrderList = DishesDBHelper.getInstance().querySwPayOrder("", "", date)
        mealTimePayOrderAdapter.data = payOrderList
        initData(payOrderList)
        initEvent()
    }

    private fun initData(payOrderList: MutableList<SwPayOrderTable>) {
        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
        var tranCount = 0
        var tranAmount = 0.0
        var tranActualAmount = 0.0
        var uploadCount = 0
        var notCount = 0
        payOrderList.forEach {
            tranCount++
            tranAmount += it.payment.ifEmpty { "0.00" }.toDouble()
            tranActualAmount += it.actualPayment.ifEmpty { "0.00" }.toDouble()
            if (it.flag == 1) uploadCount++ else notCount++
        }
        binding.dateMsg.text = "$date\n消费汇总统计"
        binding.tranCount.text = "$tranCount"
        val str = "${String.format("%.02f", tranAmount)}/${String.format("%.02f", tranActualAmount)}"
        LogUtil.i(TAG, "len: ${str.length}")
        if (str.length <= 18) binding.tranAmount.text = str
        else binding.tranAmount.text = "${String.format("%.02f", tranAmount)}\n/\n${String.format("%.02f", tranActualAmount)}"
    }

    private fun initEvent() {
        binding.queryOrder.setOnClickListener {
            val username = binding.inputUserName.text.toString().trim().replace(" ", "")
            val orderId = binding.inputOrderId.text.toString().trim().replace(" ", "")
            val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
            mealTimePayOrderAdapter.data = DishesDBHelper.getInstance().querySwPayOrder(username, orderId, date)
        }

        binding.mealTimeDetail.setOnClickListener {
            val queryAllMeals = DishesDBHelper.getInstance().queryAllMeals()
            val date= TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
            if (queryAllMeals.size <= 0) return@setOnClickListener
            val map = getTimeDetail(queryAllMeals, date)
            LogUtil.i(TAG, "mealTimeDetail: $map")
            showTimeDetailDialog.show()
            showTimeDetailDialog.setTimeDetail(map)
        }

        binding.mealAmountDetail.setOnClickListener {
            val queryAllMeals = DishesDBHelper.getInstance().queryAllMeals()
            val date= TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
            if (queryAllMeals.size <= 0) return@setOnClickListener
            val map = getAmountDetail(queryAllMeals, date)
            LogUtil.i(TAG, "mealAmountDetail: $map")
            showAmountDetailDialog.show()
            showAmountDetailDialog.setAmountDetail(map)
        }
    }

    private fun getTimeDetail(queryAllMeals: List<MealTable>, date: String): HashMap<String, TimeDetail> {
        val hashMap = hashMapOf<String,TimeDetail>()
        for ((i,v) in queryAllMeals.withIndex()) {
            var allTime = 0
            var mealTime = 0
            val standard = DishesDBHelper.getInstance().querySwPayOrderByStandardMealId(v.mealId.toInt(), date)
            val list1 = mutableListOf<SwPayOrderTable>()
            standard.forEach {
                if (it.standardNum.trim().toInt() > 0) list1.add(it)
                allTime += it.standardNum.trim().toInt()
            }
            val actual = DishesDBHelper.getInstance().querySwPayOrderByActualMealId(v.mealId.toInt(), date)
            val list2 = mutableListOf<SwPayOrderTable>()
            actual.forEach {
                if (it.standardNum.trim().toInt() > 0) list2.add(it)
                mealTime += it.standardNum.trim().toInt()
            }
            val externalTime = allTime - mealTime
            hashMap["meal0${i + 1}"] = TimeDetail(v.mealName, allTime, if (externalTime < 0) 0 else externalTime)
        }
        return hashMap
    }

    private fun getAmountDetail(queryAllMeals: List<MealTable>, date: String): HashMap<String, AmountDetail> {
        val hashMap = hashMapOf<String, AmountDetail>()
        for ((i,v) in queryAllMeals.withIndex()) {
            var payment = 0.0
            var actualPayment = 0.0
            DishesDBHelper.getInstance().querySwPayOrderByActualMealId(v.mealId.toInt(), date).forEach {
                payment += it.payment.toDouble()
                actualPayment += it.actualPayment.toDouble()
            }
            hashMap["meal0${i + 1}"] = AmountDetail(v.mealName, payment, actualPayment)
        }
        return hashMap
    }
}