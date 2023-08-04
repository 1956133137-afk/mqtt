package com.yannuo.dgcanteen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.yannuo.dgcanteen.R

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/7 14:24
 **/
class SimpleDownAdapter(context: Context, dataList: ArrayList<String>) : BaseAdapter() {
    private val mContext: Context
    private val mDataList: ArrayList<String>

    init {
        mContext = context
        mDataList = dataList
    }

    override fun getCount(): Int {
        return mDataList.size
    }

    override fun getItem(position: Int): Any {
        return mDataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_drop_down, null)
            holder = ViewHolder()
            holder.selectOther = convertView!!.findViewById<TextView>(R.id.select_other)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.selectOther!!.text = mDataList[position]
        return convertView
    }

    internal inner class ViewHolder {
        var selectOther: TextView? = null
    }
}