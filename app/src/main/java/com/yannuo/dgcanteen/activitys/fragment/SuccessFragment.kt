package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.FragmentSuccessBinding

class SuccessFragment : Fragment() {
    private val TAG = javaClass.simpleName
    private var binding: FragmentSuccessBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSuccessBinding.inflate(inflater, container, false)
        return binding!!.root
    }
}