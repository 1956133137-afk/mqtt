package com.yannuo.dgcanteen.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ItemQuotaTimeBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.QuotaTimeTable
import com.yannuo.dgcanteen.util.LogUtil

class QuotaTimeAdapter : BaseAdapter<QuotaTimeTable, ItemQuotaTimeBinding>() {
    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemQuotaTimeBinding {
        return ItemQuotaTimeBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = getData(position)
        holder.binding.startTime.text = data.startTime
        holder.binding.endTime.text = data.endTime
        holder.binding.fixedAmount.setText(data.quotaAmount)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty().not()) {
            holder.binding.fixedAmount.setText(data[position].quotaAmount)
        } else bindHolder(holder, position)
    }

    override fun addEventListener(holder: Holder) {
        holder.binding.fixedAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (isValidMoney(editable.toString())) {
                    val position = holder.adapterPosition
                    if (position == RecyclerView.NO_POSITION) return
                    val quotaTime = getData(position)
                    quotaTime?.quotaAmount = editable.toString()
                    DishesDBHelper.getInstance().updateQuotaTime(quotaTime)
                    LogUtil.d(TAG, "修改: ${Gson().toJson(quotaTime)}")
                }
            }
        })
        holder.binding.delete.setOnClickListener {//删除
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val data = getData(position)
            DishesDBHelper.getInstance().deleteQuotaTime(data)
            mData?.removeAt(position)
            notifyItemRemoved(position)
            LogUtil.d(TAG, "删除: ${Gson().toJson(data)}")
        }
    }

    private fun isValidMoney(str: String): Boolean {
        val regex = Regex("""^(0|[1-9]\d{0,5})(\.\d{0,2})?$""", RegexOption.IGNORE_CASE)
        return regex.matches(str)
    }
}