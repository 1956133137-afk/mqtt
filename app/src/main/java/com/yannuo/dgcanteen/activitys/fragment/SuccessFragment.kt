package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.FragmentSuccessBinding
import com.yannuo.dgcanteen.model.SimpleForUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil

class SuccessFragment : Fragment() {
    private val TAG = javaClass.simpleName
    private var binding: FragmentSuccessBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSuccessBinding.inflate(inflater, container, false)
        initData()
        return binding!!.root
    }

    private fun initData() {
        val data = arguments?.getParcelable<SimpleForUI>(Constant.PAY_RESULT)
        LogUtil.d(TAG, Gson().toJson(data))
    }
}














