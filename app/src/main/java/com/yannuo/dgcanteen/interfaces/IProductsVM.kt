package com.yannuo.dgcanteen.interfaces

import com.yannuo.dgcanteen.model.PayResultForUI

interface IProductsVM {
    //刷脸支付结果
    fun onFacePayResult(data : PayResultForUI)

}