package com.yannuo.dgcanteen.activitys.fragment.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.adapters.OrderDishAdapter
import com.yannuo.dgcanteen.adapters.SelectDateAdapter
import com.yannuo.dgcanteen.adapters.SelectDishAdapter
import com.yannuo.dgcanteen.adapters.SelectMealAdapter
import com.yannuo.dgcanteen.databinding.FragmentUserOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.model.OrderMeal
import com.yannuo.dgcanteen.model.SelectDateBean
import com.yannuo.dgcanteen.util.LogUtil

class UserOrderFragment : BaseFragment<FragmentUserOrderBinding>() {
    private val downloadVM by lazy { ViewModelProvider(requireActivity())[DownloadVM::class.java] }
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    private val selectDateAdapter by lazy { SelectDateAdapter() }
    private val selectMealAdapter by lazy { SelectMealAdapter() }
    private val orderDishAdapter by lazy { OrderDishAdapter() }
    private val selectDishAdapter by lazy { SelectDishAdapter() }
    private var dateBean: SelectDateBean = SelectDateBean()
    private val dbHelper = DishesDBHelper.getInstance()
    private var orderForUI: OrderForUI = OrderForUI()

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentUserOrderBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        initData()
        initObject()
        initEvent()
    }

    private fun initData() {
        val directions: UserOrderFragmentArgs by navArgs()
        orderForUI = directions.orderForUI
        if (orderForUI.custName.isEmpty()) {
            val person = dbHelper.queryPersonToCustId(orderForUI.custId)
            orderForUI.custName = if (person != null) person.personName else "未知"
        }
        orderMealVM.getUserName().value = orderForUI.custName
    }

    private fun initObject() {
        // 菜品
        binding.dishView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.dishView.adapter = orderDishAdapter
        // 显示日期
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.dateWeekView.layoutManager = layoutManager
        binding.dateWeekView.adapter = selectDateAdapter
        // 显示餐别
        binding.mealView.layoutManager = LinearLayoutManager(requireContext())
        binding.mealView.adapter = selectMealAdapter
        // 购物车
        binding.productView.layoutManager = LinearLayoutManager(requireContext())
        binding.productView.adapter = selectDishAdapter
        // 请求餐别信息
        downloadVM.synOrderMeal(orderForUI.ccbToken) {
            handler.post {
                if (it) {
                    val dateList = downloadVM.getDateWeek()
                    dateBean = dateList[0]
                    selectDateAdapter.data = dateList
                    selectMealAdapter.data = dateBean.mealList
                    showDish(dateBean.date, if (dateBean.mealList.size > 0) dateBean.mealList[0] else null)
                }
            }
        }

    }

    private fun initEvent() {
        //日期回调
        selectDateAdapter.setDateListener(object : SelectDateAdapter.SelectDateListener {
            override fun onSelectDate(bean: SelectDateBean) {
                handler.post {
                    dateBean = bean
                    selectMealAdapter.selectPos = 0
                    selectMealAdapter.data = dateBean.mealList
                    showDish(dateBean.date, if (dateBean.mealList.size > 0) dateBean.mealList[0] else null)
                }
            }
        })
        //餐别回调
        selectMealAdapter.setMealListener(object : SelectMealAdapter.SelectMealListener {
            override fun onSelectMeal(bean: OrderMeal) {
                handler.post { showDish(dateBean.date, bean) }
            }
        })
        //菜品回调
        orderDishAdapter.setDishListener(object : OrderDishAdapter.OrderDishListener {
            override fun onOrderDish(bean: DishBean) {
                handler.post {
                    selectDishAdapter.insertedData(bean)
                    updateTotalDish()
                }
            }
        })
        //已选回调
        selectDishAdapter.setDishListener(object : SelectDishAdapter.SelectDishListener {
            override fun onSelectDish(dishId: String) {
                handler.post {
                    orderDishAdapter.updateDishCount(dishId)
                    updateTotalDish()
                }
            }
        })
        binding.igBtnClear.setOnClickListener {
            selectDishAdapter.data.forEach {
                it.dishCount = 0
                orderDishAdapter.updateDishCount(it.dishId)
            }
            selectDishAdapter.clear()
            updateTotalDish()
        }
        //取消订餐
        binding.btnBack.setOnClickListener {
            orderMealVM.getUserName().value = ""
            if (findNavController().previousBackStackEntry != null) findNavController().popBackStack()
        }
        //确定订餐
        binding.btnConfirm.setOnClickListener {
            findNavController().navigate(R.id.userOrder_to_orderSettle)
        }
    }

    private fun showDish(date: String, orderMeal: OrderMeal?) {
        if (orderMeal == null) {
            orderDishAdapter.clear()
            return
        }
        LogUtil.d(TAG, "date: $date,mealName: ${orderMeal.mealName}")
        downloadVM.synOrderDish(orderForUI.ccbToken, date, orderMeal.mealId) { boolean, dishList ->
            handler.post {
                if (boolean) orderDishAdapter.data = dishList
            }
        }
    }

    private fun updateTotalDish() {
        var count: Int = 0
        var totalMoney: Double = 0.0
        selectDishAdapter.data.forEach {
            count += it.dishCount
            totalMoney += it.dishPrice.toDouble() * it.dishCount
        }
        binding.tvTotalCount.text = "$count"
        binding.tvTotalMoney.text = String.format("%.02f元", totalMoney)
    }
}