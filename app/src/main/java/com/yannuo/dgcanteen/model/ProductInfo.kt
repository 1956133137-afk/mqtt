package com.yannuo.dgcanteen.model


data class ProductInfo(var pName :String,       //产品名称
                       var pMoney:String,       //产品单价
                       var filename :String?,     //产品图片文件名
                       var type :String,        //产品类型
                       var barCode:String? = null,  //产品条码ID
                       var count:Int = 0)           //产品数量
