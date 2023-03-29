package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.ProductInfo

class ProductsVM :ViewModel() {
    val sendCountNotify : MutableLiveData<DishesInfo> = MutableLiveData()
    val receiveCountNotify : MutableLiveData<DishesInfo> = MutableLiveData()


}