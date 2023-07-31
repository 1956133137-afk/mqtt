package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.FragmentCameraSettingBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/29 15:48
 **/
class CameraSettingFragment : Fragment() {
    private lateinit var binding: FragmentCameraSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun save() {

    }

}