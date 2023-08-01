package com.yannuo.dgcanteen.activitys.presenters

import android.text.format.DateFormat
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.dao.CardPay
import com.yannuo.dgcanteen.dao.OwnOrder
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.ScanQrResultBean
import com.yannuo.dgcanteen.model.SynConsumeRecordBean
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.*
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: filowl
 * Description: 刷卡支付
 * Date: 2023/8/1 11:22
 **/
class CardPresenter : OnReadDataListener {
    private val TAG = javaClass.simpleName

    private lateinit var mCardHandle: SerialPortHelper
    private lateinit var kv: MMKV
    private lateinit var mPayCfg: PayCfg
    private var payAmount = 0.0F
    var listener: CallbackListener? = null
    private lateinit var mRespository: PayRepositoryOfPay
    private var cardStatus = CardStatus.INVALID

    //支付状态
    enum class CardStatus {
        INVALID, PAY
    }

    fun initCard() {
        kv = MMKV.defaultMMKV()
        mCardHandle = SerialPortHelper()
        mRespository = PayRepositoryOfPay() //网络请求
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)!!
    }

    /**
     * 打开IC卡串口
     */
    fun openIcCard(payment: Float) {
        payAmount = payment
        cardStatus = CardStatus.PAY
        mCardHandle.readDataListener = this
        mCardHandle.openSerialPort("/dev/ttyS4")
//          mCardHandle.openSerialPort("/dev/ttyXRUSB0")
    }

    /**
     * 关闭IC卡串口
     */
    fun closeIcCard() {
        mCardHandle.readDataListener = null
        mCardHandle.closeSerialPort()
    }

    //返回读卡信息
    override fun numberOfIcCard(number: String?) {
        number?.trim()?.also {
            if (cardStatus == CardStatus.INVALID) return@also
            cardStatus = CardStatus.INVALID
            LogUtil.d(TAG, "number :${it.uppercase()}")
            payByCard(it.uppercase())
        }
    }

    private fun payByCard(cardId: String) {
        val payBean = CardPay()
        payBean.campus_id = mPayCfg.campusId
        payBean.corp_id = mPayCfg.corp_id
        payBean.txcode = "PAY005"
        payBean.business_id = mPayCfg.businessId
        payBean.vpos_id = mPayCfg.counterId
        payBean.payment = payAmount.toString()
        payBean.actual_payment = payAmount.toString()
        payBean.offline = "0"
        if (!NetworkStateManager.getInstance().isOnline(MyApplication.applicationContext)
            && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)
        ) {
            //网络监听
            return
        }
        if (kv.decodeBool(Constant.SWITCH)) {
            payBean.offline = "1"
        }
        payBean.sign_time = DateFormat.format("yyyyMMddHHmmss", System.currentTimeMillis()).toString()
        payBean.card_id = cardId
        payBean.order_id = NumberGenerateUtil.getOrderNumber()
        val persons = DishesDBHelper.getInstance().queryPersonToCardId(payBean.card_id)
        if (persons != null) {
            payBean.cust_id = persons.custId
        }

        var res: ScanQrResultBean? = null
        when (payBean.offline) {
            "0" -> {
                runBlocking {
                    val map = CanteenEncryptionUtil.getCardToPay(payBean)
                    res = mRespository.getCcbData(map).body()?.let { //刷卡支付
                        Gson().fromJson(
                            it.string().replace("\r\n", ""),
                            ScanQrResultBean::class.java
                        )
                    }
                    LogUtil.e("ning", Gson().toJson(res))
                    if (res?.RESULT.toString() == "Y") {
                        res?.let { consumeRecord(payBean, it) }
                    }
                    if (persons != null) {
                        listener?.onOtherListener(3, resForUI("刷卡支付", payBean, res))
                    } else {
                        res = ScanQrResultBean("", "用户不存在")
                        listener?.onOtherListener(3, resForUI("刷卡支付", payBean, res))
                    }
                }
            }
            "1" -> {
                if (persons != null) {
                    payBean.up = false
                    DishesDBHelper.getInstance().insertCardOrder(payBean)
                    LogUtil.d(TAG, "离线订单已保存")
                    listener?.onOtherListener(4, resForUI("刷卡支付", payBean, res))
                } else {
                    res = ScanQrResultBean("", "用户不存在")
                    listener?.onOtherListener(3, resForUI("刷卡支付", payBean, res))
                }
            }
        }
    }

    private fun resForUI(type: String, data: CardPay, res: ScanQrResultBean?): PayResultForUI {
        val persons = DishesDBHelper.getInstance().queryPersonToCustId(data.cust_id)
        val payState = PayResultForUI()
        payState.way = type
        payState.orderid = data.order_id
        payState.custId = data.cust_id
        payState.timestamp = data.sign_time
//        payState.dishes = null
//        payState.piece = 0
        payState.cust_name = persons?.personName ?: "***"
        payState.payment = data.payment
        if (res == null || res.RESULT.toString() == "Y") {
            payState.result = PayResultForUI.Result.SUCCESS
            payState.payment = res?.ACTUAL_PAYMENT ?: data.payment
            payState.acc_no = res?.ACC_NO ?: ""
            payState.acc_bal = res?.ACC_BAL ?: ""
        } else {
            payState.result = PayResultForUI.Result.FAIL
            payState.traceid = res.TRACEID
            payState.errormsg = "error ${res.ERRCODE} ${res.ERRMSG} "
        }
        return payState
    }

    //同步刷卡消费记录
    private suspend fun consumeRecord(data: CardPay, res: ScanQrResultBean) {
        val bean = SynConsumeRecordBean()
        bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
        bean.businessId = data.business_id
        bean.counterId = data.vpos_id
        bean.consumptionType = 3
        bean.consumptionType = 2
        bean.RESULT = res.RESULT.toString()
        bean.CUST_ID = data.cust_id
        bean.PAYMENT = res.PAYMENT?.toDouble()
        bean.ACTUAL_PAYMENT = res.ACTUAL_PAYMENT?.toDouble()
        bean.ACC_NO = res.ACC_NO
        bean.ACC_BAL = res.ACC_BAL?.toDouble()
        bean.ACC_TYPE = res.ACC_TYPE?.toInt()
        bean.TRACEID = ""
        bean.ORDER_ID = data.order_id
        bean.TRAN_RESULT = when (res.RESULT.toString()) {
            "Y" -> 3
            "N" -> 2
            else -> null
        }
        bean.OFFLINE = data.offline.toInt()
        bean.ERRCODE = res.ERRCODE
        bean.ERRMSG = res.ERRMSG
        bean.ACCALIAS = when (bean.ACC_TYPE) {
            1 -> "现金账号"
            2 -> "餐补账户"
            else -> ""
        }
        bean.PAYTIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        bean.BUSINESS_NAME = "彦诺智能测试园区"
//        bean.paymentDishesList = mutableListOf()

        val responseScanPay = mRespository.synCsRecord(bean)
        LogUtil.i(TAG, Gson().toJson(responseScanPay))
        if (responseScanPay.code != HttpURLConnection.HTTP_OK) {
            saveFailureRecord(bean)
            LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${responseScanPay.data}")
        } else {
            LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
        }

    }

    //保存上传失败记录
    private fun saveFailureRecord(bean: SynConsumeRecordBean) {
        val saveOrder = OwnOrder()
        bean.apply {
            saveOrder.deviceSerialNumber = deviceSerialNumber
            saveOrder.businessId = businessId
            saveOrder.counterId = counterId
            saveOrder.consumptionType = consumptionType
            saveOrder.result = RESULT
            saveOrder.cusT_ID = CUST_ID
            saveOrder.payment = PAYMENT ?: 0.0

            saveOrder.actuaL_PAYMENT = ACTUAL_PAYMENT ?: 0.0
            saveOrder.acC_NO = ACC_NO
            saveOrder.acC_BAL = ACC_BAL ?: 0.0
            saveOrder.acC_TYPE = ACC_TYPE ?: 1
            saveOrder.traceid = TRACEID
            saveOrder.ordeR_ID = ORDER_ID
            saveOrder.traN_RESULT = TRAN_RESULT ?: 3
            saveOrder.offline = OFFLINE
            saveOrder.errcode = ERRCODE
            saveOrder.errmsg = ERRMSG
            saveOrder.accalias = ACCALIAS
            saveOrder.paytime = PAYTIME
            saveOrder.businesS_NAME = BUSINESS_NAME
        }
        DishesDBHelper.getInstance().insertConsumerOrder(saveOrder)
    }
}