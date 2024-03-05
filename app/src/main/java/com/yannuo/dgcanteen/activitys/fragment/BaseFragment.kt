package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.yannuo.dgcanteen.util.LogUtil

abstract class BaseFragment<T : ViewBinding> : Fragment() {
    lateinit var binding:T
    protected var TAG = javaClass.simpleName
    private var isCreateView = false
    private var lastVisiableState = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCreateView = true
        bindLayout(inflater,container)
        if (isCreateView && isVisible){

            dispatchUserVisibleEvent(true)
        }
        onInit()
        return binding.root
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        LogUtil.i(TAG,"可见 $isVisibleToUser")
        if (isCreateView){
            dispatchUserVisibleEvent(isVisibleToUser)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!lastVisiableState){
            dispatchUserVisibleEvent(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (lastVisiableState){
            dispatchUserVisibleEvent(false)
        }
    }

    private fun dispatchUserVisibleEvent(event :Boolean){
        lastVisiableState = event
        when(event){
            true ->{
                onFgLoadStart()
            }
            false ->{
                onFgLoadStop()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isCreateView = false
    }

    abstract fun bindLayout(inflater: LayoutInflater, container: ViewGroup?)

    open fun onInit(){}

    open fun onFgLoadStart(){}

    open fun onFgLoadStop(){}
}