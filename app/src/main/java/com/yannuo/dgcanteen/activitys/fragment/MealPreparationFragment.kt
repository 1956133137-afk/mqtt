package com.yannuo.dgcanteen.activitys.fragment

import android.graphics.Color
import android.os.Handler
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CalendarView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.MealPreparationVM
import com.yannuo.dgcanteen.adapters.MealPreparationAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentMealPreparationBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog


class MealPreparationFragment : BaseFragment<FragmentMealPreparationBinding>(), View.OnClickListener {
    private val mealPreparationVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application))[MealPreparationVM::class.java] }
    private val mealPreparationAdapter by lazy { MealPreparationAdapter(this) }
    private var inputPosition: Byte = 0
    private var mView: CalendarView? = null
    private var awaitingDialog: AwaitingDialog? = null
    private val handler: Handler = Handler(MyApplication.applicationContext.mainLooper)
    private var dataMap = HashMap<String, String>()
    private var adapter: ArrayAdapter<String>? = null
    private var options: ArrayList<String>? = null
    private var spinnerTop: Int? = null

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentMealPreparationBinding.inflate(layoutInflater)
        mealPreparationVM.setUserId()
        initObj()
        initData()
        initEvent()
    }

    override fun onStop() {
        super.onStop()
        if(awaitingDialog != null) awaitingDialog?.dismiss()
    }

    private fun initObj() {
        val time = DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()).toString()
        binding.tvCalendarBeginTime.text = time
        binding.tvCalendarDeadlineTime.text = time
        mealPreparationVM.setListener(object :MealPreparationVM.OnMealListener{
            override fun onMeal(type: Int, data: Any) {
                handler.post {
                    if (type == 0) awaitingDialog?.dismiss()
                }
            }
        })
        dataMap = HashMap()
        dataMap["全部"] = "0"
        options = ArrayList(dataMap.keys)
        adapter = ArrayAdapter(requireContext(),R.layout.item_spinner,options!!)
        binding.spinnerText.adapter = adapter

        binding.spinnerText.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val s = options?.get(position)
                if(s == "全部")spinnerTop = null
                else spinnerTop = dataMap[s]?.toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun synMealPreparation() {
        showAwaitDialog("订单查询中")
        var name = binding.nameText.text.toString()
        val BeginTime = binding.tvCalendarBeginTime.text.toString()
        val DeadlineTime = binding.tvCalendarDeadlineTime.text.toString()
        val listOf = listOf(BeginTime, DeadlineTime)
        mealPreparationVM.queryMealList(1,name,spinnerTop,listOf,"4"){ i, orderList ->
            handler.post {
                mealPreparationAdapter.data = orderList
            }
        }
        
        mealPreparationVM.queryAllMeal { 
            handler.post {
                options?.subList(1,options!!.size)?.clear()
                options?.addAll(it.values)
                adapter?.notifyDataSetChanged()
                MyApplication.mealMap = it
            }
        }
    }

    private fun initData() {
        binding.rvItems.layoutManager = GridLayoutManager(requireContext(), 1)
        binding.rvItems.adapter = mealPreparationAdapter
        synMealPreparation()
    }

    private fun initEvent() {
        binding.tvCalendarBeginTime.setOnClickListener(this)
        binding.tvCalendarDeadlineTime.setOnClickListener(this)
        binding.tvSearch.setOnClickListener {
            mealPreparationVM.setUserId()
            synMealPreparation()
        }
    }

    override fun onClick(v: View?) {
        if (v!!.id == binding.tvCalendarBeginTime.id && v.id != View.NO_ID) {
            inputPosition = 0
            showCalendar()
        } else if (v.id == binding.tvCalendarDeadlineTime.id && v.id != View.NO_ID) {
            inputPosition = 1
            showCalendar()
        }
    }

    private fun showAwaitDialog(str: String) {
        if (awaitingDialog == null) awaitingDialog = AwaitingDialog(requireContext())
        if (awaitingDialog?.isShowing == false) awaitingDialog?.show()
        awaitingDialog?.updateText(str)
    }

    private fun showCalendar() {
        if (mView != null) binding.clyCalendar.removeView(mView)
        mView = CalendarView(requireContext())
        mView!!.setBackgroundColor(Color.WHITE)
        mView!!.dateTextAppearance= R.style.SmallerCalendarText  //控制日期字体
        mView!!.scaleX = 1.5f
        mView!!.scaleY = 1.5f
//        mView!!.weekDayTextAppearance = R.style.WeekDayTextStyle  //控制星期字体
        val lParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lParams.topMargin = 51
        lParams.leftMargin = 50
        lParams.topToBottom = binding.tvCalendarBeginTime.id
        lParams.leftToLeft = binding.tvCalendarBeginTime.id
        mView!!.setOnDateChangeListener{ view: CalendarView, year: Int, month: Int, dayOfMonth: Int ->
            var str = String.format("%1\$d-%2$02d-", year, month + 1)
            str += String.format("%02d", dayOfMonth)
            if (inputPosition.toInt() == 0) binding.tvCalendarBeginTime.text = str
            else binding.tvCalendarDeadlineTime.text = str
            binding.clyCalendar.removeView(mView)
        }
        binding.clyCalendar.addView(mView, lParams)
    }

    override fun onDestroy() {
        mealPreparationAdapter.release()
        awaitingDialog?.cancel()
        super.onDestroy()
    }
}