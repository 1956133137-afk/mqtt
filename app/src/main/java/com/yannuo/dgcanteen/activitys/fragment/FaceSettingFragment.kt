package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.FragmentFaceSettingBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/29 15:48
 **/
class FaceSettingFragment : Fragment() {
    private lateinit var binding: FragmentFaceSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFaceSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun save() {

    }

}