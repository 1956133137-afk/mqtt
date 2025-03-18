package com.yannuo.dgcanteen.activitys.fragment.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import com.yannuo.dgcanteen.activitys.viewModel.OrderMealVM
import com.yannuo.dgcanteen.adapters.DateMenuAdapter
import com.yannuo.dgcanteen.adapters.OrderDishAdapter
import com.yannuo.dgcanteen.adapters.SelectDateAdapter
import com.yannuo.dgcanteen.adapters.SelectMealAdapter
import com.yannuo.dgcanteen.databinding.FragmentUserOrderBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.OrderForUI
import com.yannuo.dgcanteen.model.OrderMeal
import com.yannuo.dgcanteen.model.SelectDateBean
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil

class UserOrderFragment : BaseFragment<FragmentUserOrderBinding>() {
    private val downloadVM by lazy { ViewModelProvider(requireActivity())[DownloadVM::class.java] }
    private val orderMealVM by lazy { ViewModelProvider(requireActivity())[OrderMealVM::class.java] }
    //日期
    private val selectDateAdapter by lazy { SelectDateAdapter() }
    //餐别
    private val selectMealAdapter by lazy { SelectMealAdapter() }
    //菜单
    private val orderDishAdapter by lazy { OrderDishAdapter(requireContext()) }
    private val dateMenuAdapter by lazy { DateMenuAdapter(requireContext()) }
    private var currentDateBean: SelectDateBean = SelectDateBean()
    private var currentMealBean: OrderMeal? = null
    private val dbHelper = DishesDBHelper.getInstance()
    private var orderForUI: OrderForUI = OrderForUI()
    private var isFirst: Boolean = true

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
        orderMealVM.getReorderStatus().observe(requireActivity()) { clearSelectDish() }
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
        binding.productView.adapter = dateMenuAdapter
        // 请求餐别信息
        downloadVM.synOrderMeal(orderForUI.ccbToken) { boolean ->
            handler.post {
                if (!boolean) orderMealVM.getAwaitStatus().value = "同步餐别中"
                else {
                    downloadVM.getMenuList().clear()
                    orderMealVM.getAwaitStatus().value = ""
                    val dateList = downloadVM.getDateWeek()
                    selectDateAdapter.data = dateList
                    selectMealAdapter.data = dateList[0].mealList
                    currentDateBean = dateList[0]
                    currentMealBean = if (currentDateBean.mealList.size > 0) currentDateBean.mealList[0] else null
                    showDish(currentDateBean, currentMealBean)
                    isFirst = false
                }
            }
        }

    }

    private fun initEvent() {
        //日期回调
        selectDateAdapter.setDateListener(object : SelectDateAdapter.SelectDateListener {
            override fun onSelectDate(bean: SelectDateBean) {
                handler.post {
                    selectMealAdapter.selectPos = 0
                    selectMealAdapter.data = bean.mealList
                    currentDateBean = bean
                    currentMealBean = if (currentDateBean.mealList.size > 0) currentDateBean.mealList[0] else null
                    showDish(currentDateBean, currentMealBean)
                }
            }
        })
        //餐别回调
        selectMealAdapter.setMealListener(object : SelectMealAdapter.SelectMealListener {
            override fun onSelectMeal(bean: OrderMeal) {
                handler.post {
                    currentMealBean = bean
                    showDish(currentDateBean, currentMealBean)
                }
            }
        })
        //菜品回调
        orderDishAdapter.setDishListener(object : OrderDishAdapter.OrderDishListener {
            override fun onOrderDish(dishBean: DishBean) {
                handler.post {
                    val menuList = downloadVM.selectDateMealDish(currentDateBean, currentMealBean, dishBean)
                    dateMenuAdapter.data = menuList
                    //更新价格
                    updateTotalDish()
                }
            }

            override fun onOrderDishDescription(description: String) {
                handler.post { binding.mvControl.text = description.replace("(<p>|</p>)".toRegex(), "") }
            }
        })
        //已选回调
        dateMenuAdapter.setDateListener(object : DateMenuAdapter.SelectDateListener {
            override fun onSelectDate(date: String, mealId: String, dishBean: DishBean) {
                handler.post {
                    downloadVM.setMenuList(dateMenuAdapter.data)
                    if (currentDateBean.date == date && currentMealBean != null && currentMealBean?.mealId == mealId) {
                        orderDishAdapter.updateDishCount(dishBean)
                    }
                    updateTotalDish()
                }
            }
        })
        binding.igBtnClear.setOnClickListener { clearSelectDish() }
        //取消订餐
        binding.btnBack.setOnClickListener {
            orderMealVM.getUserName().value = ""
            if (findNavController().previousBackStackEntry != null) findNavController().popBackStack()
        }
        //确定订餐
        binding.btnConfirm.setOnClickListener {
            orderForUI.menuList.clear()
            orderForUI.menuList.addAll(dateMenuAdapter.data)
            if (orderForUI.menuList.size > 0) {
                val toOrderSettle = UserOrderFragmentDirections.userOrderToOrderSettle(orderForUI)
                findNavController().navigate(toOrderSettle)
            } else ToastShowUtil.show("未选择菜品")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFirst) showDish(currentDateBean, currentMealBean)
        /*获取用户餐别已订份数*/
//        downloadVM.queryMealOrderSize(orderForUI.ccbToken, currentDateBean.date, currentMealBean?.mealId ?: "-1", orderForUI.custId)
    }

    private fun showDish(dateBean: SelectDateBean, mealBean: OrderMeal?) {
        orderDishAdapter.clear()
        if (mealBean == null) return
        /*获取餐别限购配置*/
        kv.encode(Constant.ORDER_MEAL_LIMIT_SWITCH, mealBean.orderQuota == "1")
        kv.encode(Constant.ORDER_MEAL_LIMIT_SIZE, mealBean.orderQuotaNum.toInt())
        /*获取当前菜品数据*/
        downloadVM.synOrderDish(orderForUI.ccbToken, dateBean.date, mealBean.mealId, orderForUI.custId) { boolean, dishList ->
            handler.post {
                if (!boolean) orderMealVM.getAwaitStatus().value = "同步菜品中"
                else {
                    orderMealVM.getAwaitStatus().value = ""
                    orderDishAdapter.data = dishList
                }
            }
        }
    }

    private fun updateTotalDish() {
        var count: Int = 0
        var totalMoney: Double = 0.0
        dateMenuAdapter.data.forEach { dateMenu ->
            dateMenu.mealList.forEach { mealMenu ->
                mealMenu.dishList.forEach { dish ->
                    count += dish.dishCount
                    totalMoney += dish.dishPrice.toDouble() * dish.dishCount
                }
            }
        }
        binding.tvTotalCount.text = "$count"
        binding.tvTotalMoney.text = String.format("%.02f元", totalMoney)
    }

    private fun clearSelectDish() {
        dateMenuAdapter.data.forEach { dateMenu ->
            dateMenu.mealList.forEach { mealMenu ->
                mealMenu.dishList.forEach {
                    if (currentDateBean.date == dateMenu.date && currentMealBean != null && currentMealBean?.mealId == mealMenu.mealId) {
                        it.dishCount = 0
                        orderDishAdapter.updateDishCount(it)
                    }
                }
            }
        }
        dateMenuAdapter.clear()
        downloadVM.getMenuList().clear()
        updateTotalDish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mvControl.stopAnima()
    }
}