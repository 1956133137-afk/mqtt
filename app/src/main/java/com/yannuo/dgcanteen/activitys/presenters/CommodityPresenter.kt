package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import android.os.RemoteException
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.interfaces.ICommodityPresenter
import com.yannuo.dgcanteen.model.CcbFacePayBean
import com.yannuo.dgcanteen.model.CcbScanPayBean
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CommodityPresenter(context :Context) {







     fun startPayWithScan(){
          val bean = CcbScanPayBean()
          bean.CAMPUS_ID = "441999527"
          bean.CORP_ID = "1041"
          bean.TXCODE = "PAY003"
          bean.ccbSafeParam = ""
          bean.BUSINESS_ID = "SJ2023032511004"
          bean.VPOS_ID = "V00443832"
          bean.PAYMENT = "0.01"
          bean.ACTUAL_PAYMENT = "0.01"
          bean.COUPON_INFO = ""
          bean.ACC_NOS = "123"
          bean.QR_CODE = ""
          bean.CUST_ID = ""
          bean.ORDER_ID = ""
          bean.OFFLINE = "0"
          bean.SIGN_TIME = "20230403153000"
          try{
               runBlocking (Dispatchers.IO) {
                    val repository = PayRepositoryOfPay()
                    val response = repository.getScanQrData(bean)
                    LogUtil.e("test", Gson().toJson(response))
               }
          }catch (e: RemoteException) {
               e.printStackTrace()
          }
     }



}