package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.FragmentScanBinding

/**
 */
class ScanFragment : Fragment() {
    private lateinit var binding: FragmentScanBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScanBinding.inflate(inflater, container, false)
        binding.tvTest.setOnClickListener {
           requireActivity().finish()
        }
        return binding.root
    }
}