package com.yannuo.dgcanteen.activitys.viewModel

import android.content.SharedPreferences
import android.hardware.Camera.FaceDetectionListener
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.proembed.service.LogUtils
import com.safframework.log.converter.gson.GsonUtils
import com.safframework.log.converter.gson.GsonUtils.fromJson
import com.safframework.log.converter.gson.GsonUtils.toJson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.ScanDevice
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.CardUserRequest
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.MqttAddFaceCallback
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.VerificationRequest
import com.yannuo.dgcanteen.model.VerificationResponse
import com.yannuo.dgcanteen.util.CanteenEncryptionUtil
import com.yannuo.dgcanteen.util.CanteenEncryptionUtil.getAnalysisCode
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.util.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.text.StringBuilder

class VerificationVM : ViewModel(), ScanDevice.DataCallBack, OnReadDataListener {
    var showToastEvent: MutableLiveData<String>
    var loadingEvent: MutableLiveData<Boolean>
    private val TAG = javaClass.simpleName
    private val mRespository by lazy {
        PayRepositoryOfPay()
    }
    private var mPayCfg: PayCfg? = null
    private var exceptionHandler: CoroutineExceptionHandler
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    private lateinit var mCodeDevice: ScanDevice
    private lateinit var mCardDevice: SerialPortHelper
    private var codeStatus = CodeStatus.INVALID
    private var cardStatus = CardStatus.INVALID
    enum class CodeStatus {
        INVALID, PAY
    }

    enum class CardStatus {
        INVALID, PAY
    }

    private val callBackListener: CallbackListener? = null

    init {
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
        val sn = Utils.getSN()

        if (mPayCfg == null || TextUtils.isEmpty(mPayCfg!!.campusId) ||
            TextUtils.isEmpty(mPayCfg!!.businessId) || TextUtils.isEmpty(mPayCfg!!.counterId)
        ) {
            LogUtil.e(TAG, "未配置支付环境")
            throw Throwable("未配置支付环境")
        }
        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()
        mCodeDevice = ScanDevice()
        mCardDevice = SerialPortHelper()
        exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG, "协程异常： $throwable ${throwable.stackTraceToString()}")
            showToastEvent.postValue("错误： ${throwable.message}")
            loadingEvent.postValue(false)
        }
    }

    //打开扫码,刷卡
    fun openQrCode() {
        codeStatus = CodeStatus.PAY
        cardStatus = CardStatus.PAY
        mCodeDevice.openScan()
        mCardDevice.openSerialPort("/dev/ttyS4", 9600)
        mCodeDevice.setCallbackListener(this)
        mCardDevice.readDataListener = this
    }

    //修改支付状态
    fun setPayStatus() {
        codeStatus = CodeStatus.PAY
        cardStatus = CardStatus.PAY
    }

    //关闭扫码,刷卡
    fun closeQrCode() {
        codeStatus = CodeStatus.INVALID
        cardStatus = CardStatus.INVALID
        if (this::mCodeDevice.isInitialized) {
            mCodeDevice.setCallbackListener(null)
            mCodeDevice.closeScan()
        }
        if (this::mCardDevice.isInitialized) {
            mCardDevice.readDataListener = null
            mCardDevice.closeSerialPort()
        }
    }

    fun getPayCfg(): PayCfg? {
        return mPayCfg
    }

    /**
     * 订餐核销
     */
    fun verification(
        campusId: String?,
        businessId: String?,
        custId: String?,
        orderId: String?,
        deviceId: String?,
        cardId: String?,
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            val data =
                CanteenEncryptionUtil.encryption("CAMPUS_ID=${campusId}&BUSINESS_ID=${businessId}&CUST_ID=${custId}&ORDER_ID=${orderId}&DEVICE_ID=${deviceId}&CARD_ID=${cardId}")
            LogUtil.d(TAG, "加密数据: $data")
            var verification = VerificationRequest().apply {
                this.dcEncryptParam = data
            }
            LogUtil.d(TAG, Gson().toJson(verification))
            val ccbCodeVerification = mRespository.getCcbCodeVerification(verification)
            LogUtil.d(TAG, Gson().toJson(ccbCodeVerification))
            if (ccbCodeVerification.code == 200) {
                val toJson = Gson().toJson(ccbCodeVerification.data)
                LogUtil.d(TAG, toJson)
                val json = Gson().fromJson(toJson, VerificationResponse::class.java)
                val verifyDishes = json.verifyDishes
                val sb = StringBuilder()
                for (dish in verifyDishes) {
                    sb.append("$dish,")
                }
                LogUtil.i(TAG, "核销的菜品: $sb")
                if (json.unVerifyWindowName.isNotEmpty()) {
                    sb.append("未核销的菜品请前往")
                    for (window in json.unVerifyWindowName) {
                        sb.append(window)
                    }
                }
                CommonAndDpToPxUtil.speakWork(sb.toString())
                backToHome(verifyDishes)
            } else {
                LogUtil.w(TAG, "${ccbCodeVerification.msg}")
                CommonAndDpToPxUtil.speakWork(ccbCodeVerification.msg)
                backToHome(null)
            }
        }
    }

    //返回相应的初始界面
    private fun backToHome(dishes: Array<String>?) {
        when (kv.decodeString(Constant.APP_MODE)) {
            Constant.ORDERING_FOOD_MODE -> {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFICATION, dishes))
            }

            Constant.PROCEEDS_MODE -> {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_THIRTY, dishes))
            }

            Constant.ORDERING_TWO_MODE -> {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_THIRTY_ONE, dishes))
            }
        }
    }

    override fun onData(data: String) {
        if (codeStatus == CodeStatus.INVALID) return
        codeStatus = CodeStatus.INVALID
        val qrData = data.trim().replace("\r\n", "")
        if (qrData.contains("CT005")) {
            CommonAndDpToPxUtil.speakWork("请点击刷脸，展示核销码")
            return
        }
        val info = getAnalysisCode(qrData)
        Log.d(TAG, "onData: $info")
        verification(
            info["CAMPUS_ID"],
            info["BUSINESS_ID"],
            info["CUST_ID"],
            info["ORDER_ID"],
            info["DEVICE_ID"],
            info["CARD_ID"]
        )
    }

    override fun numberOfIcCard(number: String?) {
        if (cardStatus == CardStatus.INVALID) return
        cardStatus = CardStatus.INVALID
        val cardData = number?.trim()?.replace("\r\n", "")
        LogUtil.d(TAG, "卡号：$cardData")
        val campusId = if (mPayCfg == null) "" else mPayCfg!!.campusId
        val businessId = if (mPayCfg == null) "" else mPayCfg!!.businessId
        val sn = Utils.getSN()
        verification(campusId, businessId, null, null, sn, cardData)
    }

    private fun error(msg: String?) {
        callBackListener?.onOtherListener(20, msg)
        LogUtil.e(TAG, msg)
    }

}