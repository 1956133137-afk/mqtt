package com.yannuo.dgcanteen.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yannuo.dgcanteen.activitys.fragment.OrderMenuFragment
import com.yannuo.dgcanteen.activitys.fragment.OrderSFFragment
import com.yannuo.dgcanteen.util.LogUtil

class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    private var list: MutableList<Int>? = mutableListOf()

    private var TAG = javaClass.simpleName

    override fun getItemCount(): Int = list?.size ?:0

    override fun createFragment(position: Int): Fragment {

        LogUtil.i(TAG,"创建页面 : ${list?.get(position)}")
        return when(position){
            0->{
                OrderMenuFragment()
            }
            else->{
                OrderSFFragment()
            }

        }

    }

    override fun getItemId(position: Int): Long {
        return list?.get(position).hashCode().toLong()
    }


    fun addData(das: MutableList<Int>){
        list?.clear()
        list?.addAll(das)
        notifyDataSetChanged()
    }
}