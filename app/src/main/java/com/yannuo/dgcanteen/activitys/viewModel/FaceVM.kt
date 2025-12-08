package com.yannuo.dgcanteen.activitys.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.MyBitmapUtil
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.AccListTable
import com.yannuo.dgcanteen.greendao.entity.OfflineOrderTable
import com.yannuo.dgcanteen.greendao.entity.PayDishTable
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.greendao.entity.UserFaceData
import com.yannuo.dgcanteen.model.ACCLIST
import com.yannuo.dgcanteen.model.EncryptedDataRequest
import com.yannuo.dgcanteen.model.FacePayRequest
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.ResponsePay
import com.yannuo.dgcanteen.model.UploadFaceImageRequest
import com.yannuo.dgcanteen.model.UploadFaceRequest
import com.yannuo.dgcanteen.networkstate.NetworkStateManager
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.DES3CBCUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.util.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Random

class FaceVM: ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private var listener: OnListener? = null
    private var payment: String = "0"
    private val dbHelper = DishesDBHelper.getInstance()
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()

    var isPayStatus = false
    private val mutex: Mutex = Mutex()
    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
    }

    fun setListener(listener: OnListener, payment: String = "0"){
        this.listener = listener
        this.payment = payment
    }

    fun getPayment(): String{
        return this.payment
    }

    /**
     * 上传人脸特征值
     */
    fun uploadFaceToken(persons: Persons, eigenvalue: String){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
            if(payCfg == null){
                ToastShowUtil.show("未获取到配置信息")
                return@launch
            }
            val bean = UploadFaceRequest().apply {
                this.userId = persons.userId
                this.custId = persons.custId
                this.campusId = payCfg.campusId
                this.eigenvalue = eigenvalue
                this.version = "1.1"
            }
            Log.d(TAG, "uploadFaceToken: 未加密上传数据：${Gson().toJson(bean)}")
            val encryption = DES3CBCUtil.encryption(Gson().toJson(bean))
            val data = mRepository.uploadUserEigenvalue(EncryptedDataRequest(encryption))
            if(data.code == "200"){
                //解密
                val decryptRSA = DES3CBCUtil.decryptRSA(data.data)
                val fromJson = Gson().fromJson(decryptRSA, UserFaceData::class.java)
                //更新人员信息
                DishesDBHelper.getInstance().insertFaceData(fromJson)
                listener?.uploadResult(2,true, "上传成功")
            }else{
                LogUtil.e(TAG,"人脸特征值上传异常：${data.code} - ${data.msg}")
                listener?.uploadResult(2,false, data.msg)
            }
        }
    }

    /**
     * 上传人脸图片
     */
    fun uploadFaceImage(persons: Persons, img: String){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
            if(payCfg == null){
                ToastShowUtil.show("未获取到配置信息")
                return@launch
            }
            val bean = UploadFaceImageRequest().apply {
                this.userId = persons.userId
                this.custId = persons.custId
                this.campusId = payCfg.campusId
                this.faceBase64 = img
            }
            Log.d(TAG, "uploadFaceToken: 未加密上传数据：${Gson().toJson(bean)}")
            val encryption = DES3CBCUtil.encryption(Gson().toJson(bean))
            val data = mRepository.uploadFaceDBImg(EncryptedDataRequest(encryption))
            if(data.code == "200"){
                //解密
                val decryptRSA = DES3CBCUtil.decryptRSA(data.data)
                val fromJson = Gson().fromJson(decryptRSA, UserFaceData::class.java)
                //更新人员信息
                DishesDBHelper.getInstance().insertFaceData(fromJson)
                listener?.uploadResult(1,true, "上传成功")
            }else{
                LogUtil.e(TAG,"人脸图片上传异常：${data.code} - ${data.msg}")
                listener?.uploadResult(1,false, data.msg)
            }
        }
    }

    fun localFacePay(token: String, payMoney: String, img: String, searchScore: String){
        val bean = DishesDBHelper.getInstance().queryFaceByEigenvalue(token)
        if(bean == null){
            val payForUI = PayForUI().apply{
                result = "N"
                errCode = "0x000001"
                errMsg = "找不到该人脸信息"
            }
            listener?.facePayResult(payForUI)
            return
        }
        val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
        if(payCfg == null){
            val payForUI = PayForUI().apply{
                result = "N"
                errCode = "0x000001"
                errMsg = "未获取到支付配置信息"
            }
            listener?.facePayResult(payForUI)
            return
        }
        val payBean = FacePayRequest().apply {
            deviceId = CommonAndDpToPxUtil.getDeviceSerial()
            campusId = payCfg.campusId
            businessId = payCfg.businessId
            businessName = payCfg.businessName
            vposId = payCfg.counterId
            payment = payMoney
            actualPayment = payMoney
            offline = if(kv.decodeBool(Constant.SWITCH)) "1" else "0"
            signTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(System.currentTimeMillis())
            personNumber = bean.userId
            custId = bean.custId
//            faceBase64 = CameraUtil.instance.imageUrlToBase64(img)
            faceBase64 = MyBitmapUtil(MyApplication.applicationContext).imagePathToBase64(img)
            faceScore = BigDecimal(searchScore).setScale(2,RoundingMode.DOWN ).toPlainString()
        }
        LogUtil.d(TAG, "eventCalculate: 识别成功生成订单：${Gson().toJson(payBean)}")
        //支付
        isPayStatus = true
        localScanFacePayment(payBean)
    }

    private fun localScanFacePayment(bean: FacePayRequest){
        if (!isPayStatus) {
            viewModelScope.launch(Dispatchers.Main) { ToastShowUtil.show("无效操作") }
            return
        }
        viewModelScope.launch(Dispatchers.IO + mHandler){
            mutex.withLock { isPayStatus = false }
            if (bean.custId.isNullOrEmpty()){
                val payForUI = PayForUI().apply{
                    result = "N"
                    errCode = "0x000001"
                    errMsg = "未查询到人员"
                }
                listener?.facePayResult(payForUI)
                return@launch
            }
            val payForUI = initData(bean.custId!!, bean.payment, bean.offline)
            if(payForUI.offline == "1"){
                //离线支付
                try {
                    val offlineOrder = Gson().fromJson(Gson().toJson(payForUI), OfflineOrderTable::class.java)
                    val user = dbHelper.queryPersonToCustId(payForUI.payContent)
                    if(user == null){
                        payForUI.errCode = "0x000001"
                        payForUI.errMsg = "找不到该人员"
                    }else{
                        offlineOrder.custId = user.custId
                        offlineOrder.username = user.personName
                        dbHelper.insertOfflineOrder(offlineOrder)
                        payForUI.result = "Y"
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    payForUI.errCode = "0x000000"
                    payForUI.errMsg = "离线支付失败"
                }
                listener?.facePayResult(payForUI)
                return@launch
            } else{
                //在线支付
                val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
                if(payCfg == null){
                    Log.d(TAG, "localScanFacePayment: 未获取到支付配置")
                    return@launch
                }
                val encryption = DES3CBCUtil.encryption(Gson().toJson(bean))
                val response = mRepository.localScanFacePayment(EncryptedDataRequest(encryption))
                if(response.code == "200"){
                    //解密
                    var totalBalance = BigDecimal(0.00)
                    val decryptRSA = DES3CBCUtil.decryptRSA(response.data)
                    Log.d(TAG, "localScanFacePayment: 支付数据解密: $decryptRSA")
                    val result = Gson().fromJson(decryptRSA, ResponsePay::class.java)
                    LogUtil.d(TAG, "支付结果: ${Gson().toJson(result)}")
                    payForUI.result = result.RESULT
                    payForUI.accType = result.ACC_TYPE
                    payForUI.accNo = result.ACC_NO
                    payForUI.accBal = result.REMAIN_BAL.ifEmpty { result.ACC_BAL }
                    result.ACC_LIST.forEach {
                        val acclist = ACCLIST().apply {
                            ACC_NO = it.ACC_NO
                            ACC_BAL = it.ACC_BAL
                            ACC_TYPE = it.ACC_TYPE
                            TRAN_ID = it.TRAN_ID
                            PAYMENT = it.PAYMENT
                        }
                        payForUI.accList.add(acclist)
                        totalBalance = totalBalance.add(BigDecimal(it.PAYMENT))
                    }
                    /*查询人员姓名*/
                    if (result.CUST_ID.isNotEmpty() && payForUI.username.isEmpty()) {
                        val persons = dbHelper.queryPersonToCustId(result.CUST_ID)
                        if (persons != null) payForUI.username = persons.personName
                    }
                    payForUI.actualPayment = if(totalBalance.compareTo(BigDecimal(0.00)) == 1) totalBalance.toEngineeringString() else payForUI.actualPayment
                    payForUI.orderId = result.ORDERID
                    payForUI.traceId = result.TRACEID
                    payForUI.errCode = result.ERRCODE
                    payForUI.errMsg = result.ERRMSG
                } else{
                    payForUI.errCode = response.code
                    payForUI.errMsg = response.msg
                }
                if (payForUI.result == "Y") {
                    saveOrderRecord(payForUI, 1)
                }
                LogUtil.d(TAG, Gson().toJson(payForUI))
                listener?.facePayResult(payForUI)
            }
        }
    }

    /**
     * 保存消费记录
     */
    private fun saveOrderRecord(payForUI: PayForUI, flag: Int) {
        val payOrder = Gson().fromJson(Gson().toJson(payForUI), PayOrderTable::class.java)
        payOrder.tranResult = "3" //1：待支付，2：支付失败，3：支付成功
        payOrder.flag = flag
        dbHelper.insertPayOrder(payOrder)
        val order = dbHelper.queryPayOrder(payOrder.orderId)
        payForUI.paymentDishes.forEach {
            val dish = Gson().fromJson(Gson().toJson(it), PayDishTable::class.java)
            dish.payOrderTable = order
            dbHelper.insertPayDish(dish)
        }
        payForUI.accList.forEach {
            val acc = Gson().fromJson(Gson().toJson(it), AccListTable::class.java)
            acc.payOrderTable = order
            dbHelper.insertAccList(acc)
        }
    }

    /**
     * 初始化消费数据
     */
    private fun initData(content: String, payAmount: String, offline: String): PayForUI {
        val mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
        val currentTime = System.currentTimeMillis()
        val deviceSerial = Utils.getSN()
        val payForUI = PayForUI().apply {
            businessId = mPayCfg.businessId
            businessName = mPayCfg.businessName
            campusId = mPayCfg.campusId
            corpId = mPayCfg.corpId
            vposId = mPayCfg.counterId
            deviceId = deviceSerial
            payType = "3"
            payContent = content
            payment = payAmount
            actualPayment = payAmount
            payTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss", currentTime)
            payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
            sessionId = "$deviceSerial$currentTime${Random().nextInt(10)}"
            signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
            this.offline = offline
        }
        return payForUI
    }

    interface OnListener{
        /**
         * 本地脸库特征值上传结果
         */
        fun uploadResult(code: Int, type: Boolean,msg: String)

        /**
         * 本地脸库刷脸支付结果
         */
        fun facePayResult(payForUI: PayForUI)

    }

}