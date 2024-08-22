package com.yannuo.dgcanteen.activitys.presenters

import android.content.Context
import com.yannuo.dgcanteen.adapters.ProductsAdapter
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.model.DishesInfo

class OrderMenuPresenter(context: Context) {
    private var cnt = context


    fun dishesData(adapter : ProductsAdapter? , i: Int) {
        val dataList: MutableList<DishesInfo> = ArrayList()
        val list = DishesDBHelper.getInstance(cnt).queryDishesByMealIdAneStatus(i, 1)
        for (u in list) {
            dataList.add(
                DishesInfo(
                    u.dishesId,
                    u.dishesName,
                    u.mealId,
                    null,
                    u.price?:0.00,
                    u.unit?:"",
                    u.imgUrl?:"",
                    u.status,
                    0
                )
            )
        }
        adapter?.data = dataList
    }

    fun calculate(list : MutableList<DishesInfo>):FloatArray{
        val result = FloatArray(2)
        list.forEach {
            if (it.count > 0){
                val mid = it.price.toBigDecimal().multiply(it.count.toBigDecimal()).add(result[0].toBigDecimal())
                result[0] = mid.toFloat()
                result[1] = it.count + result[1]
            }
        }
        return result
    }


}