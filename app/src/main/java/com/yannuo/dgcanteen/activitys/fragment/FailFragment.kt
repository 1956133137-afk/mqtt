package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.FragmentFailBinding

class FailFragment : Fragment() {
    private val TAG = javaClass.simpleName
    private var binding: FragmentFailBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFailBinding.inflate(inflater, container, false)
        return binding!!.root
    }
}