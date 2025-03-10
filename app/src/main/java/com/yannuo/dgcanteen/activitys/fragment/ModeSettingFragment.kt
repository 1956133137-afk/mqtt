package com.yannuo.dgcanteen.activitys.fragment

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.QuotaTimeActivity
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.adapters.SimpleDownAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentModeSettingBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PersonList
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/29 15:48
 **/
class ModeSettingFragment : Fragment() {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentModeSettingBinding
    private lateinit var kv: MMKV
    private var popup: PopupWindow = PopupWindow()
    private var listView: ListView? = null
    private val dataList: ArrayList<String> = arrayListOf<String>("刷脸支付模式", "刷卡支付模式", "扫码支付模式", "码卡支付模式")
    private val printerList: ArrayList<String> = arrayListOf<String>("按天打印", "全部打印")
    private var printerListView: ListView? = null
    private lateinit var mScope: CoroutineScope
    private lateinit var mHandle: CoroutineExceptionHandler
    private lateinit var confirmDialog: ConfirmDialog
    private lateinit var awaitingDialog: AwaitingDialog
    private val mContext = MyApplication.applicationContext
    private var self_help = false
    private var saveCheck = false
//    private var mService: CameraService? = null

//    private val connection = object : ServiceConnection {
//
//        override fun onServiceConnected(className: ComponentName, service: IBinder) {
//
//            val binder = service as CameraService.LocalBinder
//            mService = binder.getService()
//
//        }
//
//        override fun onServiceDisconnected(arg0: ComponentName) {
//            mService = null
//        }
//    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentModeSettingBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
        initData()
        verifyType()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // 分时段金额
//        val currentQuotaAmount = kv.decodeString(Constant.QUOTA_TIME_DEFAULT_AMOUNT, "0.00")
//        kv.encode(Constant.QUOTA_AMOUNT, currentQuotaAmount)
//        binding.fixedSum.setText(currentQuotaAmount)
        binding.fixedSum.setText(kv.decodeString(Constant.QUOTA_AMOUNT, "0.00"))
    }

    private fun initObject() {
        kv = MMKV.defaultMMKV()
        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler $e ${e.message}")
        }
        mScope = CoroutineScope(Dispatchers.Default + mHandle)

        binding.tvPeopleCount.text = "同步人数：${DishesDBHelper.getInstance().getPersonsCount()}"
//        val intent = Intent(requireContext(), CameraService::class.java)
//        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)

//        val count = FaceHandler.getInstance()?.ksHandler?.getLocalGroupFaceNum(Constant.GROUP_NAME) ?: 0
//        binding.tvFaceCount.text = "人脸同步数：$count"


        listView = ListView(requireContext())
        listView?.divider = null
        listView?.isVerticalScrollBarEnabled = false
        listView?.adapter = SimpleDownAdapter(requireContext(), dataList)
        listView?.setOnItemClickListener { adapterView, view, position, id -> //下拉框选择
            binding.payMode.text = dataList[position]
            popup.dismiss()
            when (dataList[position]) {
                "刷脸支付模式" -> kv.encode(Constant.PAY_MODE, Constant.PAY_FACE_TYPE)
                "刷卡支付模式" -> kv.encode(Constant.PAY_MODE, Constant.PAY_IC_TYPE)
                "扫码支付模式" -> kv.encode(Constant.PAY_MODE, Constant.PAY_CODE_TYPE)
                "码卡支付模式" -> kv.encode(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)
            }
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY_CHANGE, true))
        }

        printerListView = ListView(requireContext())
        printerListView?.divider = null
        printerListView?.isVerticalScrollBarEnabled = false
        printerListView?.adapter = SimpleDownAdapter(requireContext(), printerList)
        printerListView?.setOnItemClickListener { adapterView, view, position, id -> //下拉框选择
            binding.orderPrinterFormat.text = printerList[position]
            popup.dismiss()
            kv.encode(Constant.ORDER_PRINTER_FORMAT, position)
        }
    }

    private fun initEvent() {
        binding.appMode.setOnClickListener { //切换模式
//            if (!this::confirmDialog.isInitialized)
//                confirmDialog = ConfirmDialog(requireActivity())
//            changeMode()
            changeConsumeMode()
        }
        binding.payMode.setOnClickListener { //切换支付
            popup.width = binding.payMode.width
            popup.height = 400
            popup.contentView = listView
            popup.isOutsideTouchable = true
            popup.showAsDropDown(binding.payMode, 0, 0)
        }
        binding.codeVerification.setOnClickListener { //核销模式
            kv.encode(Constant.CODE_VERIFICATION_SET, binding.codeVerification.isChecked)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY_CHANGE, true))
        }
        binding.displayCardVerify.setOnClickListener {
            kv.encode(Constant.DISPLAY_CARD_VERIFY, binding.displayCardVerify.isChecked)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY_CHANGE, true))
        }
        binding.verifyPersonStatistic.setOnClickListener {
            kv.encode(Constant.VERIFY_PERSON_STATISTIC, binding.verifyPersonStatistic.isChecked)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY_CHANGE, true))
        }
        binding.supportPay.setOnClickListener {
            kv.encode(Constant.SUPPORT_PAY, binding.supportPay.isChecked)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFY_CHANGE, true))
        }
        binding.autoVerify.setOnClickListener {
            var flag = -1
            when {
                binding.autoVerify.isChecked && !binding.codeVerification.isChecked -> flag = 0
                binding.autoVerify.isChecked && binding.autoPay.isChecked -> flag = 1
            }
            val strMsg = when (flag) {
                -1 -> ""
                0 -> "请先开启核销模式"
                else -> "请先关闭自动定额收款"
            }
            if (flag != -1) {
                ToastShowUtil.show(strMsg)
                binding.autoVerify.isChecked = false
            }
            kv.encode(Constant.AUTO_VERIFY, binding.autoVerify.isChecked)
            kv.encode(Constant.VERIFY_CHANGE, false)
        }
        binding.autoPay.setOnClickListener {
            var flag = -1
            when {
                binding.autoPay.isChecked && binding.autoVerify.isChecked -> flag = 0
                binding.autoPay.isChecked && !binding.switchFixed.isChecked -> flag = 1
                binding.autoPay.isChecked && kv.decodeString(Constant.QUOTA_AMOUNT, "0.00")!!.toDouble() <= 0.0 -> flag = 2
            }
            val strMsg = when (flag) {
                -1 -> ""
                0 -> "请先关闭自动核销"
                1 -> "请先开启定额模式"
                else -> "定额收款金额不能为0元"
            }
            if (flag != -1) {
                ToastShowUtil.show(strMsg)
                binding.autoPay.isChecked = false
            }
            kv.encode(Constant.AUTO_PAY, binding.autoPay.isChecked)
        }
        binding.switchUseMealLimitPay.setOnClickListener {
            // 开餐后才能收款
            val check = binding.switchUseMealLimitPay.isChecked
            if (check) kv.encode(Constant.USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH, 1)
            else kv.encode(Constant.USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH, 0)
        }
        binding.mealTimeSwitch.setOnClickListener {
            val switchValue = if (binding.mealTimeSwitch.isChecked) 1 else 0
            kv.encode(Constant.MEAL_TIME_SWITCH, switchValue)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_MEAL_TIME_SWITCH, null))
        }
//        binding.switchMealTimeMode.setOnClickListener {
//            // 餐次消费模式
//            val check = binding.switchMealTimeMode.isChecked
//            if (check) kv.encode(Constant.MEAL_TIME_MODE, 1)
//            else kv.encode(Constant.MEAL_TIME_MODE, 0)
//        }
        binding.tvQuotaTime.setOnClickListener { startActivity(Intent(requireContext(), QuotaTimeActivity::class.java)) }
        binding.switchFixed.setOnClickListener { //定额模式
            if (binding.switchFixed.isChecked && !amountJudgment(binding.fixedSum, Constant.QUOTA_AMOUNT)) {
                binding.switchFixed.isChecked = false
            }
            if (!binding.switchFixed.isChecked && binding.autoPay.isChecked) {
                binding.autoPay.isChecked = false
                kv.encode(Constant.AUTO_PAY, false)
            }
            kv.encode(Constant.QUOTA_SWITCH, binding.switchFixed.isChecked)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUOTA_CHANGE, null))
        }
        binding.queryVerify.setOnClickListener {
            kv.encode(Constant.QUERY_VERIFY, binding.queryVerify.isChecked)
        }

//        binding.mealTime.setOnClickListener {
//            kv.encode(Constant.MEAL_TIME, 10)
//        }
        binding.cbBalance.setOnClickListener {
            kv.encode(Constant.BALANCE_SWITCH, binding.cbBalance.isChecked)
        }

        binding.btnSynPerson.setOnClickListener { view: View? ->
            if (!this::awaitingDialog.isInitialized)
                awaitingDialog = AwaitingDialog(requireActivity())
            awaitingDialog.show()
            awaitingDialog.updateText("同步中")
            downPerson()
//            ToastShowUtil.show("人员信息已同步~")
        }

        binding.llLogUp.setOnClickListener {
            if (!this::awaitingDialog.isInitialized)
                awaitingDialog = AwaitingDialog(requireActivity())
            awaitingDialog.show()
            awaitingDialog.updateText("上传中")
            val serial = CommonAndDpToPxUtil.getDeviceSerial()
            var sdcardPath = "/sdcard/recycle_machine"
            val f = Environment.getExternalStorageDirectory()
            if (f != null) {
                sdcardPath = f.absolutePath + "/device-record/pay/log"
            }
            CoroutineScope(Dispatchers.IO).launch {
                EmailSender.sendEmail(
                    "zhangzhanmian@yannuozhineng.com",
                    "建行开放平台13.3+10.1双屏设备软件日志", sdcardPath,
                    "序列号：${serial}", object : EmailSender.CallbackListener {
                        override fun onStare(code: Int, msg: String?) {
                            requireActivity().runOnUiThread(Runnable {
                                when (code) {
                                    0 -> {
                                        awaitingDialog.cancel()
                                        ToastShowUtil.show("上送成功")
                                    }
                                    10 -> awaitingDialog.show()
                                    else -> awaitingDialog.cancel()
                                }
                            })
                        }
                    })
            }
        }
        binding.orderPrinterFormat.setOnClickListener { //切换支付
            popup.width = binding.orderPrinterFormat.width
            popup.height = 400
            popup.contentView = printerListView
            popup.isOutsideTouchable = true
            popup.showAsDropDown(binding.orderPrinterFormat, 0, 0)
        }
        binding.orderDefaultWay.setOnClickListener { kv.encode(Constant.ORDER_DEFAULT_WAY, binding.orderDefaultWay.isChecked) }
        binding.orderDiscountSwitch.setOnClickListener { kv.encode(Constant.ORDER_DISCOUNT_SWITCH, binding.orderDiscountSwitch.isChecked) }
        binding.orderQuery.setOnClickListener { kv.encode(Constant.ORDER_QUERY, binding.orderQuery.isChecked) }
        // 重复支付判断
        binding.repeatPayJudge.setOnClickListener { kv.encode(Constant.REPEAT_PAY_JUDGE, binding.repeatPayJudge.isChecked) }

        binding.btnSynFace.setOnClickListener { view: View? ->
//            if (!this::awaitingDialog.isInitialized)
//                awaitingDialog = AwaitingDialog(requireActivity())
//            awaitingDialog.show()
//            awaitingDialog.updateText("同步中")
//            if (mService == null) {
//                ToastShowUtil.show("同步失败，服务异常")
//                return@setOnClickListener
//            }
//            mService?.synchFace(object : CallbackListener {
//                override fun onOtherListener(event: Int, any: Any?) {
//                    when (event) {
//                        0 -> {
//                            requireActivity().runOnUiThread {
//                                awaitingDialog.setText(any as String)
//                            }
//                        }
//                        1 -> {
//                            val count =
//                                FaceHandler.getInstance()?.ksHandler?.getLocalGroupFaceNum(Constant.GROUP_NAME)
//                                    ?: 0
//                            requireActivity().runOnUiThread {
//                                awaitingDialog.cancel()
//                                binding.tvFaceCount.text = "同步人脸数：$count"
//                            }
//                        }
//                    }
//                }
//            })
//            ToastShowUtil.show("人员信息已同步~")
        }
    }

    private fun verifyType() {
        binding.spVerify.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                kv.encode(Constant.VERIFY_MODE, p2)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun changeConsumeMode() {
        val array = arrayOf(
            Constant.ORDERING_FOOD_MODE,
            Constant.ORDERING_TWO_MODE,
            Constant.PROCEEDS_MODE,
            Constant.PROCEEDS_TWO_MODE,
            Constant.ORDERING_MEAL_MODE
        )
        val position = byteArrayOf(0)
        val oldPosition = when (kv.decodeString(Constant.APP_MODE)) {
            array[0] -> 0
            array[1] -> 1
            array[2] -> 2
            array[3] -> 3
            else -> 4
        }
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
            .setIcon(R.mipmap.ic_app)
            .setTitle("消费模式切换")
            .setSingleChoiceItems(array, oldPosition) { dialog, which ->
                position[0] = which.toByte()
                LogUtil.i(TAG, "which $which")
            }
            .setNegativeButton("取消") { dialog, which -> dialog?.dismiss() }
            .setPositiveButton("确定") { dialog, which ->
                ToastShowUtil.show(array[position[0].toInt()])
                kv.encode(Constant.APP_MODE, array[position[0].toInt()])

                val restartIntent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
                restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(restartIntent)
                exitProcess(0)
            }
        builder.create()
            .apply {
                show()
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                getButton(DialogInterface.BUTTON_POSITIVE).setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
            }

    }

    private fun initData() {
        reload()
    }

    fun save() {
        var flag = true
        if (!(flag && amountJudgment(binding.fixedSum, Constant.QUOTA_AMOUNT))) flag = false
        if (!(flag && amountJudgment(binding.limitAmount, Constant.LIMIT_AMOUNT))) flag = false
        kv.encode(Constant.TITLE_CONTENT, binding.titleContent.text.toString())
        kv.encode(Constant.MEAL_TIME, binding.mealTime.text.toString().toInt())
        kv.encode(Constant.PAY_RESULT_DIALOG_TIME, binding.mealTimeDialogTime.text.toString().toLong())  //主屏
        kv.encode(Constant.MEAL_TIME_PAY_RESULT_TIME, binding.mealTimePayResultTime.text.toString().toLong())  //副屏
        kv.encode(Constant.MEAL_TIME_QUERY_BALANCE_TIME, binding.mealTimeQueryBalTime.text.toString().toLong()) //副屏
        if (flag) {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUOTA_CHANGE, null))
            ToastShowUtil.show("保存成功: ${mContext.filesDir.absolutePath}/mmkv")
        }
        if (self_help.equals(kv.decodeBool(Constant.BALANCE_SWITCH, false)).not()) {
            val restartIntent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
            restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(restartIntent)
            exitProcess(0)
        }
        if ((saveCheck == kv.decodeBool(Constant.QUERY_VERIFY, false)).not()) {
            val restartIntent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
            restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(restartIntent)
            exitProcess(0)
        }

        kv.encode(Constant.ORDER_ADVANCE_DAY, binding.orderAdvanceDay.text.toString().trim().replace(" ", "").ifEmpty { "0" }.toInt())
    }

    private fun reload() {
        self_help = kv.decodeBool(Constant.BALANCE_SWITCH, false)
        binding.switchUseMealLimitPay.isChecked = kv.decodeInt(Constant.USE_MEAL_TIME_LIMIT_CALCULATE_SWITCH, 0) == 1
        binding.mealTimeSwitch.isChecked = kv.decodeInt(Constant.MEAL_TIME_SWITCH, 0) == 1
//        binding.switchMealTimeMode.isChecked = kv.decodeInt(Constant.MEAL_TIME_MODE, 0) == 1
        binding.switchFixed.isChecked = kv.decodeBool(Constant.QUOTA_SWITCH, false)
        binding.cbBalance.isChecked = self_help
        saveCheck = kv.decodeBool(Constant.QUERY_VERIFY, false)
        binding.codeVerification.isChecked = kv.decodeBool(Constant.CODE_VERIFICATION_SET, false)
        binding.autoVerify.isChecked = kv.decodeBool(Constant.AUTO_VERIFY, false)
        binding.supportPay.isChecked = kv.decodeBool(Constant.SUPPORT_PAY, true)
        binding.payMode.text = dataList[kv.decodeInt(Constant.PAY_MODE, Constant.PAY_CODE_IC_TYPE)]
        binding.autoPay.isChecked = kv.decodeBool(Constant.AUTO_PAY, false)
        binding.queryVerify.isChecked = saveCheck
        binding.mealTime.setText(kv.decodeInt(Constant.MEAL_TIME, 10).toString())
//        binding.fixedSum.setText(kv.decodeString(Constant.QUOTA_AMOUNT, "0.00"))
        binding.mealTimeDialogTime.setText(kv.decodeLong(Constant.PAY_RESULT_DIALOG_TIME, 3L).toString())  //主屏
        binding.mealTimePayResultTime.setText(kv.decodeLong(Constant.MEAL_TIME_PAY_RESULT_TIME, 10L).toString())  //副屏
        binding.mealTimeQueryBalTime.setText(kv.decodeLong(Constant.MEAL_TIME_QUERY_BALANCE_TIME, 10L).toString()) //副屏
        binding.limitAmount.setText(kv.decodeString(Constant.LIMIT_AMOUNT, "30.00"))
        binding.titleContent.setText(kv.decodeString(Constant.TITLE_CONTENT, ""))
        if (kv.decodeString(Constant.APP_MODE) == null) kv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
        binding.appMode.text = kv.decodeString(Constant.APP_MODE)
        binding.tvFinalTime.text = kv.decodeString(Constant.FINAL_TIME)

        binding.displayCardVerify.isChecked = kv.decodeBool(Constant.DISPLAY_CARD_VERIFY, false)
        binding.verifyPersonStatistic.isChecked = kv.decodeBool(Constant.VERIFY_PERSON_STATISTIC, true)

        binding.orderPrinterFormat.text = printerList[kv.decodeInt(Constant.ORDER_PRINTER_FORMAT, 0)]
        binding.orderAdvanceDay.setText(kv.decodeInt(Constant.ORDER_ADVANCE_DAY, 6).toString())
        binding.orderDefaultWay.isChecked = kv.decodeBool(Constant.ORDER_DEFAULT_WAY, false)
        binding.orderDiscountSwitch.isChecked = kv.decodeBool(Constant.ORDER_DISCOUNT_SWITCH, false)
        binding.orderQuery.isChecked = kv.decodeBool(Constant.ORDER_QUERY, false)

        val verifyType = resources.getStringArray(R.array.spVerify)
        val spVerifyAdapter = ArrayAdapter<String>(requireContext(), R.layout.item_text, verifyType)
        binding.spVerify.adapter = spVerifyAdapter
        val verifyMode = kv.decodeInt(Constant.VERIFY_MODE).toString()
        for (vm in verifyType.indices) {
            if (verifyType[vm].equals(verifyMode)) {
                binding.spVerify.setSelection(vm)
            }
        }
        binding.spVerify.setSelection(kv.decodeInt(Constant.VERIFY_MODE))
        // 重复支付判断
        binding.repeatPayJudge.isChecked = kv.decodeBool(Constant.REPEAT_PAY_JUDGE, true)
    }

    private fun amountJudgment(view: EditText, name: String): Boolean {
        val amountStr = view.text.toString()
        if (isFormJudgment(amountStr)) {
            val payMoney = String.format("%.02f", amountStr.toDouble())
            kv.encode(name, payMoney)
            if (name == Constant.QUOTA_AMOUNT) kv.encode(Constant.QUOTA_TIME_DEFAULT_AMOUNT, payMoney)
            mScope.launch { withContext(Dispatchers.Main) { view.setText(kv.decodeString(name)) } }
            return true
        } else {
            if (view.id == binding.fixedSum.id) {
                binding.switchFixed.isChecked = false
                binding.autoPay.isChecked = false
            }
            ToastShowUtil.show("输入金额有误")
            return false
        }
    }

    private fun isFormJudgment(str: String): Boolean { //判断格式
        val regex = Regex("""^(0|[1-9]\d{0,5})(\.\d{0,2})?$""", RegexOption.IGNORE_CASE)
        return regex.matches(str)
    }

    private fun changeMode() {
        confirmDialog.apply {
            show()
            val strText = when (kv.decodeString(Constant.APP_MODE)) {
                Constant.ORDERING_FOOD_MODE -> "是否切换为 ${Constant.PROCEEDS_MODE} 并且重启应用？"
                Constant.PROCEEDS_MODE -> "是否切换为 ${Constant.ORDERING_TWO_MODE} 并且重启应用？"
                Constant.ORDERING_TWO_MODE -> "是否切换为 ${Constant.ORDERING_FOOD_MODE} 并且重启应用？"
                else -> "是否切换为  并且重启应用？"
            }
            val str = SpannableString(strText)
            str.setSpan(StyleSpan(Typeface.BOLD), 5, strText.length - 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.tvText.text = str

            setListener(object : ConfirmDialog.OnConfirmCallback {
                override fun confirmCallback(flag: Boolean) {
                    if (flag) {
                        when (kv.decodeString(Constant.APP_MODE)) {
                            Constant.ORDERING_FOOD_MODE -> kv.encode(Constant.APP_MODE, Constant.PROCEEDS_MODE)
                            Constant.PROCEEDS_MODE -> kv.encode(Constant.APP_MODE, Constant.ORDERING_FOOD_MODE)
                        }
                        val restartIntent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
                        restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(restartIntent)
                        exitProcess(0)
                    }
                }
            })
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun downPerson() {
        mScope.launch {
            val mv = MMKV.defaultMMKV()
            val repository = PayRepositoryOfPay()
            var finish = false
            var currentPage = 1
            var failTime = 0
            DishesDBHelper.getInstance().deleteAllPersons()
            LogUtil.d(TAG, "准备全量更新人员")
            do {
                val res = repository.downPerson(500, currentPage)
                try {
                    if (res.code == "200") {
                        val result = DES3CBCUtil.decryptRSA(res.data)
                        val bean = Gson().fromJson(result, PersonList::class.java)
                        DishesDBHelper.getInstance().insertPersons(bean.list)
                        if (currentPage >= bean.totalPage) {
                            finish = true
                            mv.encode(Constant.PERSONINFO_TIME, System.currentTimeMillis())
                        } else {
                            currentPage = bean.page + 1
                        }
                    } else {
                        LogUtil.e(TAG, "人员下载错误 ${res.msg}")
                        failTime++
                    }
                } catch (e: Exception) {
                    failTime++
                    LogUtil.e(TAG, "error ${e.message}")
                }
            } while (!finish && (failTime < 10))
            LogUtil.d(TAG, "全量更新人员完成")
            withContext(Dispatchers.Main) {
                awaitingDialog.dismiss()
                if (failTime != 0) ToastShowUtil.show("同步失败，请重试！")
                binding.tvPeopleCount.text =
                    "同步人数：${DishesDBHelper.getInstance().getPersonsCount()}"
            }
        }
    }

    override fun onDestroy() {
        if (this::confirmDialog.isInitialized) confirmDialog.cancel()
        if (this::awaitingDialog.isInitialized) awaitingDialog.cancel()
        if ((saveCheck == kv.decodeBool(Constant.QUERY_VERIFY, false)).not() || (self_help == kv.decodeBool(Constant.BALANCE_SWITCH, false)).not()) {
            val restartIntent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
            startActivity(restartIntent)
            exitProcess(0)
        }
        super.onDestroy()
    }
}