package com.yannuo.dgcanteen.model



data class PrinterTicker(
    var title : String? ,   //小票标题
    var stitle : Array<String> ,   //列表项（商品编码        单价  数量       小计）
    var products  : MutableList<Array<String?>>? ,   //购买的商品
    var quantity: String ,   //购买的商品数量（件）
    var amount  : String ,   //购买的商品总金额
    var cashier : String , //收银员
    var welcomeSpeech : String? = null,  //欢迎词（欢迎光临   多谢惠顾）
    var storeName : String? = null , //店名
    var timeBuying: String? = null ,   //购买的商品时间
    var journalNumber : String ?= null  //交易流水号

)
