package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.FragmentDeviceInfoBinding
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant

/**
 * Author: filowl
 * Description: 设备信息
 * Date: 2023/7/29 15:48
 **/
class DeviceInfoFragment : Fragment() {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentDeviceInfoBinding
    private lateinit var kv: MMKV
    private lateinit var mPayCfg: PayCfg

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceInfoBinding.inflate(inflater, container, false)
        initObject()
        initData()
        return binding.root
    }

    private fun initObject() {
        kv = MMKV.defaultMMKV()
        mPayCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)!!
    }

    private fun initData() {
//        binding.businessName.text = mPayCfg.business_name
        binding.businessId.text = mPayCfg.businessId
        binding.campusId.text = mPayCfg.campusId
        binding.vposId.text = mPayCfg.counterId
        binding.serialNumber.text = CommonAndDpToPxUtil.getDeviceSerial()
    }
}