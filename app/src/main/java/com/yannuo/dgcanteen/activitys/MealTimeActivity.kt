//package com.yannuo.dgcanteen.activitys
//
//import android.annotation.SuppressLint
//import android.os.CountDownTimer
//import android.text.format.DateFormat
//import android.view.View
//import androidx.lifecycle.ViewModelProvider
//import com.proembed.service.MyService
//import com.tencent.mmkv.MMKV
//import com.yannuo.dgcanteen.R
//import com.yannuo.dgcanteen.activitys.viewModel.MealTimeVM
//import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
//import com.yannuo.dgcanteen.databinding.ActivityMealTimeBinding
//import com.yannuo.dgcanteen.dialogView.AwaitingDialog
//import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
//import com.yannuo.dgcanteen.interfaces.CallbackListener
//import com.yannuo.dgcanteen.model.MessageEvent
//import com.yannuo.dgcanteen.model.PayForUI
//import com.yannuo.dgcanteen.model.SwForUI
//import com.yannuo.dgcanteen.networkstate.NetworkStateManager
//import com.yannuo.dgcanteen.util.*
//import kotlinx.coroutines.*
//import org.greenrobot.eventbus.EventBus
//import org.json.JSONObject
//import java.util.concurrent.TimeUnit
//
///**
// * @dsc     餐次模式
// * @Author  LiWeiZhong
// * @Date    2024/11/26 10:58
// * @Version 1.0
// */
//class MealTimeActivity: BaseActivity<ActivityMealTimeBinding>(),
//    NetworkStateManager.NetWorkListener, CallbackListener {
//
//    private val payViewModel by lazy { ViewModelProvider(this)[PayViewModel::class.java] }
//    private val viewModel by lazy { ViewModelProvider(this)[MealTimeVM::class.java] }
//    private lateinit var awaitPayDialog: AwaitingDialog
//
//    private val kv by lazy { MMKV.defaultMMKV() }
//    private var mXService: MyService? = null
//    private var navigation = true
//    private var mealId = -1
//    private var countDown: CountDownTimer? = null
//    private var countDownTime: Long = 10L
//
//
//    private val mHandler = CoroutineExceptionHandler{ coroutineContext, throwable ->
//        throwable.printStackTrace()
//        LogUtil.e(TAG, "error: ${throwable.message}")
//    }
//
//    override fun bindLayout() {
//        binding = ActivityMealTimeBinding.inflate(layoutInflater)
//    }
//
//    override fun onInit() {
//        payViewModel.setIsSw(1)
//        initData()
//        checkTime()
//        initEvent()
//    }
//
//    private fun initData() {
//        NetworkStateManager.getInstance().registerObserver(this)
//        mXService = MyService(this)
//
//        if (NetworkStateManager.getInstance().isOnline(this).not()) {
//            binding.network.setImageResource(R.drawable.ic_wifi_no)
//        }else{
//            netWorkStatus("0")
//        }
//
//        viewModel.showErrorToast.observe(this) {
//            ToastShowUtil.show(it)
//        }
//
//        payViewModel.openPayStatus()
//        payViewModel.listener = this
//    }
//
//    private fun initEvent() {
//        binding.tvTitle.setOnLongClickListener {
//            navigation = !navigation
//            mXService?.hideNavBar = navigation
//            if (!navigation) ToastShowUtil.show("导航可用")
//            true
//        }
//
//        binding.ibtBack.setOnClickListener {
//            finish()
//        }
//    }
//
//    private fun checkTime() {
//        mScope.launch {
//            while (isActive) {
//                val mMealId = TimeUtil.CurrentTimeSection()
//                LogUtil.i(TAG, "checkTime mealId: $mMealId")
//                mealId = mMealId
//                val str = StringBuilder()
//                when (mealId) {
//                    0 -> {
//                        kv.encode(Constant.IS_USE_MEAL, 0)
//                        str.append(resources.getString(R.string.unOpen_meal))
//                    }
//                    else -> {
//                        kv.encode(Constant.IS_USE_MEAL, 1)
//                        val meal = DishesDBHelper.getInstance().queryToMeals(mealId)
//                        if (meal == null) {
//                            kv.encode(Constant.IS_USE_MEAL, 0)
//                            str.append(resources.getString(R.string.unOpen_meal))
//                            LogUtil.e(TAG, "checkTime --> meal is null")
//                        } else {
//                            str.append(meal.mealName + " ")
//                            str.append(
//                                DateFormat.format("HH:mm", meal.startTime).toString() + "~"
//                            )
//                            str.append(DateFormat.format("HH:mm", meal.endTime).toString())
//                        }
//                    }
//                }
//                withContext(Dispatchers.Main) {
//                    binding.mealTime.text = str
//                }
//                delay(5000)
//            }
//        }
//    }
//
//    override fun netWorkStatus(statue: String) {
//        mScope.launch(Dispatchers.Main + mHandler) {
//            when (statue) {
//                "0" -> {
//                    binding.network.setImageResource(R.drawable.ic_wifi)
//                    if (kv.decodeBool(Constant.SWITCH, false)) {
//                        kv.encode(Constant.SWITCH, false)
//                        EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
//                    }
//                }
//                else -> {
//                    binding.network.setImageResource(R.drawable.ic_wifi_no)
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        updateOrderCount()
//    }
//
//    override fun onStop() {
//        super.onStop()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        NetworkStateManager.getInstance().unRegisterObserver(this)
//        if (this::awaitPayDialog.isInitialized) awaitPayDialog.cancel()
//        payViewModel.closePayStatus()
//        payViewModel.listener = null
//    }
//
//    fun redraw(view: View) {
//        binding.drawHook.clearData()
//        binding.drawHook.invalidate()
//        binding.drawCross.clearData()
//        binding.drawCross.invalidate()
//    }
//
//    @SuppressLint("SetTextI18n")
//    override fun onOtherListener(event: Int, any: Any?) {
//        mScope.launch(Dispatchers.Main + mHandler) {
//            try {
//                if (this@MealTimeActivity::awaitPayDialog.isInitialized && awaitPayDialog.isShowing) awaitPayDialog.dismiss()
//                when (event) {
//                    1 -> { //开始支付
//                        if (!this@MealTimeActivity::awaitPayDialog.isInitialized) awaitPayDialog = AwaitingDialog(this@MealTimeActivity)
//                        awaitPayDialog.show()
//                        awaitPayDialog.updateText("支付中")
//                    }
//                    2 -> { //异常
//                        ToastShowUtil.show("支付异常：$any")
//                        LogUtil.d(TAG, "支付异常：$any")
//                    }
//                    3-> { //支付成功
//                        countDown?.cancel()
//                        val payForUI = any as PayForUI
//                        if (payForUI.result == "Y") {
//                            binding.paySuccess.visibility = View.VISIBLE
//                            binding.payFail.visibility = View.GONE
//                            binding.drawHook.clearData()
//                            binding.drawHook.invalidate()
//
//                            addScreenData(payForUI.swForUI)
//
//                            updateOrderCount()
//
//                            CommonAndDpToPxUtil.speakWork("消费成功！")
//                        } else {
//                            binding.paySuccess.visibility = View.GONE
//                            binding.payFail.visibility = View.VISIBLE
//                            binding.drawCross.clearData()
//                            binding.drawCross.invalidate()
//                            binding.tvFailMsg.text = "消费失败！${any.errMsg}"
//
//                            CommonAndDpToPxUtil.speakWork("消费失败！${any.errMsg}")
//                        }
//                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(countDownTime) + 200, 1000) {
//                            override fun onTick(mil: Long) {}
//
//                            override fun onFinish() {
//                                clearScreenData()
//                            }
//                        }
//                        countDown?.start()
//                        delay(500)
//                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
//                    }
//                    4 -> { //支付失败
//                        countDown?.cancel()
//                        binding.paySuccess.visibility = View.GONE
//                        binding.payFail.visibility = View.VISIBLE
//                        binding.drawCross.clearData()
//                        binding.drawCross.invalidate()
//                        binding.tvFailMsg.text = "消费失败！${any as String}"
//                        CommonAndDpToPxUtil.speakWork("消费失败！${any as String}")
//                        countDown = object : CountDownTimer(TimeUnit.SECONDS.toMillis(countDownTime) + 200, 1000) {
//                            override fun onTick(mil: Long) {}
//
//                            override fun onFinish() {
//                                clearScreenData()
//                            }
//                        }
//                        countDown?.start()
//                        delay(500)
//                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
//                    }
//                    5 -> { //无效码
//                        when (any as Int) {
//                            1 -> {
//                                ToastShowUtil.show("请刷新付款码再支付")
//                                CommonAndDpToPxUtil.speakWork("无效码，请刷新付款码再支付")
//                            }
//                            2 -> {
//                                ToastShowUtil.show("请检查网络,不支持离线聚合支付!")
//                                CommonAndDpToPxUtil.speakWork("不支持离线聚合支付")
//                            }
//                            else -> {
//                                ToastShowUtil.show("请切换离线码再支付")
//                                CommonAndDpToPxUtil.speakWork("请切换离线码再支付")
//                            }
//                        }
//                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
//                    }
//                    6 -> { //异常
//                        ToastShowUtil.show("支付异常：${any as? String}")
//                        LogUtil.d(TAG, "支付异常：$any")
//                        payViewModel.setPayState(PayViewModel.PayStatus.PAY)
//                    }
//                    7 -> {
////                        val bean = any as SimpleForUI
////                        val str = when (bean.way!!.toInt()) {
////                            20 -> "微信"
////                            else -> "支付宝"
////                        }
////                        if (bean.state == 0) {
////                            CommonAndDpToPxUtil.speakWork("${str}收款${bean.payment}元")
////                        } else {
////                            CommonAndDpToPxUtil.speakWork("${str}支付失败了")
////                        }
//                    }
//                }
//            } catch (e: Exception) {
//                LogUtil.e(TAG, "${e.cause} ${e.message}")
//            }
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun updateOrderCount() {
//        val date = TimeUtil.timeFormat("yyyy-MM-dd", System.currentTimeMillis())
//        val breakfastBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "早餐")
//        val lunchBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "午餐")
//        val dinnerBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "晚餐")
//        val supperBill = DishesDBHelper.getInstance().querySwPayOrderListByDate(date, "夜宵")
//        binding.tvBreakfastBill.text = "早餐：${breakfastBill.size.toString()} 单"
//        binding.tvLunchBill.text = "午餐：${lunchBill.size.toString()} 单"
//        binding.tvDinnerBill.text = "晚餐：${dinnerBill.size.toString()} 单"
//        binding.tvSupperBill.text = "夜宵：${supperBill.size.toString()} 单"
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun addScreenData(swForUI: SwForUI) {
//        LogUtil.i(TAG, "addScreenData swForUI: $swForUI")
//        binding.tvUsername.text = "姓名：${swForUI.username}"
//        binding.tvBalance.text = "余额：${swForUI.balance} 元"
//        var breakfastTime = swForUI.breakfastTime
//        var lunchTime = swForUI.lunchTime
//        var dinnerTime = swForUI.dinnerTime
//        var supperTime = swForUI.supperTime
//        if (swForUI.result == "Y") {
//            //显示餐标
//            binding.llRuleInfo.visibility = View.VISIBLE
//            if (swForUI.useMealRuleName.isNullOrBlank()) binding.llRuleInfo.visibility = View.GONE
//            if (swForUI.restTime == 0) {
//                binding.tvRuleUse.visibility = View.GONE
//                binding.tvRestTime.visibility = View.GONE
//                binding.tvRuleSubsidy.visibility = View.GONE
//            }
//            binding.tvMealRule.text = "餐标：${swForUI.useMealRuleName}"
//            binding.tvRuleUse.text = "耗次：${swForUI.everyUseTime} 次"
//            binding.tvRestTime.text = "余次：${swForUI.restTime - swForUI.everyUseTime.trim().toInt()} 次"
//            binding.tvRulePrice.text = "单价：${swForUI.rulePrice.toString()} 元"
//            binding.tvRuleSubsidy.text = "补贴：${swForUI.subsidy} 元"
//
//            when (swForUI.standardMealName) {
//                "早餐" -> breakfastTime -= swForUI.everyUseTime.trim().toInt()
//                "午餐" -> lunchTime -= swForUI.everyUseTime.trim().toInt()
//                "晚餐" -> dinnerTime -= swForUI.everyUseTime.trim().toInt()
//                "夜宵" -> supperTime -= swForUI.everyUseTime.trim().toInt()
//            }
//        } else binding.llRuleInfo.visibility = View.GONE
//        binding.tvBreakfast.text = "早餐：$breakfastTime 次"
//        binding.tvLunch.text = "午餐：$lunchTime 次"
//        binding.tvDinner.text = "晚餐：$dinnerTime 次"
//        binding.tvSupper.text = "夜宵：$supperTime 次"
//    }
//
//    private fun clearScreenData() {
//        binding.paySuccess.visibility = View.GONE
//        binding.payFail.visibility = View.GONE
//
//        binding.tvUsername.text = "姓名："
//        binding.tvBalance.text = "余额："
//        binding.tvBreakfast.text = "早餐："
//        binding.tvLunch.text = "午餐："
//        binding.tvDinner.text = "晚餐："
//        binding.tvSupper.text = "夜宵："
//
//        binding.llRuleInfo.visibility = View.GONE
//        binding.tvMealRule.text = "餐标："
//        binding.tvRuleUse.text = "耗次："
//        binding.tvRestTime.text = "余次："
//        binding.tvRulePrice.text = "单价："
//        binding.tvRuleSubsidy.text = "补贴："
//    }
//}