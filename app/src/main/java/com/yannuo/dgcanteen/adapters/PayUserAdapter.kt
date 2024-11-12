package com.yannuo.dgcanteen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.databinding.ItemPayUserBinding
import com.yannuo.dgcanteen.databinding.ItemVerifyUserBinding
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable
import com.yannuo.dgcanteen.greendao.entity.VerifyDishes

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/11/12 15:27
 **/
class PayUserAdapter : BaseAdapter<PayOrderTable, ItemPayUserBinding>() {

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemPayUserBinding {
        return ItemPayUserBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = mData[position]
        holder.binding.verifyMsg.text = "${data.payTime}\n${data.username}支付${data.actualPayment.ifEmpty { data.payment }}元"
    }
}