package com.yannuo.dgcanteen.activitys.viewModel

import android.os.RemoteException
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.dao.DishesTable
import com.yannuo.dgcanteen.dao.MealTable
import com.yannuo.dgcanteen.dao.OrderDishList
import com.yannuo.dgcanteen.dao.OwnOrder
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*

class ProductsVM :ViewModel() {
    var showToastEvent : MutableLiveData<String>
    var loadingEvent : MutableLiveData<Boolean>


    private val TAG = javaClass.simpleName
    private lateinit var mRespository :PayRepositoryOfPay
    private var exceptionHandler :CoroutineExceptionHandler
    private lateinit var kv : MMKV
    private var mPayCfg : PayCfg ?= null

    var listener : IProductsVM?= null


    init {
        kv = MMKV.defaultMMKV()
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()
        mRespository =  PayRepositoryOfPay()

        exceptionHandler =  CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG,"协程异常： $throwable ${throwable.printStackTrace()}")
            showToastEvent.postValue("错误： ${throwable.message}")
        }
    }


    /**
     * 检查是否已更新菜品
     * @return Boolean
     */
    private fun checkIsNeedUpdate() :Boolean{
//        val kv = MMKV.defaultMMKV()
        val old = kv.decodeString(Constant.UPDATE_TIME,Constant.update_time)
        val now = DateFormat.format("yyyyMMdd",System.currentTimeMillis()).toString()
        return  (old!!.toInt() >= now.toInt())
    }


    fun upDataDishes(force :Boolean = false){
        viewModelScope.launch(exceptionHandler + Dispatchers.Default) {
            val check = checkIsNeedUpdate()
            if (check.not() || force){
                loadingEvent.postValue(true)
                val rs = mRespository.getDayDishes()
                if (rs.code == HttpURLConnection.HTTP_OK){
                    val mealList = mutableListOf<MealTable>()
                    val dishList = mutableListOf<DishesTable>()
                     val picList =  mutableListOf<String>() //菜品图片
                    for (da in rs.data!!){
                        val meal = MealTable()
                        meal.mealId = da.mealId
                        meal.mealName = da.mealName

                        da.startTime?.also {
                            val split =it.split(":")
                            val date = Date()
                            date.hours = split[0].toInt()
                            date.minutes = split[1].toInt()
                            date.seconds = split[2].toInt()
                            meal.startTime = date
                        }

                        da.endTime?.also {
                            val split =it.split(":")
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
                            dish.imgUrl = bean.imgUrl
                            dishList.add(dish)
                            picList.add(bean.imgUrl)
                        }
                    }
                    DishesDBHelper.getInstance().clearAllDishes()
                    DishesDBHelper.getInstance().clearAllMeal()
                    DishesDBHelper.getInstance().insertDishes(dishList)
                    DishesDBHelper.getInstance().insertMeals(mealList)

                    //下载菜品图片
                    downLoadPic(picList)

                    //设置菜品数据已更新
//                    val kv = MMKV.defaultMMKV()
                    val now = DateFormat.format("yyyyMMdd HH:mm:ss",System.currentTimeMillis()).toString()
                    kv.encode(Constant.UPDATE_TIME,now.substring(0,8))
                    kv.encode(Constant.FINAL_TIME,now )
                    //发送菜品更新通知
                    EventBus.getDefault().post(MessageEvent(Constant.EVENT_FIFTH, null))
                }else {
                    LogUtil.e(TAG,"菜品下载出错 ${rs.msg}")
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
        viewModelScope.launch(exceptionHandler + Dispatchers.Default) {
//            val mv = MMKV.defaultMMKV()
//            val payCfg = kv.decodeParcelable(
//                Constant.PAY_CONFIG,
//                PayCfg::class.java
//            )
            if (mPayCfg == null || TextUtils.isEmpty(mPayCfg!!.campusId) ||
                TextUtils.isEmpty(mPayCfg!!.businessId) || TextUtils.isEmpty(mPayCfg!!.counterId)
            ) {
                LogUtil.e(TAG, "未配置支付环境")
                throw Throwable("未配置支付环境")
            }
            val stringBuffer  = StringBuffer()
            for (da in detail.products){
                stringBuffer.append("${ da.dishesName};")
            }
            var offline = 0  //在线
            if (kv.decodeBool(Constant.SWITCH)) {
                offline = 1  //离线
            }


            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = mPayCfg!!.campusId.toString()
            bean.CORP_ID = mPayCfg!!.corp_id.toString()
            bean.PAYMENT = detail.totalMoney.replace('元',' ')
            bean.BUSINESS_ID = mPayCfg!!.businessId.toString()
            bean.VPOS_ID = mPayCfg!!.counterId.toString()
            bean.REMARK = stringBuffer.toString()
            bean.OFFLINE = offline.toString()

            service!!.startFacePay(
                Gson().toJson(bean),  bean.OFFLINE, object : PayResultListener.Stub() { @Throws(RemoteException::class)
                override fun onResult(result: String) {
                    LogUtil.d(TAG, result)
                    val payResult = Gson().fromJson(result,CcbFacePayResultBean::class.java)
                    val payState = PayResultForUI()
                    payState.way = "人脸支付"
                    payState.orderid = payResult.ORDER_ID
                    payState.timestamp = payResult.PAYTIME
                    payState.dishes = detail.products
                    payState.piece = detail.count.toInt()
                    when(payResult.RESULT){
                        "Y" -> { //订单状态,成功
                            payState.cust_name = payResult.CUST_NAME
                            payState.custId = payResult.CUST_ID
                            payState.payment = payResult.PAYMENT
                            if (offline == 0) payState.payment = payResult.ACTUAL_PAYMENT  //非离线用实际支付值
                            payState.acc_no =  payResult.ACC_NO
                            payState.acc_bal = payResult.ACC_BAL
                            //检查支付结果，
                            when(payResult.TRAN_RESULT){
                                "3"->{  //3支付成功
                                    payState.result = PayResultForUI.Result.SUCCESS
                                    payState.traceid  = payResult.TRACEID
                                    saveOrSynConsumeRecord(payResult,detail.products)
                                }
                                else ->{ //1 -待支付、2-支付失败
                                    payState.errormsg = "error ${payResult.ERRCODE} ${payResult.ERRMSG} "
                                }
                            }
                        }
                        else ->{ //订单状态,失败
                            payState.errormsg = "error ${payResult.ERRCODE} ${payResult.ERRMSG} "
                        }
                    }
                    listener?.onFacePayResult(payState)
                }
                })
        }
    }


    /**
     * 保存或同步消费记录,离线模式将直接保存，在线模式上传失败也会保存
     */
    private fun saveOrSynConsumeRecord(
        payResult: CcbFacePayResultBean,
        products: MutableList<DishesInfo>
    ) {
        viewModelScope.launch(exceptionHandler + Dispatchers.Default) {

            val bean = SynConsumeRecordBean()
            bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
            bean.businessId = mPayCfg?.businessId
            bean.counterId = mPayCfg?.counterId
            bean.consumptionType = 1
            bean.RESULT  = "Y"
            bean.CUST_ID = payResult.CUST_ID
            bean.PAYMENT = payResult.PAYMENT!!.toDouble()

            bean.ACTUAL_PAYMENT = if (payResult.ACTUAL_PAYMENT.isNullOrEmpty().not()) payResult.ACTUAL_PAYMENT!!.toDouble()
            else 0.0
            bean.ACC_NO = payResult.ACC_NO

            bean.ACC_BAL =  if (payResult.ACC_BAL.isNullOrEmpty().not()) payResult.ACC_BAL!!.toDouble()
            else 0.0
            bean.ACC_TYPE =   if (payResult.ACC_TYPE.isNullOrEmpty().not()) payResult.ACC_TYPE!!.toInt()
            else 1
            bean.TRACEID = payResult.TRACEID
            bean.ORDER_ID = payResult.ORDER_ID
            bean.TRAN_RESULT =  3
            bean.OFFLINE = payResult.OFFLINE.toInt()
            bean.ERRCODE = ""
            bean.ERRMSG = ""
            bean.ACCALIAS =payResult.ACCALIAS

            bean.PAYTIME = payResult.PAYTIME
            bean.BUSINESS_NAME = "彦诺智能测试园区"
            bean.paymentDishesList = mutableListOf()
            products.forEach {
                bean.paymentDishesList.add(PaymentDishesList(
                    it.dishesId,
                    it.dishesName,
                    it.count,
                    it.price
                ))
            }
            var needSave = true
            if(bean.OFFLINE == 0){
                val res = mRespository.synCsRecord(bean)
                if (res.code == HttpURLConnection.HTTP_OK){
                    needSave = false
                    LogUtil.i(TAG,"订单${bean.ORDER_ID} 上传成功!")
                }else{
                    LogUtil.e(TAG,"上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
                }
            }
            //上传成功直接返回
            if (needSave.not()) return@launch
            val saveOrder =  OwnOrder()
            bean.apply {
                saveOrder.deviceSerialNumber = deviceSerialNumber
                saveOrder.businessId = businessId
                saveOrder.counterId = counterId
                saveOrder.consumptionType = consumptionType
                saveOrder.result = RESULT
                saveOrder.cusT_ID = CUST_ID
                saveOrder.payment = PAYMENT ?:0.0

                saveOrder.actuaL_PAYMENT = ACTUAL_PAYMENT ?:0.0
                saveOrder.acC_NO = ACC_NO
                saveOrder.acC_BAL = ACC_BAL ?:0.0
                saveOrder.acC_TYPE = ACC_TYPE ?:1
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
            val saveDishList = mutableListOf<OrderDishList>()
            bean.paymentDishesList.forEach {
                val dish = OrderDishList()
                dish.dishesId = it.dishesId
                dish.dishesName = it.dishesName
                dish.dishesNumber = it.dishesNumber
                dish.dishesPrice = it.dishesPrice
                dish.order = saveOrder
                saveDishList.add(dish)
            }
            DishesDBHelper.getInstance().insertConsumerDishes(saveDishList)
        }
    }



    private fun downLoadPic(picList: MutableList<String>) {
        if (picList.size<1)return
        // 清空目录
        val savePath = File(MyApplication.applicationContext.filesDir, Constant.PIC_DIR)
        if (!savePath.exists()) {
            savePath.mkdirs()
        }
        for (fi in savePath.listFiles()){
            fi.delete()
        }
        for (path in picList) {
            val pic = Glide.with(MyApplication.applicationContext)
                .load(path)
                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
            writeFile2Sd(pic, path.substring(path.lastIndexOf("/")+1))
        }
    }

    private fun writeFile2Sd(source :File ,name :String) {
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
            LogUtil.i(TAG,"download ：${file.name} !")
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