package com.yannuo.dgcanteen.activitys

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import com.proembed.service.MyService
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.presenters.SelfHelpPresenter
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.ActivityBalanceBinding
import com.yannuo.dgcanteen.dialogView.PasswordDialog
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.model.BalanceResponse
import com.yannuo.dgcanteen.model.CallBackBean
import com.yannuo.dgcanteen.model.UserInfoBean
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.views.CardDrawable
import com.yannuo.dgcanteen.views.LoadingDialog2

class BalanceActivity :BaseActivity<ActivityBalanceBinding>(), CallbackListener {
    private lateinit var mCardDrawable : CardDrawable
    private lateinit var mPrensenter :SelfHelpPresenter
    private var mLoading : LoadingDialog2 ?= null
    private lateinit var spanWatcher : SpannableStringBuilder
    private var mXService: MyService? = null
    private var passwordDialog: PasswordDialog?= null
    private lateinit var mHandler: Handler

    override fun bindLayout() {
        binding = ActivityBalanceBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initObject()
        initView()
        initEvent()
        binding.ivDh.setImageDrawable( mCardDrawable)
//        binding.ivDh.setBackgroundDrawable(CardDrawable(this))
    }

    private fun initView() {

        binding.serialNumber.text = "${CommonAndDpToPxUtil.getDeviceSerial()}  v${packageManager.getPackageInfo(packageName, 0).versionName}"
        clearContent()
    }

    private fun initObject() {
        mPrensenter = SelfHelpPresenter()
        mPrensenter.listener = this
        mPrensenter.openIcCard()
        mCardDrawable = CardDrawable(this)
        mLoading = LoadingDialog2(this)
        spanWatcher = SpannableStringBuilder()
        mXService = MyService(this)
        passwordDialog = PasswordDialog(this)
        mHandler = Handler()
    }

    private fun initEvent() {

        binding.ibtBack.setOnClickListener {
            mXService?.hideNavBar = false
            finish()
        }

        binding.tvSetting.setOnClickListener {
            passwordDialog?.apply {
                show()
                binding.tvBack.text = "输入密码"
                setListener(object : CloseEvent {
                    override fun onEvent(code: Int, msg: String?) {
                        startActivity(Intent(this@BalanceActivity, SettingActivity::class.java))
                    }
                })
            }
//            val intent = Intent(this@BalanceActivity, SettingActivity::class.java)
//            startActivity(intent)
        }
        binding.tvClear.setOnClickListener {
            clearContent()
        }
    }


    override fun onResume() {
        super.onResume()
        mXService?.hideNavBar = true
    }
    override fun onDestroy() {
        mCardDrawable.release()
        mPrensenter.closeIcCard()
        mPrensenter.listener = null
        mLoading?.cancel()
        super.onDestroy()
    }


    private fun updateData(rs: BalanceResponse, ps: UserInfoBean) {
        spanWatcher.clear()
        spanWatcher.clearSpans()
        val foregroundColorSpan = ForegroundColorSpan(Color.BLACK)
//        val ps = DishesDBHelper.getInstance().queryPersonToCustId(rs.CUST_ID.trim())

        spanWatcher.append("姓名：${ps.personName}")
        spanWatcher.setSpan(foregroundColorSpan,3,spanWatcher.length,Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvName.text = spanWatcher
        spanWatcher.clear()
        spanWatcher.append("工号/学号：${ps.personNumber}")
        spanWatcher.setSpan(foregroundColorSpan,6,spanWatcher.length,Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvStudentNumber.text = spanWatcher
        spanWatcher.clear()
        spanWatcher.append("年级：${ps.grade}")
        spanWatcher.setSpan(foregroundColorSpan,3,spanWatcher.length,Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvGlass.text = spanWatcher
        spanWatcher.clear()
        spanWatcher.append("卡号：${ps.cardId}")
        spanWatcher.setSpan(foregroundColorSpan,3,spanWatcher.length,Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvCardNumber.text = spanWatcher

//        var foregroundColorSpan2 :ForegroundColorSpan ?= null
//        val foregroundColorSpan3 = AbsoluteSizeSpan(70)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            foregroundColorSpan2 = ForegroundColorSpan(resources.getColor(R.color.sky_color,null))
//        }

        var totalMoney = 0.0f
        spanWatcher.clear()

        rs.ACC_DATA.forEachIndexed { idx, ad ->
            val type = when(ad.ACC_TYPE){
                "01" ->"现金账户"
                "02" ->"餐补账户1"
                "03" ->"餐补账户2"
                "04" ->"餐补账户3"
                "05" ->"餐补账户4"
                "06" ->"餐补账户5"
                else -> "其他虚拟账户"
            }
            disposalData(spanWatcher,"账户类型：$type")
            spanWatcher.appendLine()
            disposalData(spanWatcher,"账户号码：${ad.ACC_NO}")
            spanWatcher.appendLine()
            disposalData(spanWatcher,"账户余额：￥${ad.MONEY}")
            spanWatcher.appendLine()
            if (idx != rs.ACC_DATA.size -1)
                spanWatcher.appendLine()
            totalMoney += ad.MONEY.toFloat()
        }
        binding.tvAccNo.text = spanWatcher

        binding.tvMoneyTotal.text = "￥${totalMoney}"
        CommonAndDpToPxUtil.speakWork("您的总余额为${totalMoney}元")
        mHandler.removeCallbacksAndMessages(null)
        mHandler.postDelayed(Runnable {
            clearContent()
        },1000*10)

    }


    private fun disposalData(span : SpannableStringBuilder ,content :String){
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ForegroundColorSpan(resources.getColor(R.color.sky_color,null))
        } else {
            return
        }
        val start = span.length + content.lastIndexOf("：") + 1
        span.append(content)
//        LogUtil.d(TAG,"start:$start , span.length:${span.length}")
        span.setSpan(style,start,span.length,Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

    }

    private fun showErrorMsg(content: String){
        spanWatcher.clear()
        spanWatcher.clearSpans()
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ForegroundColorSpan(Color.RED)
        } else {
            return
        }
        spanWatcher.append(content)
        spanWatcher.setSpan(style,0,spanWatcher.length,Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        binding.tvAccNo.text = spanWatcher
    }

    private fun clearContent(){
        binding.tvName.text = "姓名："
        binding.tvStudentNumber.text = "工号/学号："
        binding.tvGlass.text = "年级："
        binding.tvCardNumber.text = "卡号："
        binding.tvMoneyTotal.text = ""
        binding.tvAccNo.text = ""
    }


    override fun onOtherListener(e: Int, any: Any?) {
        runOnUiThread {
            if (e == 10)  mLoading?.show()
            else mLoading?.cancel()
            when(e){
                10 ->{
                    clearContent()
                }
                20 ->{
                    any?.also {
                        showErrorMsg(it as String)
                    }
                    mPrensenter.cardStatus = SelfHelpPresenter.CardStatus.VALID
                }
                0->{
                    any?.also {
                        (it as? CallBackBean)?.also {
                            updateData(it.any1 as BalanceResponse,it.any2 as UserInfoBean)
                        }
                    }
                    mPrensenter.cardStatus = SelfHelpPresenter.CardStatus.VALID
                }
            }

        }
    }

}












