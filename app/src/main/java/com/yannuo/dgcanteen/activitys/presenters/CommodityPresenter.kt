package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import android.os.RemoteException
import com.ccb.smartcanteen.PayResultListener
import com.ccb.smartcanteen.ZHSTFacePayService
import com.google.gson.Gson
import com.yannuo.dgcanteen.interfaces.ICommodityPresenter
import com.yannuo.dgcanteen.model.CcbFacePayBean
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.LogUtil

class CommodityPresenter(context :Context) {
     private var cnt = context
     private val TAG = javaClass.simpleName

     var listener : ICommodityPresenter ?= null


     fun startPayWithFace(service: ZHSTFacePayService?, productsDetail: ProductsDetail) {
          val bean = CcbFacePayBean()
          bean.CAMPUS_ID = "441999527"
          bean.CORP_ID = "1041"
          bean.PAYMENT = "0.01"
          bean.BUSINESS_ID = "SJ2023032511004"
          bean.VPOS_ID = "V00443832"
          bean.TXCODE = "ZF0001"
          bean.OFFLINE = "0"
          try {
               service!!.startFacePay(Gson().toJson(bean), "0",
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