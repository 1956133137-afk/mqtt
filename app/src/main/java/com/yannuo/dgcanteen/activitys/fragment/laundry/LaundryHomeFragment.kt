package com.yannuo.dgcanteen.activitys.fragment.laundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.fragment.order.BaseFragment
import com.yannuo.dgcanteen.activitys.viewModel.LaundryModeVM
import com.yannuo.dgcanteen.databinding.FragmentLaundryHomeBinding

class LaundryHomeFragment : BaseFragment<FragmentLaundryHomeBinding>() {

    private val placeLaundryOrderFragment = PlaceLaundryOrderFragment()
    private val pickupClothesFragment = PickupClothesFragment()
    private var activeFragment: Fragment = placeLaundryOrderFragment
    private val laundryModeVM by lazy { ViewModelProvider(requireActivity())[LaundryModeVM::class.java] }

    override fun initFragment(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentLaundryHomeBinding.inflate(inflater, container, false)
    }

    override fun initOperation() {
        val custId = arguments?.getString("custId")
        val custName = arguments?.getString("custName")
        binding.laundryDetails.text = "$custName ($custId)"

        childFragmentManager.beginTransaction().apply {
            add(R.id.laundry_content_container, pickupClothesFragment, "2").hide(pickupClothesFragment)
            add(R.id.laundry_content_container, placeLaundryOrderFragment, "1")
        }.commit()

        binding.sidebarMenu.check(R.id.btn_place_order)

        binding.sidebarMenu.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_place_order -> switchFragment(placeLaundryOrderFragment)
                R.id.btn_pickup_clothes -> switchFragment(pickupClothesFragment)
            }
        }

        binding.btnLogout.setOnClickListener {
            laundryModeVM.getUserName().value = ""
            findNavController().navigate(R.id.action_laundryHomeFragment_to_verifyLaundryUserFragment)
        }
    }

    private fun switchFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }
}