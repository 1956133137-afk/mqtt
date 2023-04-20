package com.yannuo.dgcanteen.activitys.presenters

import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.dao.CardPay
import com.yannuo.dgcanteen.dao.OffLineTable
import com.yannuo.dgcanteen.dao.OrderDishList
import com.yannuo.dgcanteen.dao.OwnOrder
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.PaymentDishesList
import com.yannuo.dgcanteen.model.ScanQrResultBean
import com.yannuo.dgcanteen.model.SynConsumeRecordBean
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class DataPresenter() {

    private val TAG = javaClass.simpleName

    private var mRespository : PayRepositoryOfPay = PayRepositoryOfPay()

    //同步扫码消费记录
    suspend fun consumeRecord( data : OffLineTable, res : ScanQrResultBean, DishesData : MutableList<DishesInfo>){
        val bean = SynConsumeRecordBean()
        bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
        bean.businessId = data.businesS_ID
        bean.counterId = data.vpoS_ID
        bean.consumptionType = 3
        if (data.qR_CODE != null){
            bean.consumptionType = 2    //1：刷脸，2：扫码，3：刷卡
        }
        bean.RESULT  = res.RESULT.toString()
        bean.CUST_ID = data.cusT_ID
        bean.PAYMENT = res.PAYMENT?.toDouble()
        bean.ACTUAL_PAYMENT = res.ACTUAL_PAYMENT?.toDouble()
        bean.ACC_NO = res.ACC_NO
        bean.ACC_BAL = res.ACC_BAL?.toDouble()
        bean.ACC_TYPE = res.ACC_TYPE?.toInt()
        bean.TRACEID = ""
        bean.ORDER_ID = data.ordeR_ID
        bean.TRAN_RESULT =  when(res.RESULT.toString()){
            "Y" -> 3
            "N" -> 2
            else -> null
        }
        bean.OFFLINE = data.offline.toInt()
        bean.ERRCODE = res.ERRCODE
        bean.ERRMSG = res.ERRMSG
        bean.ACCALIAS = when(bean.ACC_TYPE){
            1 -> "现金账号"
            2 -> "餐补账户"
            3 -> "餐补账户1"
            else -> ""
        }
        bean.PAYTIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        bean.BUSINESS_NAME = "彦诺智能测试园区"
        bean.paymentDishesList = mutableListOf()
        DishesData.forEach {
            bean.paymentDishesList.add(
                PaymentDishesList(
                it.dishesId,
                it.dishesName,
                it.count,
                it.price
            )
            )
        }

        val responseScanPay = mRespository.synCsRecord(bean)
        LogUtil.i(TAG, Gson().toJson(responseScanPay))
        if (responseScanPay.code != HttpURLConnection.HTTP_OK){
            saveFailureRecord(bean)
            LogUtil.e(TAG,"上传消费${bean.ORDER_ID} 订单失败==\n${responseScanPay.data}")
        }else {
            LogUtil.i(TAG,"订单${bean.ORDER_ID} 上传成功!")
        }

    }

    //同步刷卡消费记录
    suspend fun cardConsumeRecord(data : CardPay, res : ScanQrResultBean, DishesData :MutableList<DishesInfo>){
        val bean = SynConsumeRecordBean()
        bean.deviceSerialNumber = CommonAndDpToPxUtil.getDeviceSerial()
        bean.businessId = data.business_id
        bean.counterId = data.vpos_id
        bean.consumptionType = 3    //1：刷脸，2：扫码，3：刷卡
        bean.RESULT  = res.RESULT.toString()
        bean.CUST_ID = data.cust_id
        bean.PAYMENT = res.PAYMENT?.toDouble()
        bean.ACTUAL_PAYMENT = res.ACTUAL_PAYMENT?.toDouble()
        bean.ACC_NO = res.ACC_NO
        bean.ACC_BAL = res.ACC_BAL?.toDouble()
        bean.ACC_TYPE = res.ACC_TYPE?.toInt()
        bean.TRACEID = ""
        bean.ORDER_ID = data.order_id
        bean.TRAN_RESULT =  when(res.RESULT.toString()){
            "Y" -> 3
            "N" -> 2
            else -> null
        }
        bean.OFFLINE = data.offline.toInt()
        bean.ERRCODE = res.ERRCODE
        bean.ERRMSG = res.ERRMSG
        bean.ACCALIAS = when(bean.ACC_TYPE){
            1 -> "现金账号"
            2 -> "餐补账户"
            3 -> "餐补账户1"
            else -> ""
        }
        bean.PAYTIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        bean.BUSINESS_NAME = "彦诺智能测试园区"
        bean.paymentDishesList = mutableListOf()
        DishesData.forEach {
            bean.paymentDishesList.add(PaymentDishesList(
                it.dishesId,
                it.dishesName,
                it.count,
                it.price
            ))
        }

        val responseScanPay = mRespository.synCsRecord(bean)
        LogUtil.i(TAG,Gson().toJson(responseScanPay))
        if (responseScanPay.code != HttpURLConnection.HTTP_OK){
            saveFailureRecord(bean)
            LogUtil.e(TAG,"上传消费${bean.ORDER_ID} 订单失败==\n${responseScanPay.data}")
        }else {
            LogUtil.i(TAG,"订单${bean.ORDER_ID} 上传成功!")
        }

    }

    //保存上传失败记录
    private fun saveFailureRecord(bean: SynConsumeRecordBean){
        val saveOrder = OwnOrder()
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