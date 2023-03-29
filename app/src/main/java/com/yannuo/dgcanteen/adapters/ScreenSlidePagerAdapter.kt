package com.yannuo.dgcanteen.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yannuo.dgcanteen.activitys.ProductFragment

class ScreenSlidePagerAdapter(fa: FragmentActivity,list :MutableList<String>) : FragmentStateAdapter(fa) {
   var list: MutableList<String>? = list

    override fun getItemCount(): Int = list?.size ?:0

    override fun createFragment(position: Int): Fragment {
//        return ProductFragment(list?.get(position))
        return ProductFragment()
    }
}