package com.yannuo.dgcanteen.activitys.viewModel

import android.os.RemoteException
import android.text.format.DateFormat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.DishesTable
import com.yannuo.dgcanteen.dao.MealTable
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class ProductsVM :ViewModel() {
    val sendCountNotify : MutableLiveData<DishesInfo> = MutableLiveData()
    val receiveCountNotify : MutableLiveData<DishesInfo> = MutableLiveData()
    var showToastEvent : MutableLiveData<String>
    var loadingEvent : MutableLiveData<Boolean>


    private val TAG = javaClass.simpleName
    private lateinit var mRespository :PayRepositoryOfPay
    private var exceptionHandler :CoroutineExceptionHandler

    var listener : IProductsVM?= null


    init {
        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()
        mRespository =  PayRepositoryOfPay()

        exceptionHandler =  CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG,"协程异常： $throwable ${throwable.message}")
            showToastEvent.postValue("错误： ${throwable.message}")
        }
    }


    /**
     * 检查是否已更新菜品
     * @return Boolean
     */
    private fun checkIsNeedUpdate() :Boolean{
        val kv = MMKV.defaultMMKV()
        val old = kv.decodeString(Constant.UPDATE_TIME,Constant.update_time)
        val now = DateFormat.format("yyyyMMdd",System.currentTimeMillis()).toString()
        return  (old!!.toInt() >= now.toInt())
//        return  false
    }


    fun upDataDishes(){
        viewModelScope.launch(exceptionHandler + Dispatchers.Default) {
            val check = checkIsNeedUpdate()
            if (check.not()){
                loadingEvent.postValue(true)
                val rs = mRespository.getDayDishes()
                if (rs.code == HttpURLConnection.HTTP_OK){
                    val mealList = mutableListOf<MealTable>()
                    val dishList = mutableListOf<DishesTable>()
                    var mid = 2
                    for (da in rs.data!!){
                        val meal = MealTable()
                        meal.mealId = da.mealId
                        meal.mealName = da.mealName

                        //测试代码
                        meal.startTime  = Date(2023,4,6,7+mid,0,0)
                        mid +=2
                        meal.endTime  = Date(2023,4,6,7+mid,0,0)
                        mid +=2

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
                        }
                    }
                    DishesDBHelper.getInstance().clearAllDishes()
                    DishesDBHelper.getInstance().clearAllMeal()
                    DishesDBHelper.getInstance().insertDishes(dishList)
                    DishesDBHelper.getInstance().insertMeals(mealList)
                    //设置菜品数据已更新
                    val kv = MMKV.defaultMMKV()
                    val now = DateFormat.format("yyyyMMdd",System.currentTimeMillis()).toString()
                    kv.encode(Constant.UPDATE_TIME,now)
                    kv.encode("FinalTime", SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(Date()))
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
            val stringBuffer  = StringBuffer()
            for (da in detail.products){
                stringBuffer.append("${ da.dishesName};")
            }

            var offline = 0  //在线
            if (MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) {
                offline = 1  //离线
            }
            offline = 1  //离线
            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = "441999527"
            bean.CORP_ID = "1041"
            bean.PAYMENT = detail.totalMoney.replace('元',' ')
            bean.BUSINESS_ID = "SJ2023032511004"
            bean.VPOS_ID = "V00443832"
            bean.REMARK = stringBuffer.toString()
            bean.OFFLINE = offline.toString()

            service!!.startFacePay(
                Gson().toJson(bean),  bean.OFFLINE, object : PayResultListener.Stub() { @Throws(RemoteException::class)
                override fun onResult(result: String) {
                    LogUtil.d(TAG, "" + result)
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
     * 保存或同步消费记录
     */
   private fun saveOrSynConsumeRecord(
        payResult: CcbFacePayResultBean,
        products: MutableList<DishesInfo>
    ) {
       viewModelScope.launch(exceptionHandler + Dispatchers.Default) {
           val bean = SynConsumeRecordBean()
           bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
           bean.businessId = "SJ2023032511004"
           bean.counterId = "V00443832"
           bean.RESULT  = "Y"
           bean.CUST_ID = payResult.CUST_ID
           bean.PAYMENT = payResult.PAYMENT!!.toDouble()
           bean.ACTUAL_PAYMENT = payResult.ACTUAL_PAYMENT?.toDouble()  ?: 0.0
           bean.ACC_NO = payResult.ACC_NO
           bean.ACC_BAL = payResult.ACC_BAL?.toDouble()
           bean.ACC_TYPE = payResult.ACC_TYPE?.toInt()
           bean.TRACEID = payResult.TRACEID
           bean.ORDER_ID = payResult.ORDER_ID
           bean.TRAN_RESULT =  3
           bean.OFFLINE = payResult.OFFLINE?.toInt()
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
          val res = mRespository.synCsRecord(bean)
           if (res.code != HttpURLConnection.HTTP_OK){
               LogUtil.e(TAG,"上传消费${bean.ORDER_ID} 订单失败==\n${res.data}")
           }
           LogUtil.i(TAG,"订单${bean.ORDER_ID} 上传成功!")
       }
   }
}