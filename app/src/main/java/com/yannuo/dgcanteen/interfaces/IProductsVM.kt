package com.yannuo.dgcanteen.interfaces

import com.yannuo.dgcanteen.model.PayForUI

interface IProductsVM {
    //刷脸支付结果
    fun onFacePayResult(payForUI: PayForUI)

}