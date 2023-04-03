package com.yannuo.dgcanteen.model

data class ProductsDetail (
    var products :MutableList<DishesInfo>,
    var totalMoney :String = "0.00" , //总计花费
    var count :String = "0" , //选择总数
)