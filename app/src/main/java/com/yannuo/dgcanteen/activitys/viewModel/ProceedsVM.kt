package com.yannuo.dgcanteen.activitys.viewModel

import androidx.lifecycle.ViewModel
import com.yannuo.dgcanteen.util.LogUtil

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/28 17:51
 **/
class ProceedsVM : ViewModel() {
    private val TAG = javaClass.simpleName

    enum class PayStatus {
        PAY,
        INVALID
    }

    //处理支付
    fun getAmount(amount: String, status: PayStatus) {
        LogUtil.d(TAG, "amount: $amount status: $status")
        if (status == PayStatus.PAY) {

        }
    }

    override fun onCleared() {
        super.onCleared()
    }

}