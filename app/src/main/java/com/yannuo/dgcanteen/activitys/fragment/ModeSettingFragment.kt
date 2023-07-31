package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.FragmentModeSettingBinding
import com.yannuo.dgcanteen.dialogView.AwaitingDialog
import com.yannuo.dgcanteen.dialogView.ConfirmDialog
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PersonList
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.DES3CBCUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.LoadingDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.util.*
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
    private lateinit var mScope: CoroutineScope
    private lateinit var mHandle: CoroutineExceptionHandler
    private lateinit var confirmDialog: ConfirmDialog
    private lateinit var awaitingDialog: AwaitingDialog
    private val mContext = MyApplication.applicationContext

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentModeSettingBinding.inflate(inflater, container, false)
        initObject()
        initEvent()
        initData()
        return binding.root
    }

    private fun initObject() {
        kv = MMKV.defaultMMKV()
        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "CoroutineExceptionHandler $e ${e.message}")
        }
        mScope = CoroutineScope(Dispatchers.Default + mHandle)
    }

    private fun initEvent() {
        binding.appMode.setOnClickListener { //切换模式
            if (!this::confirmDialog.isInitialized)
                confirmDialog = ConfirmDialog(requireActivity())
            changeMode()
        }
        binding.switchFixed.setOnClickListener { //定额模式
            kv.encode(Constant.QUOTA_SWITCH, binding.switchFixed.isChecked)
            amountJudgment(binding.fixedSum.text.toString())
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_QUOTA_CHANGE, null))
        }
        binding.btnSynPerson.setOnClickListener { view: View? ->
            if (!this::awaitingDialog.isInitialized)
                awaitingDialog = AwaitingDialog(requireActivity())
            awaitingDialog.show()
            awaitingDialog.updateText("同步中")
            downPerson()
            ToastShowUtil.show("人员信息已同步~")
        }
    }

    private fun initData() {
        reload()
    }

    fun save() {
        amountJudgment(binding.fixedSum.text.toString())
    }

    private fun reload() {
        binding.switchFixed.isChecked = kv.decodeBool(Constant.QUOTA_SWITCH, false)
        binding.fixedSum.setText(kv.decodeString(Constant.QUOTA_AMOUNT, "0.00"))
        binding.appMode.text = kv.decodeString(Constant.APP_MODE)
        binding.tvFinalTime.text = kv.decodeString(Constant.FINAL_TIME)
    }

    private fun amountJudgment(str: String) {
        val amount = String.format(Locale.CHINA, "%.02f", str.toFloat())
        if (isFormJudgment(amount)) {
            kv.encode(Constant.QUOTA_AMOUNT, amount)
            mScope.launch {
                withContext(Dispatchers.Main) {
                    binding.fixedSum.setText(kv.decodeString(Constant.QUOTA_AMOUNT))
                }
            }
            ToastShowUtil.show("保存成功: ${mContext.filesDir.absolutePath}/mmkv")
        } else {
            binding.switchFixed.isChecked = false
            ToastShowUtil.show("输入金额有误")
        }
    }

    private fun isFormJudgment(str: String): Boolean { //判断格式
        val regex = Regex(
            """^(0|[1-9]\d{0,5})(\.\d{0,2})?$""",
            RegexOption.IGNORE_CASE
        )
        return regex.matches(str)
    }

    private fun changeMode() {
        confirmDialog.apply {
            show()
            val strText = when (kv.decodeString(Constant.APP_MODE)) {
                Constant.ORDERING_FOOD_MODE -> "是否切换为 ${Constant.PROCEEDS_MODE} 并且重启应用？"
                Constant.PROCEEDS_MODE -> "是否切换为 ${Constant.ORDERING_FOOD_MODE} 并且重启应用？"
                else -> "是否切换为  并且重启应用？"
            }
            val str = SpannableString(strText)
            str.setSpan(
                StyleSpan(Typeface.BOLD),
                5,
                strText.length - 7,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvText.text = str

            setListener(object : ConfirmDialog.OnConfirmCallback {
                override fun confirmCallback(flag: Boolean) {
                    if (flag) {
                        when (kv.decodeString(Constant.APP_MODE)) {
                            Constant.ORDERING_FOOD_MODE -> kv.encode(
                                Constant.APP_MODE,
                                Constant.PROCEEDS_MODE
                            )
                            Constant.PROCEEDS_MODE -> kv.encode(
                                Constant.APP_MODE,
                                Constant.ORDERING_FOOD_MODE
                            )
                        }
                        val restartIntent =
                            mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
                        restartIntent?.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
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
            val prvKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIGRJ0RqOaaYrem6zmTo" +
                    "/SF2OROMcJwRws/b05kaG0N90ZKFdRucIuiWvCiU4y9LLD6yNaCIyDGH0VubFOGnwzF7BqGR" +
                    "4LTJgCHtfYodkE8XA99/P/cT/gi38uoX+UBnjxR2WeJPhHEr59tvVejb93KJQPMnhs7wJnxX" +
                    "YycTmJuLAgMBAAECgYAvoMcZfB7jIb7Ua2oRaCAc29ORXw/KHzFIrVs0LYeWILsYLFznIFco" +
                    "vrg+BrUYnn6OMX5LG9zTcETCctiTNtMmaBwG6J41GNWdwwJDdTmhjXs/jh6q1Wp3oT4jzlJW" +
                    "DozrwTWnzxFg/zoywntSFd46xzlt0YIXxpSQR8e0WxktYQJBAME/MTnp8csABoZ/OGhIS4qb" +
                    "xlVayHS+H8qCOU1mdb/aYDoiqf94LYpebqkCwcerjhz02ZX9xwqWTA2TUib8p1ECQQCrpDDi" +
                    "/M+mU2f63qs66usmLCIeJpaD56AeYIm5TdVC7PI6ZOx/FsBxJGkk1b6pmrOEAKQ7lFhMoq4U" +
                    "Z2I28hYbAkAqQF/J8s2L/ehvVbeGjW/+0UpO9Tdo1vzqcQiIVMOf++YYL+YNVkBWxYjaaSDn" +
                    "QColSJ+ePMtdFDlyqmhG3+zRAkAghHq+hibQ2/xXCthl0Ru7n6DXFXhuhPNQzflJofVFOJ6r" +
                    "cXNcoHLU/JDu6Y+1khlwaK60muYfnrJcKznwLu0BAkAKhJcHprKRRCJpT//A169jrbfuX1B6" +
                    "mFcOGXwPzO2s1JYzUlXCU4ylOVrLmdOpV+e7OSkrKNihVeIUm+TJt4MK"
            val mv = MMKV.defaultMMKV()
            val repository = PayRepositoryOfPay()
            var finish = false
            var currentPage = 1
            var failTime = 0
            LogUtil.d(TAG, "准备全量更新人员")
            do {
                val res = repository.downPerson(100, currentPage)
                try {
                    if (res.code == 200) {
                        val result = DES3CBCUtil.decryptRSA(res.data, prvKey)
                        val bean = Gson().fromJson(result, PersonList::class.java)
                        DishesDBHelper.getInstance().insertPersons(bean.list)
                        if (currentPage == bean.totalPage) {
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
                if (!finish)
                    ToastShowUtil.show("同步失败，请重试！")
            }
        }
    }

    override fun onDestroy() {
        if (this::confirmDialog.isInitialized) confirmDialog.cancel()
        if (this::awaitingDialog.isInitialized) awaitingDialog.cancel()
        super.onDestroy()
    }
}