package com.yannuo.dgcanteen.activitys.fragment.order

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.common.MyApplication

/**
 * Author: Agoni
 * Description: Fragment基类
 **/
abstract class BaseFragment<T : ViewBinding> : Fragment() {
    val TAG = javaClass.simpleName
    lateinit var binding: T
    val handler = Handler(MyApplication.applicationContext.mainLooper)
    val kv: MMKV = MMKV.defaultMMKV()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (!this::binding.isInitialized) { //防止重复加载 onCreateView
            initFragment(inflater, container)
            initOperation()
        }
        return binding.root
    }

    abstract fun initFragment(inflater: LayoutInflater, container: ViewGroup?)     //初始化界面
    abstract fun initOperation()    //其他处理
}