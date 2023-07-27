package com.yannuo.dgcanteen.activitys

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yannuo.dgcanteen.databinding.ItemInputKeyboardBinding

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/27 17:26
 **/
class KeyBoardFragment : Fragment() {

    private lateinit var binding: ItemInputKeyboardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ItemInputKeyboardBinding.inflate(inflater, container, false)
        initObject()
        initEvent()

        return binding.root
    }

    private fun initObject() {

    }

    private fun initEvent() {

    }

}