package com.yannuo.dgcanteen.activitys.fragment.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.adapters.SelectDateAdapter
import com.yannuo.dgcanteen.adapters.SelectMealAdapter
import com.yannuo.dgcanteen.databinding.FragmentUserOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.model.OrderMeal
import com.yannuo.dgcanteen.model.SelectDateBean
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil

class UserOrderFragment : BaseFragment<FragmentUserOrderBinding>() {
    private val downloadVM by lazy { ViewModelProvider(requireActivity())[DownloadVM::class.java] }
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private val selectDateAdapter by lazy { SelectDateAdapter() }
    private val selectMealAdapter by lazy { SelectMealAdapter() }
    private var dateBean: SelectDateBean = SelectDateBean()
    private val dbHelper = DishesDBHelper.getInstance()
    private var orderForUI: OrderForUI = OrderForUI()

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentUserOrderBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        initObject()
        initData()
        initEvent()
    }

    private fun initObject() {
        // 人员信息
        val directions: UserOrderFragmentArgs by navArgs()
        orderForUI = directions.orderForUI
        // 显示日期
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.dateWeekView.layoutManager = layoutManager
        binding.dateWeekView.adapter = selectDateAdapter
        selectDateAdapter.setDateListener(object : SelectDateAdapter.SelectDateListener {
            override fun onSelectDate(bean: SelectDateBean) {
                LogUtil.d(TAG, Gson().toJson(bean))
            }
        })
        val dateList = downloadVM.getDateWeek()
        if (dateList.size > 0) dateBean = dateList[0]
        selectDateAdapter.data = dateList
        // 显示餐别
        binding.mealView.layoutManager = LinearLayoutManager(requireContext())
        binding.mealView.adapter = selectMealAdapter
        selectMealAdapter.setMealListener(object : SelectMealAdapter.SelectMealListener {
            override fun onSelectMeal(bean: OrderMeal) {
                LogUtil.d(TAG, Gson().toJson(bean))
            }
        })
        downloadVM.synOrderMeal(orderForUI.ccbToken) { handler.post { selectMealAdapter.data = it } }
    }

    private fun initData() {
        if (orderForUI.custName.isEmpty()) {
            val person = dbHelper.queryPersonToCustId(orderForUI.custId)
            orderForUI.custName = if (person != null) person.personName else "未知"
        }
        orderMealVM.getUserName().value = orderForUI.custName
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener {
            orderMealVM.getUserName().value = ""
            if (findNavController().previousBackStackEntry != null) findNavController().popBackStack()
        }
        binding.btnConfirm.setOnClickListener {
//            downloadVM.synOrderMeal(orderForUI.ccbToken)
            findNavController().navigate(R.id.userOrder_to_orderSettle)
        }
    }

}