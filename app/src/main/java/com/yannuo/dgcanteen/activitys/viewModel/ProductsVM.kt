package com.yannuo.dgcanteen.activitys.viewModel

import android.os.Build
import android.os.RemoteException
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.DishesDisplay
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.*
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class ProductsVM : ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private val dbHelper = DishesDBHelper.getInstance()

    val showToastEvent: MutableLiveData<String> = MutableLiveData()
    val loadingEvent: MutableLiveData<Boolean> = MutableLiveData()
    val menuChange: MutableLiveData<Int> = MutableLiveData()
    val tab: MutableLiveData<Int> = MutableLiveData()
    val uiData: MutableLiveData<PayForUI> = MutableLiveData()

    private val mRespository: PayRepositoryOfPay = PayRepositoryOfPay()
    private val mPayCfg: PayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    private var mDishesDisplay: DishesDisplay? = null
    var listener: IProductsVM? = null

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "协程异常： $throwable ${throwable.stackTraceToString()}")
        showToastEvent.postValue("错误： ${throwable.message}")
        loadingEvent.postValue(false)
    }

    /**
     * 检查是否已更新菜品
     * @return Boolean
     */
    private fun checkIsNeedUpdate(): Boolean {
        val old = kv.decodeString(Constant.UPDATE_TIME, Constant.update_time)
        val now = DateFormat.format("yyyyMMdd", System.currentTimeMillis()).toString()
        return (old!!.toInt() >= now.toInt())
    }

    fun setDisplay(display: DishesDisplay?) {
        mDishesDisplay = display
    }

    fun getDisplay(): DishesDisplay? {
        return mDishesDisplay
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun upDataDishes(force: Boolean = false) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            val check = checkIsNeedUpdate()
            if (check.not() || force) {
                loadingEvent.postValue(true)
                val rs = mRespository.getDayDishes()
                if (rs.code == "200") {
                    val mealList = mutableListOf<MealTable>()
                    val dishList = mutableListOf<DishesTable>()
                    val picList = mutableListOf<String>() //菜品图片

                    //提取下架菜品
                    val dishMap = DishesDBHelper.getInstance().queryDishes().stream().filter {
                        it.status == 0
                    }.collect(Collectors.toMap({
                        "${it.mealId}:${it.dishesId}:${it.dishesName}"
                    }) { t -> t.status })
                    LogUtil.i(TAG, "未更新时已下架菜品总数: ${dishMap.size}")
                    dishMap.forEach { t, u ->
                        LogUtil.i(TAG, "下架的菜品 $t $u")
                    }

                    for (da in rs.data!!) {
                        val meal = MealTable()
                        meal.mealId = da.mealId
                        meal.mealName = da.mealName
                        if (da.mealName == null) continue

                        da.startTime?.also {
                            val split = it.split(":")
                            val date = Date()
                            date.hours = split[0].toInt()
                            date.minutes = split[1].toInt()
                            date.seconds = split[2].toInt()
                            meal.startTime = date
                        }

                        da.endTime?.also {
                            val split = it.split(":")
                            val date = Date()
                            date.hours = split[0].toInt()
                            date.minutes = split[1].toInt()
                            date.seconds = split[2].toInt()
                            meal.endTime = date
                        }

                        mealList.add(meal)
                        for (bean in da.selectedDishesList) {
                            val dish = DishesTable()
                            dish.dishesId = bean.dishesId
                            dish.dishesName = bean.dishesName
                            dish.mealId = da.mealId
                            dish.price = bean.price.toDouble()
                            dish.unit = bean.unit
                            dish.imgUrl = bean.imgUrl ?: ""
                            dish.status = dishMap.get("${dish.mealId}:${dish.dishesId}:${dish.dishesName}") ?: 1
                            dishList.add(dish)
                            picList.add(dish.imgUrl)
                        }
                    }
                    LogUtil.i(TAG, "mealList: $mealList")
                    DishesDBHelper.getInstance().clearAllDishes()
                    DishesDBHelper.getInstance().clearAllMeal()
                    DishesDBHelper.getInstance().insertDishes(dishList)
                    DishesDBHelper.getInstance().insertMeals(mealList)

                    //下载菜品图片
                    downLoadPic(picList)

                    //设置菜品数据已更新
                    val now = DateFormat.format("yyyyMMdd HH:mm:ss", System.currentTimeMillis()).toString()
                    kv.encode(Constant.UPDATE_TIME, now.substring(0, 8))
                    kv.encode(Constant.FINAL_TIME, now)
                    //发送菜品更新通知
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_FIFTH, null))
                } else {
                    LogUtil.e(TAG, "菜品下载出错 ${rs.msg}")
                    showToastEvent.postValue("菜品下载出错 ${rs.msg}")
                }
                loadingEvent.postValue(false)
            }
        }

    }


    /**
     * 发起人脸支付
     * @param service ZHSTFacePayService?
     * @param productsDetail ProductsDetail
     * @throws RemoteException
     */
    fun startPayWithFace(service: ZHSTFacePayService?, detail: ProductsDetail) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            if (TextUtils.isEmpty(mPayCfg.campusId) || TextUtils.isEmpty(mPayCfg.businessId) || TextUtils.isEmpty(mPayCfg.counterId)) {
                LogUtil.e(TAG, "未配置支付环境")
                throw Throwable("未配置支付环境")
            }
            val stringBuffer = StringBuffer()
            for (da in detail.products) stringBuffer.append("${da.dishesName};")
            //0-在线 1-离线
            val offline = if (kv.decodeBool(Constant.SWITCH)) 1 else 0
            service!!.stopFacePay()
            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = mPayCfg.campusId
            bean.CORP_ID = mPayCfg.corp_id
            bean.PAYMENT = detail.totalMoney.replace('元', ' ')
            bean.BUSINESS_ID = mPayCfg.businessId
            bean.VPOS_ID = mPayCfg.counterId
            bean.REMARK = stringBuffer.toString()
            bean.OFFLINE = offline.toString()

            service!!.startFacePay(Gson().toJson(bean), bean.OFFLINE, object : PayResultListener.Stub() {
                override fun onResult(result: String) {
                    LogUtil.d(TAG, result)
                    val responseStr = result.replace("\"[", "[").replace("]\"", "]")
                    val currentTime = System.currentTimeMillis()
                    val payResult = Gson().fromJson(responseStr, CcbFacePayResultBean::class.java)
                    val payForUI = PayForUI().apply {
                        businessId = mPayCfg.businessId
                        businessName = mPayCfg.businessName
                        campusId = mPayCfg.campusId
                        corpId = mPayCfg.corp_id
                        vposId = mPayCfg.counterId
                        deviceId = CommonAndDpToPxUtil.getDeviceSerial()
                        payType = "1"
                        payment = detail.totalMoney
                        orderId = payResult.ORDER_ID
                        payTime = payResult.PAYTIME
                        payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
                        sessionId = "${CommonAndDpToPxUtil.getDeviceSerial()}$currentTime${Random().nextInt(10)}"
                        signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
                        this.offline = offline.toString()
                    }
                    detail.products.forEach {
                        val dish = Dish().apply {
                            dishesId = it.dishesId
                            dishesName = it.dishesName
                            dishesNumber = it.count.toString()
                            dishesPrice = it.price.toString()
                        }
                        payForUI.paymentDishes.add(dish)
                    }
                    when (payResult.RESULT) {
                        "Y" -> { //订单状态,成功
                            payForUI.username = payResult.CUST_NAME
                            payForUI.custId = payResult.CUST_ID
                            if (offline == 0) payForUI.actualPayment = payResult.ACTUAL_PAYMENT  //非离线用实际支付值
                            payForUI.accType = payResult.ACC_TYPE
                            payForUI.accNo = payResult.ACC_NO
                            payForUI.accBal = payResult.ACC_BAL
//                            payForUI.accList = payResult.ACC_LIST
                            payResult.ACC_LIST.forEach {
                                val acclist = ACCLIST().apply {
                                    ACC_NO = it.ACC_NO
                                    ACC_BAL = it.ACC_BAL
                                    ACC_TYPE = it.ACC_TYPE
                                    TRAN_ID = it.TRAN_ID
                                    PAYMENT = it.PAYMENT
                                }
                                payForUI.accList.add(acclist)
                            }
                            //检查支付结果，
                            when (payResult.TRAN_RESULT) {
                                "3" -> {  //3支付成功
                                    payForUI.result = "Y"
                                    payForUI.traceId = payResult.TRACEID
                                    saveOrSynOrder(payForUI)
                                }
                                else -> { //1 -待支付、2-支付失败
                                    payForUI.errCode = payResult.ERRCODE
                                    payForUI.errMsg = payResult.ERRMSG
                                }
                            }
                        }
                        else -> { //订单状态,失败
                            payForUI.errCode = payResult.ERRCODE
                            payForUI.errMsg = payResult.ERRMSG
                        }
                    }
                    listener?.onFacePayResult(payForUI)
                }
            })
        }
    }

    /**
     * 收款模式发起人脸支付
     */
    fun startPayWithFace(service: ZHSTFacePayService?, amount: Float) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            if (TextUtils.isEmpty(mPayCfg.campusId) || TextUtils.isEmpty(mPayCfg.businessId) || TextUtils.isEmpty(mPayCfg.counterId)) {
                LogUtil.e(TAG, "未配置支付环境")
                val err = PayForUI()
                err.payType = "1"
                err.errMsg = "未配置支付环境"
                err.payTime = DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString()
                listener?.onFacePayResult(err)
                return@launch
            }
            service!!.stopFacePay()
            val offline = if (kv.decodeBool(Constant.SWITCH)) 1 else 0  //在线

            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = mPayCfg.campusId
            bean.CORP_ID = mPayCfg.corp_id
            bean.PAYMENT = String.format("%.2f", amount)
            bean.BUSINESS_ID = mPayCfg.businessId
            bean.VPOS_ID = mPayCfg.counterId
            bean.OFFLINE = offline.toString()

            service!!.startFacePay(Gson().toJson(bean), bean.OFFLINE, object : PayResultListener.Stub() {
                override fun onResult(result: String) {
                    LogUtil.d(TAG, result)
                    val currentTime = System.currentTimeMillis()
                    val payResult = Gson().fromJson(result, CcbFacePayResultBean::class.java)
                    val payForUI = PayForUI().apply {
                        businessId = mPayCfg.businessId
                        businessName = mPayCfg.businessName
                        campusId = mPayCfg.campusId
                        corpId = mPayCfg.corp_id
                        vposId = mPayCfg.counterId
                        deviceId = CommonAndDpToPxUtil.getDeviceSerial()
                        payType = "1"
                        payment = payResult.PAYMENT
                        orderId = payResult.ORDER_ID
                        payTime = payResult.PAYTIME
                        payDate = TimeUtil.timeFormat("yyyy-MM-dd", currentTime)
                        sessionId = "${CommonAndDpToPxUtil.getDeviceSerial()}$currentTime${Random().nextInt(10)}"
                        signTime = TimeUtil.timeFormat("yyyyMMddHHmmss", currentTime)
                        this.offline = offline.toString()
                    }
                    when (payResult.RESULT) {
                        "Y" -> { //订单状态,成功
                            payForUI.username = payResult.CUST_NAME
                            payForUI.custId = payResult.CUST_ID
                            if (offline == 0) payForUI.actualPayment = payResult.ACTUAL_PAYMENT  //非离线用实际支付值
                            payForUI.accType = payResult.ACC_TYPE
                            payForUI.accNo = payResult.ACC_NO
                            payForUI.accBal = payResult.ACC_BAL
                            payResult.ACC_LIST.forEach {
                                val acclist = ACCLIST().apply {
                                    ACC_NO = it.ACC_NO
                                    ACC_BAL = it.ACC_BAL
                                    ACC_TYPE = it.ACC_TYPE
                                    TRAN_ID = it.TRAN_ID
                                    PAYMENT = it.PAYMENT
                                }
                                payForUI.accList.add(acclist)
                            }
                            //检查支付结果，
                            when (payResult.TRAN_RESULT) {
                                "3" -> {  //3支付成功
                                    payForUI.result = payResult.RESULT
                                    payForUI.traceId = payResult.TRACEID
                                    saveOrSynOrder(payForUI)
                                }
                                else -> { //1 -待支付、2-支付失败
                                    payForUI.result = payResult.RESULT
                                    payForUI.errCode = payResult.ERRCODE
                                    payForUI.errMsg = payResult.ERRMSG
                                }
                            }
                        }
                        else -> { //订单状态,失败
                            payForUI.result = payResult.RESULT
                            payForUI.errCode = payResult.ERRCODE
                            payForUI.errMsg = payResult.ERRMSG
                        }
                    }
                    listener?.onFacePayResult(payForUI)
                }
            })
        }
    }


    /**
     * 保存或同步消费记录,离线模式将直接保存，在线模式上传失败也会保存
     */
    private fun saveOrSynOrder(payForUI: PayForUI) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            //保存记录
            val payOrder = Gson().fromJson(Gson().toJson(payForUI), PayOrderTable::class.java)
            payOrder.tranResult = "3" //1：待支付，2：支付失败，3：支付成功
            dbHelper.insertPayOrder(payOrder)
            val order = dbHelper.queryPayOrder(payOrder.orderId)
            payForUI.paymentDishes.forEach {
                val dish = Gson().fromJson(Gson().toJson(it), PayDishTable::class.java)
                dish.payOrderTable = order
                dbHelper.insertPayDish(dish)
            }
            //上传记录
            val bean = SynConsumeRecordBean().apply {
                deviceSerialNumber = order.deviceId
                businessId = order.businessId
                campusId = order.campusId
                counterId = order.vposId
                consumptionType = order.payType
                RESULT = order.result
                CUST_ID = order.custId
                PAYMENT = order.payment
                ACTUAL_PAYMENT = order.actualPayment ?: "0.0"
                ACC_NO = order.accNo
                ACC_BAL = order.accBal
                ACC_TYPE = order.accType
                TRACEID = order.traceId
                ORDER_ID = order.orderId
                TRAN_RESULT = order.tranResult
                OFFLINE = order.offline
                ERRCODE = ""
                ERRMSG = ""
                order.accList.forEach {
                    val acclist = ACCLIST().apply {
                        ACC_NO = it.acC_NO
                        ACC_BAL = it.acC_BAL
                        ACC_TYPE = it.acC_TYPE
                        TRAN_ID = it.traN_ID
                        PAYMENT = it.payment
                    }
                    ACC_LIST.add(acclist)
                }
                PAYTIME = order.payTime
                BUSINESS_NAME = order.businessName
            }
            payForUI.paymentDishes.forEach { bean.paymentDishesList.add(it) }
            LogUtil.d(TAG, Gson().toJson(bean))
            if (payForUI.offline == "0") {
                val res = mRespository.synCsRecord(bean)
                if (res.code == "200") {
                    order.flag = 1
                    dbHelper.updatePayOrder(order)
                    LogUtil.i(TAG, "订单${bean.ORDER_ID} 上传成功!")
                } else LogUtil.e(TAG, "上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
            }
        }
    }

    private fun downLoadPic(picList: MutableList<String>) {
        if (picList.size < 1) return
        // 清空目录
        val savePath = File(MyApplication.applicationContext.filesDir, Constant.PIC_DIR)
        if (!savePath.exists()) {
            savePath.mkdirs()
        }
        for (fi in savePath.listFiles()) {
            fi.delete()
        }
        for (path in picList) {
            if (path == null || path.isEmpty()) continue
            val pic = Glide.with(MyApplication.applicationContext)
                .load(path)
                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
            writeFile2Sd(pic, path.substring(path.lastIndexOf("/") + 1))
        }
    }

    private fun writeFile2Sd(source: File, name: String) {
        val file = File("${MyApplication.applicationContext.filesDir.absolutePath}${File.separator}${Constant.PIC_DIR}${File.separator}${name}")
        var fos: FileOutputStream? = null
        var fis: FileInputStream? = null
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            fis = FileInputStream(source)
            fos = FileOutputStream(file)

            val buf = ByteArray(1024)
            var len: Int
            while (fis.read(buf, 0, buf.size).also { len = it } != -1) {
                fos.write(buf, 0, len)
            }
            fos.flush()
            LogUtil.i(TAG, "download ：${file.name} !")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
                fis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}