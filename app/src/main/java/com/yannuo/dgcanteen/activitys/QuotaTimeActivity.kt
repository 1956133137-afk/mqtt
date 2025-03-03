package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.adapters.QuotaTimeAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityQuotaTimeBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.QuotaTimeTable
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.PickerView
import java.util.*

class QuotaTimeActivity : BaseActivity<ActivityQuotaTimeBinding>() {
    private val mmkv = MMKV.defaultMMKV()
    private val quotaTimeAdapter by lazy { QuotaTimeAdapter() }
    private val quotaTimeList: MutableList<QuotaTimeTable> = mutableListOf()
    private val hourList: MutableList<String> = mutableListOf()
    private val minuteList: MutableList<String> = mutableListOf()
    private var defaultHour: String = ""
    private var defaultMinute: String = ""

    private var startHour = ""
    private var startMinute = ""
    private var endHour = ""
    private var endMinute = ""

    override fun bindLayout() {
        binding = ActivityQuotaTimeBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initEvent()
        initObject()
    }

    private fun initObject() {
        binding.etDefaultAmount.setText(mmkv.decodeString(Constant.QUOTA_TIME_DEFAULT_AMOUNT, "0.00"))
        // 设置时间选择默认数据
        defaultHourMinute()
        binding.startHourPv.setData(hourList, defaultHour)
        binding.startMinutePv.setData(minuteList, defaultMinute)
        binding.endHourPv.setData(hourList, defaultHour)
        binding.endMinutePv.setData(minuteList, defaultMinute)
        // 显示分段时间
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = quotaTimeAdapter
        changeUI()
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener { finish() }
        // 监听输入金额
        binding.etDefaultAmount.setOnClickListener { binding.etDefaultAmount.setSelection(binding.etDefaultAmount.text.length) }
        binding.etDefaultAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                val payMoneyStr = editable.toString()
                if (isValidMoney(payMoneyStr)) mmkv.encode(
                    Constant.QUOTA_TIME_DEFAULT_AMOUNT,
                    String.format("%.02f", payMoneyStr.toDouble())
                )
                else {
                    LogUtil.d(TAG, "金额格式异常")
                    ToastShowUtil.show("金额格式异常")
                    mmkv.encode(Constant.QUOTA_TIME_DEFAULT_AMOUNT, "0.00")
                }
            }
        })
        //
        binding.startHourPv.setOnSelectListener(object : PickerView.OnSelectListener {
            override fun onSelect(text: String) {
                startHour = text
            }
        })
        binding.startMinutePv.setOnSelectListener(object : PickerView.OnSelectListener {
            override fun onSelect(text: String) {
                startMinute = text
            }
        })
        binding.endHourPv.setOnSelectListener(object : PickerView.OnSelectListener {
            override fun onSelect(text: String) {
                endHour = text
            }
        })
        binding.endMinutePv.setOnSelectListener(object : PickerView.OnSelectListener {
            override fun onSelect(text: String) {
                endMinute = text
            }
        })
        binding.btnAdd.setOnClickListener {
            val startTime = "${startHour}:${startMinute}:00"
            val endTime = "${endHour}:${endMinute}:59"
            val quotaAmountStr = binding.etQuotaAmount.text.toString()
            when {
                !isRepeatTime(startTime, endTime) -> {
                    ToastShowUtil.show("\"时间区间与已有区间冲突\" 或 \"开始时间大于结束时间\"")
                    return@setOnClickListener
                }
                !isValidMoney(quotaAmountStr) -> {
                    ToastShowUtil.show("金额格式异常")
                    return@setOnClickListener
                }
            }
            val quotaAmount = String.format("%.02f", quotaAmountStr.toDouble())
            val quotaTime = QuotaTimeTable().apply {
                this.startTime = startTime
                this.endTime = endTime
                this.quotaAmount = quotaAmount
            }
            LogUtil.d(TAG, Gson().toJson(quotaTime))
            DishesDBHelper.getInstance().insertQuotaTime(quotaTime)
            changeUI()
            ToastShowUtil.show("添加成功")
        }
    }

    private fun changeUI() {
        //更新UI
        quotaTimeList.clear()
        quotaTimeList.addAll(DishesDBHelper.getInstance().queryQuotaTime())
        quotaTimeList.sortWith(compareBy { it.startTime })
        quotaTimeAdapter.data = quotaTimeList
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            val inputMethodManager = MyApplication.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager != null && view != null) inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
        return super.onTouchEvent(event)
    }

    private fun defaultHourMinute() {
        val timeStr = TimeUtil.timeFormat("HH:mm", System.currentTimeMillis())
        defaultHour = timeStr.substring(0, timeStr.indexOf(':'))
        defaultMinute = timeStr.substring(timeStr.indexOf(':') + 1)
//        LogUtil.d(TAG, "$defaultHour $defaultMinute")
        var minute = 0
        while (minute < 60) {
            val value = String.format("%02d", minute)
            if (minute < 24) hourList.add(value)
            minuteList.add(value)
            minute++
        }
    }

    private fun isValidMoney(str: String): Boolean {
        val regex = Regex("""^(0|[1-9]\d{0,5})(\.\d{0,2})?$""", RegexOption.IGNORE_CASE)
        return regex.matches(str)
    }

    private fun isRepeatTime(startTime: String, endTime: String): Boolean {
        val currentDate = TimeUtil.timeFormat("yyyy/MM/dd", System.currentTimeMillis())
        val newStart: Long = Date("$currentDate $startTime").time
        val newEnd: Long = Date("$currentDate $endTime").time
        if (newStart >= newEnd) return false
        DishesDBHelper.getInstance().queryQuotaTime().forEach {
            val oldStart: Long = Date("$currentDate ${it.startTime}").time
            val oldEnd: Long = Date("$currentDate ${it.endTime}").time
            if (newEnd in oldStart..oldEnd || newStart in oldStart..oldEnd) return false
            if (oldEnd in newStart..newEnd || oldStart in newStart..newEnd) return false
        }
        return true
    }
}