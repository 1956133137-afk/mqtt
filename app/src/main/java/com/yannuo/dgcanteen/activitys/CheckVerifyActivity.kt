package com.yannuo.dgcanteen.activitys

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.proembed.service.MyService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.adapters.VerifyQueryAdapter
import com.yannuo.dgcanteen.databinding.ActivityCheckVerifyBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.FaceResult
import com.yannuo.dgcanteen.model.VerificationUI
import com.yannuo.dgcanteen.util.*
import com.yannuo.dgcanteen.views.CardDrawable
import com.yannuo.dgcanteen.views.LoadingDialog2

class CheckVerifyActivity : BaseActivity<ActivityCheckVerifyBinding>(), CallbackListener {
    private lateinit var mCardDrawable: CardDrawable
    private var mLoading: LoadingDialog2? = null
    private lateinit var spanWatcher: SpannableStringBuilder
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog? = null
    private lateinit var mHandler: Handler
    private var lastTime = 0L  //上次触发时间
    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val verificationVM by lazy { ViewModelProvider(this)[VerificationVM::class.java] }
    private val verifyQueryAdapter by lazy { VerifyQueryAdapter(this) }

    private var mFacePayService: ZHSTFacePayService? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, "onServiceConnected")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.d(TAG, " onServiceDisconnected")
        }
    }

    override fun bindLayout() {
        binding = ActivityCheckVerifyBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initView()
        initEvent()
        binding.ivDh.setImageDrawable(mCardDrawable)
    }

    private fun initObject() {
        initOpear()
        mCardDrawable = CardDrawable(this)
        mLoading = LoadingDialog2(this)
        spanWatcher = SpannableStringBuilder()
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)
        mHandler = Handler(Looper.getMainLooper())
    }

    private fun initOpear() {
        verificationVM.openQrCode()
        verificationVM.setListener(this)
        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        bindService(lIntent, mServiceConnection, BIND_AUTO_CREATE)
    }

    private fun initView() {
        binding.verifyDishView.layoutManager = LinearLayoutManager(this)
        binding.verifyDishView.adapter = verifyQueryAdapter
        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}  v${packageManager.getPackageInfo(packageName, 0).versionName}"
        clearContent()
    }

    private fun initEvent() {
        binding.ibtBack.setOnClickListener {
            mXService?.hideNavBar = false
            mmkv.encode(Constant.QUERY_VERIFY, false)
            finish()
        }

        binding.btnFaceCheck.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            faceVerification()
        }

        binding.btnClear.setOnClickListener {
            if ((System.currentTimeMillis() - lastTime) < 2000) return@setOnClickListener
            lastTime = System.currentTimeMillis()
            clearContent()
        }

        binding.tvSetting.setOnClickListener {
            passwordDialog?.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@CheckVerifyActivity, SettingActivity::class.java))
                    }
                })
            }
        }
    }

    private fun faceVerification() {
        LogUtil.d(TAG, "查询人脸信息~")
        var offline = 0  //在线
        if (mmkv.decodeBool(Constant.SWITCH)) offline = 1  //离线
        val mPayCfg = verificationVM.getPayCfg()
        val campusId = if (mPayCfg == null) "" else mPayCfg.campusId
        val businessId = if (mPayCfg == null) "" else mPayCfg.businessId
        val sn = Utils.getSN()
        mFacePayService?.startFacePay(
            null,
            offline.toString(),
            object : PayResultListener.Stub() {
                override fun onResult(result: String?) {
                    LogUtil.i(TAG, result)
                    val res = Gson().fromJson(result, FaceResult::class.java)
                    if (res.RESULT == "Y") {
                        verificationVM.verification(campusId, businessId, res.CUST_ID, null, sn, null, 1)
                    } else {
                        mHandler.postDelayed({
                            val verificationUI = VerificationUI().apply {
                                errorMsg = res.ERRMSG
                                time = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())
                            }
                            updateData(verificationUI)
                        }, 300)
                    }
                }
            }
        )
    }

    private fun updateData(verify: VerificationUI) {
        spanWatcher.clear()
        spanWatcher.clearSpans()
        val foregroundColorSpan = ForegroundColorSpan(Color.BLACK)

        spanWatcher.append("用户姓名：${verify.personName}")
        spanWatcher.setSpan(foregroundColorSpan, 5, spanWatcher.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvName.text = spanWatcher
        spanWatcher.clear()
        spanWatcher.append("用户编号：")
        spanWatcher.setSpan(foregroundColorSpan, 5, spanWatcher.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvStudentNumber.text = spanWatcher
        spanWatcher.clear()
        spanWatcher.append("年级：")
        spanWatcher.setSpan(foregroundColorSpan, 3, spanWatcher.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvGlass.text = spanWatcher
        spanWatcher.clear()
        spanWatcher.append("卡号：")
        spanWatcher.setSpan(foregroundColorSpan, 3, spanWatcher.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvCardNumber.text = spanWatcher
        spanWatcher.clear()
        if (verify.errorMsg != "核销成功") {
            verifyQueryAdapter.clear()
            disposalData(spanWatcher, "查询失败：\n${verify.errorMsg}")
            binding.tvAccNo.visibility = View.VISIBLE
            binding.tvAccNo.text = spanWatcher
        } else {
            binding.tvAccNo.visibility = View.GONE
//            val dish = Gson().toJson(verify.dishesList).replace("\\[|\\]|\"".toRegex(), "").replace(",","\n")
//            val window = Gson().toJson(verify.windows).replace("\\[|\\]|\"".toRegex(), "").replace(",","\n")
//            disposalData(spanWatcher, "待核销的菜品：\n${dish}")
//            disposalData(spanWatcher, "\n待核销窗口：\n${window}")
//            binding.tvAccNo.text = spanWatcher
            LogUtil.d(TAG, Gson().toJson(verify.verify))
            verifyQueryAdapter.data = verify.verify
        }
        mHandler.removeCallbacksAndMessages(null)
        mHandler.postDelayed({
            clearContent()
            verifyQueryAdapter.clear()
        }, 1000 * 10)

    }


    private fun disposalData(span: SpannableStringBuilder, content: String) {
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ForegroundColorSpan(resources.getColor(R.color.sky_color, null))
        } else {
            return
        }
        val start = span.length + content.indexOf("：") + 1
        span.append(content)
//        LogUtil.d(TAG,"start:$start , span.length:${span.length}")
        span.setSpan(style, start, span.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

    }

    private fun clearContent() {
        binding.tvName.text = "姓名："
        binding.tvStudentNumber.text = "工号/学号："
        binding.tvGlass.text = "年级："
        binding.tvCardNumber.text = "卡号："
        binding.tvAccNo.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        mCardDrawable.release()
        mLoading?.cancel()
    }

    override fun onOtherListener(event: Int, any: Any?) {
        //刷脸结果返回
        runOnUiThread {
            when (event) {
                0 -> {
                    LogUtil.d(TAG, "查询成功")
                    val verificationUI = any as VerificationUI
                    updateData(verificationUI)
                }

                10 -> {
                    LogUtil.d(TAG, "查询失败")
                    val verificationUI = any as VerificationUI
                    updateData(verificationUI)
                }
            }
        }
    }

}