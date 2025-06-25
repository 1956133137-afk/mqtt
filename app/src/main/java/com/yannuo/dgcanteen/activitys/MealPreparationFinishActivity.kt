package com.yannuo.dgcanteen.activitys

import android.os.CountDownTimer
import android.os.Handler
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.DownloadVM
import com.yannuo.dgcanteen.activitys.viewModel.MealPreparationVM
import com.yannuo.dgcanteen.adapters.MealPreparationFinishAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivityMealPreparationFinishBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.model.SelectDateBean
import com.yannuo.dgcanteen.util.Constant
import java.util.Calendar

class MealPreparationFinishActivity : BaseActivity<ActivityMealPreparationFinishBinding>() {
    private val mealPreparationVM by lazy { ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[MealPreparationVM::class.java] }
    private val mealPreparationFinishAdapter by lazy { MealPreparationFinishAdapter(this) }
    private val handler: Handler = Handler(MyApplication.applicationContext.mainLooper)
    private var awaitingDialog: AwaitingDialog? = null
    private var confirmDialog: ConfirmDialog? = null
    private var countDown: CountDownTimer? = null
    private val kv = MMKV.defaultMMKV()
    private val downloadVM by lazy { ViewModelProvider(this)[DownloadVM::class.java] }
    private var meal: MutableList<SelectDateBean>? = null

    override fun bindLayout() {
        binding = ActivityMealPreparationFinishBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    private fun initObject() {
        meal = downloadVM.getDateWeek()
        mealPreparationVM.setUserId()
        mealPreparationVM.setListener(object : MealPreparationVM.OnMealListener {
            override fun onMeal(type: Int, data: Any) {
                handler.post {
                    if (type == 0) awaitingDialog?.dismiss()
                }
            }
        })
        binding.orderListView.layoutManager = GridLayoutManager(this, 3)
        binding.orderListView.adapter = mealPreparationFinishAdapter
        synOrderRecord()
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun synOrderRecord() {
        showAwaitDialog("同步订餐记录中•••")
        val decodeInt = kv.decodeInt(Constant.ORDER_ADVANCE_DAY, 6)
        Constant.ORDER_ADVANCE_DAY
        val calendar = Calendar.getInstance()
        var year = calendar[Calendar.YEAR]
        var month = calendar[Calendar.MONTH] + 1
        var day = calendar[Calendar.DAY_OF_MONTH]
        var day2 = "$year-${month.toString().padStart(2,'0')}-${day.toString().padStart(2,'0')}"
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        year = calendar[Calendar.YEAR]
        month = calendar[Calendar.MONTH] + 1
        day = calendar[Calendar.DAY_OF_MONTH]
        var day1 = "$year-${month.toString().padStart(2,'0')}-${day.toString().padStart(2,'0')}"
        val listOf = listOf(day1, day2)
        mealPreparationVM.queryMealList(false,1,null,null,listOf,"1"){ _, orderList, msg ->
            handler.post {
                mealPreparationFinishAdapter.data = orderList
            }
        }
    }

    private fun showAwaitDialog(str: String) {
        if (awaitingDialog == null) awaitingDialog = AwaitingDialog(this)
        if (awaitingDialog?.isShowing == false) awaitingDialog?.show()
        awaitingDialog?.updateText(str)
    }

    override fun onDestroy() {
        mealPreparationFinishAdapter.release()
        awaitingDialog?.cancel()
        confirmDialog?.cancel()
        countDown?.cancel()
        super.onDestroy()
    }
}