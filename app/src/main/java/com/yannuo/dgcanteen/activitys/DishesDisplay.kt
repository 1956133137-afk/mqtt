package com.yannuo.dgcanteen.activitys

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.adapters.DishListAdapter
import com.yannuo.dgcanteen.adapters.FoodsAdapter
import com.yannuo.dgcanteen.databinding.DishesDisplayBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.Constant

/**
 * Author: filowl
 * Description: 简易异现
 * Date: 2023/8/4 15:41
 **/
class DishesDisplay(context: Context, display: Display) : Presentation(context, display) {
    private val TAG = javaClass.simpleName
    private lateinit var binding: DishesDisplayBinding
    private lateinit var kv: MMKV
    private lateinit var mAdapter :DishListAdapter




    override fun onCreate(savedInstanceState: Bundle?) {
        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = DishesDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initEvent()

    }




    private fun initView() {
        kv = MMKV.defaultMMKV()
        var strText = kv.decodeString(Constant.TITLE_CONTENT, "")
        if (strText.isNullOrEmpty().not())strText +="•"
        binding.tvFpTitle.text = "${strText}智慧食堂"

        val gridLayoutManager = GridLayoutManager(context,6)
        mAdapter = DishListAdapter(context)
        binding.rvFoods.layoutManager = gridLayoutManager
        binding.rvFoods.adapter = mAdapter
    }


    private fun initEvent() {

    }


    fun showWaitHit(show: Boolean){
        when(show){
            true -> binding.tvPayWait.visibility = View.VISIBLE
            else -> binding.tvPayWait.visibility = View.GONE
        }
    }

    fun upDataWithUi(data: MutableList<DishesInfo>, money: String, count: String) {
        mAdapter.data = data
        binding.tvAmount.text = money
        binding.tvDishesCount.text = count
    }



}