package com.yannuo.dgcanteen.activitys.presenters

import com.yannuo.dgcanteen.model.DishesInfo

class DataPresenter() {
    private val TAG = javaClass.simpleName

    fun calculate(list: MutableList<DishesInfo>): FloatArray {
        val result = FloatArray(2)
        list.forEach {
            if (it.count > 0) {
                val mid = it.price.toBigDecimal().multiply(it.count.toBigDecimal()).add(result[0].toBigDecimal())
                result[0] = mid.toFloat()
                result[1] = it.count + result[1]
            }
        }
        return result
    }
}