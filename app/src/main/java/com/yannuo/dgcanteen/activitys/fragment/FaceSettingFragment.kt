package com.yannuo.dgcanteen.activitys.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.FragmentFaceSettingBinding
import com.yannuo.dgcanteen.util.Constant

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/29 15:48
 **/
class FaceSettingFragment : Fragment(), SeekBar.OnSeekBarChangeListener {
    private val TAG = javaClass.simpleName

    private lateinit var binding: FragmentFaceSettingBinding
    private lateinit var kv: MMKV

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFaceSettingBinding.inflate(inflater, container, false)
        initObject()
        initData()
        initEvent()
        return binding.root
    }

    private fun initObject() {
        kv = MMKV.defaultMMKV()
    }

    private fun initData() {
        //相机偏航角
        binding.cameraYaw.setText(kv.decodeInt(Constant.YAW_SET, Constant.YAW_SET_V.toInt()).toString())
        binding.seekBarYaw.progress = kv.decodeInt(Constant.YAW_SET, Constant.YAW_SET_V.toInt())
        //相机俯仰角
        binding.cameraPith.setText(kv.decodeInt(Constant.PITCH_SET, Constant.PITCH_SET_V.toInt()).toString())
        binding.seekBarPith.progress = kv.decodeInt(Constant.PITCH_SET, Constant.PITCH_SET_V.toInt())
        //相机翻滚角
        binding.cameraRoll.setText(kv.decodeInt(Constant.ROLL_SET, Constant.ROLL_SET_V.toInt()).toString())
        binding.seekBarRoll.progress = kv.decodeInt(Constant.ROLL_SET, Constant.ROLL_SET_V.toInt())
        //相机旋转角
        binding.cameraRotate.setText(kv.decodeInt(Constant.ROTATE_SET, Constant.ROTATE_SET_V).toString())
        binding.seekBarRotate.progress = kv.decodeInt(Constant.ROTATE_SET, Constant.ROTATE_SET_V) / 90
        //预览角度
        binding.cameraPvAg.setText(kv.decodeInt(Constant.PRE_ANGLE_SET, Constant.PRE_ANGLE_SET_V).toString())
        binding.seekBarPvAg.progress = kv.decodeInt(Constant.PRE_ANGLE_SET, Constant.PRE_ANGLE_SET_V) / 90
        //相机镜像
        binding.switchMirror.isChecked = kv.decodeBool(Constant.MIRROR_SET, Constant.MIRROR_SET_V)
        //人脸识别语音播报
        binding.voiceEn.isChecked = kv.decodeBool(Constant.VOICE_ENABLE_SET, Constant.VOICE_ENABLE_SET_V)
        //识别距离阈值
        binding.distance.setText(kv.decodeFloat(Constant.DISTANCE_SET, Constant.DISTANCE_SET_V).toString())
        binding.seekBarDistance.progress = (kv.decodeFloat(Constant.DISTANCE_SET, Constant.DISTANCE_SET_V) * 10).toInt()
        //活检开关
        binding.liveEn.isChecked = kv.decodeBool(Constant.LIVE_ENABLE_SET, Constant.LIVE_ENABLE_SET_V)
        //活检阈值
        binding.liveVl.setText(kv.decodeInt(Constant.RECOGNIZE_VALUE_SET, Constant.RECOGNIZE_VALUE_SET_V.toInt()).toString())
        binding.seekBarLiveVl.progress = kv.decodeInt(Constant.RECOGNIZE_VALUE_SET, Constant.RECOGNIZE_VALUE_SET_V.toInt())
    }

    private fun initEvent() {
        binding.seekBarYaw.setOnSeekBarChangeListener(this)
        binding.seekBarPith.setOnSeekBarChangeListener(this)
        binding.seekBarRoll.setOnSeekBarChangeListener(this)
        binding.seekBarRotate.setOnSeekBarChangeListener(this)
        binding.seekBarPvAg.setOnSeekBarChangeListener(this)
        binding.seekBarDistance.setOnSeekBarChangeListener(this)
        binding.seekBarLiveVl.setOnSeekBarChangeListener(this)
    }

    fun save() {
        kv.encode(Constant.YAW_SET, binding.seekBarYaw.progress)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, boolean: Boolean) {
        when (seekBar.id) {
            R.id.seekBar_yaw -> binding.cameraYaw.setText(progress.toString())
            R.id.seekBar_pith -> binding.cameraPith.setText(progress.toString())
            R.id.seekBar_roll -> binding.cameraRoll.setText(progress.toString())
            R.id.seekBar_rotate -> binding.cameraRotate.setText((progress * 90).toString())
            R.id.seekBar_pv_ag -> binding.cameraPvAg.setText((progress * 90).toString())
            R.id.seekBar_distance -> binding.distance.setText((progress / 10.0).toString())
            R.id.seekBar_live_vl -> binding.liveVl.setText(progress.toString())
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {} //开始

    override fun onStopTrackingTouch(seekBar: SeekBar?) {} //结束

}