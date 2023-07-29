package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.yannuo.dgcanteen.databinding.FragmentFaceBinding

/**
 */
class FaceFragment : Fragment(), View.OnClickListener {
    private var binding: FragmentFaceBinding? = null
    private val countDownTimer: CountDownTimer? = null
    private val CHANNEL = "" //渠道
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentFaceBinding.inflate(inflater, container, false)
        initEvent()
        return binding!!.root
    }

    private fun initEvent() {
        binding!!.btToScan.setOnClickListener(this)
        binding!!.btToSuccess.setOnClickListener(this)
        binding!!.btToFail.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == binding!!.btToScan.id) Navigation.findNavController(v).navigate(
            FaceFragmentDirections.actionFaceToScan()
        ) else if (v.id == binding!!.btToSuccess.id) Navigation.findNavController(v).navigate(
            FaceFragmentDirections.actionFaceToSuccess()
        ) else if (v.id == binding!!.btToFail.id) Navigation.findNavController(v).navigate(
            FaceFragmentDirections.actionFaceToFail()
        )
    }
}