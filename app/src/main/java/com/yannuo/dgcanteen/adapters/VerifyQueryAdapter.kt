package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ItemVerifyQueryBinding
import com.yannuo.dgcanteen.model.Verify

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/12/17 9:40
 **/
class VerifyQueryAdapter(context: Context) : BaseAdapter<Verify, ItemVerifyQueryBinding>() {
    private val mContext = context
    private val spanWatcher = SpannableStringBuilder()
    private val dishStr = StringBuilder()

    override fun getB(inflater: LayoutInflater, parent: ViewGroup): ItemVerifyQueryBinding {
        return ItemVerifyQueryBinding.inflate(inflater, parent, false)
    }

    override fun bindHolder(holder: Holder, position: Int, payloads: MutableList<Any>?) {
        bindHolder(holder, position)
    }

    override fun bindHolder(holder: Holder, position: Int) {
        val data = getData(position)
        spanWatcher.clear()
        dishStr.clear()
        data.dishesList.forEachIndexed { index, dish ->
            if (index != 0) dishStr.append("\n")
            dishStr.append(dish)
        }
        disposalData(spanWatcher, "${data.mealName} 待核销菜品:\n${dishStr}")
        holder.binding.verifyDish.text = spanWatcher
    }

    private fun disposalData(span: SpannableStringBuilder, content: String) {
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ForegroundColorSpan(mContext.resources.getColor(R.color.sky_color, null))
        } else {
            return
        }
        val start = span.length + content.indexOf(":") + 1
        span.append(content)
        span.setSpan(style, start, span.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}