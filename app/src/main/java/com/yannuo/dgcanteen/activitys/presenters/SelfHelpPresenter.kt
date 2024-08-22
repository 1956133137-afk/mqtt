package com.yannuo.dgcanteen.activitys.presenters

import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.SerialPortHelper
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.OnReadDataListener
import com.yannuo.dgcanteen.model.CallBackBean
import com.yannuo.dgcanteen.model.CardUserRequest
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.util.CanteenEncryptionUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.*

class SelfHelpPresenter : OnReadDataListener {
    private val TAG = javaClass.simpleName
    private var kv: MMKV
    @Volatile
    private var mPayCfg: PayCfg ?= null
    private lateinit var mCardHandle: SerialPortHelper
    var cardStatus = CardStatus.INVALID
    private lateinit var mRespository: PayRepositoryOfPay
    private var exceptionHandler :CoroutineExceptionHandler
    private var mScope : CoroutineScope
    var listener: CallbackListener? = null
    //支付状态
    enum class CardStatus {
        INVALID, VALID
    }

    init {
        kv = MMKV.defaultMMKV()
        mCardHandle = SerialPortHelper()
        mRespository = PayRepositoryOfPay() //网络请求
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
        exceptionHandler =  CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG,"协程异常： $throwable ${throwable.printStackTrace()}")
            error("协程异常： ${throwable} ")

        }
        mScope = CoroutineScope(Dispatchers.IO + exceptionHandler)
    }



    /**
     * 打开IC卡串口
     */
    fun openIcCard() {
        cardStatus = CardStatus.VALID
        mCardHandle.readDataListener = this
        mCardHandle.openSerialPort("/dev/ttyS4")
    }

    /**
     * 关闭IC卡串口
     */
    fun closeIcCard() {
        listener = null
        cardStatus = CardStatus.INVALID
        if (this::mCardHandle.isInitialized) {
            mCardHandle.readDataListener = null
            mCardHandle.closeSerialPort()
        }
    }

    override fun numberOfIcCard(number: String?) {
        number?.trim()?.also {
            if (cardStatus == CardStatus.INVALID) return@also
            cardStatus = CardStatus.INVALID
//            LogUtil.d(TAG, "number :${it.uppercase()}")
            icCardMsgHandler(it.uppercase())
        }

    }

    private fun icCardMsgHandler(uppercase: String) {
        mScope.launch {
            listener?.onOtherListener(10, true)
            if (mPayCfg == null){
                mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
                if (mPayCfg == null) {
                    error("设备对应商户信息为空")
                    return@launch
                }
            }
           val person = mRespository.queryPerson(CardUserRequest(mPayCfg?.campusId!!,uppercase))
           if (person.code != "200"){
               error(person.msg)
               return@launch
           }
//            val persons = DishesDBHelper.getInstance().queryPersonToCardId(uppercase)
            if (person.data == null){
                error("该卡人员为空,请联系后台管理员")
                return@launch
            }
            val prikey = CanteenEncryptionUtil.encryption("CUST_ID=${person.data!!.custId}&ACC_NO=")
            val hashMap = HashMap<String,String>()
            hashMap.put("CCB_IBSVersion","V6")
            hashMap.put("PT_STYLE","8")
            hashMap.put("PT_LANGUAGE","CN")
            hashMap.put("CAMPUS_ID", mPayCfg?.campusId!!)
            hashMap.put("TXCODE","VIAC05")
            hashMap.put("CORP_ID",mPayCfg!!.corp_id!!)
            hashMap.put("ccbSafeParam",prikey)

            val res = mRespository.ccbPersonBanlance(hashMap)
            if (res.code != "200"){
                error(res.msg)
                return@launch
            }
            when(res.data!!.RESULT){
                "Y"->{
                   val bean = CallBackBean().apply {
                       any1 = res.data
                       any2 = person.data
                   }

                  listener?.onOtherListener(0,bean)
                }
                "N" ->{
                    error("${res.data?.ERRCODE} ${res.data?.ERRMSG}")
                }
            }
        }
    }


    private fun error(msg :String?){
        listener?.onOtherListener(20, msg)
        LogUtil.e(TAG,msg)
    }

    fun release(){
        mScope.cancel()
    }
}