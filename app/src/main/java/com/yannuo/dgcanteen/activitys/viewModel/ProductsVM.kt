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
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.interfaces.ICommodityPresenter
import com.yannuo.dgcanteen.model.*
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.util.*

class ProductsVM :ViewModel() {
    val sendCountNotify : MutableLiveData<DishesInfo> = MutableLiveData()
    val receiveCountNotify : MutableLiveData<DishesInfo> = MutableLiveData()
    var showToastEvent : MutableLiveData<String>
    var loadingEvent : MutableLiveData<Boolean>
    var resultEvent : MutableLiveData<PayResultForUI>  //支付结果

    private val TAG = javaClass.simpleName
    private lateinit var mRespository :PayRepositoryOfPay
    private var exceptionHandler :CoroutineExceptionHandler

    var listener : ICommodityPresenter?= null

    init {
        showToastEvent = MutableLiveData()
        loadingEvent = MutableLiveData()
        resultEvent = MutableLiveData()
        mRespository =  PayRepositoryOfPay()

        exceptionHandler =  CoroutineExceptionHandler { coroutineContext, throwable ->
            LogUtil.e(TAG,"协程异常： $throwable ${throwable.message}")
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
                        meal.startTime  = Date(2023,4,3,7+mid,0,0)
                        mid +=2
                        meal.endTime  = Date(2023,4,3,7+mid,0,0)
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

            val bean = CcbFacePayBean()
            bean.CAMPUS_ID = "441999527"
            bean.CORP_ID = "1041"
            bean.PAYMENT = detail.totalMoney
            bean.BUSINESS_ID = "SJ2023032511004"
            bean.VPOS_ID = "V00443832"
            bean.REMARK = stringBuffer.toString()
            bean.OFFLINE = "0"
            try {
                service!!.startFacePay(
                    Gson().toJson(bean), "0",
                    object : PayResultListener.Stub() {
                        @Throws(RemoteException::class)
                        override fun onResult(result: String) {
                            LogUtil.d(TAG, "" + result)
                            listener?.onFacePayResult()
                        }
                    })
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }
}