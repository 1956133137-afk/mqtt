package com.yannuo.dgcanteen.activitys.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.databinding.FragmentFaceVerificationBinding
import com.yannuo.dgcanteen.model.VerificationUI

class FaceVerificationFragment : BaseFragment<FragmentFaceVerificationBinding>() {
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    private val viewModel by lazy {
        VerificationVM()
    }
    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentFaceVerificationBinding.inflate(inflater, container, false)
        initObject()
    }

    private fun initObject() {
        if (requireActivity().intent.extras?.getString("verify") != null) {
            val verify = requireActivity().intent.extras?.getString("verify")
            val verificationUI = Gson().fromJson(verify, VerificationUI::class.java)
            val skip = requireActivity().intent.extras?.getInt("id")
            if (skip == 0) {
                val toSuccess = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToShowDishFragment(verificationUI)
                findNavController().navigate(toSuccess)
            }else {
                val toFailed = FaceVerificationFragmentDirections.actionFaceVerificationFragmentToFailedFragment(verificationUI)
                findNavController().navigate(toFailed)
            }
        }
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
    }
}