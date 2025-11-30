package com.yannuo.dgcanteen.activitys

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.yannuo.dgcanteen.databinding.ActivityLocalFaceBinding
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.service.FaceService
import com.yannuo.dgcanteen.util.Constant
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class LocalFaceActivity: BaseActivity<ActivityLocalFaceBinding>() {

    override fun bindLayout() {
        binding = ActivityLocalFaceBinding.inflate(layoutInflater)
    }

    override fun onInit() {
//        EventBus.getDefault().register(this)
        init()
        initEvent()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        EventBus.getDefault().unregister(this)
//    }

    private fun init(){
        val intent = Intent(this, FaceService::class.java)
        startService(intent)
    }

    private fun initEvent(){
        binding.uploadFace.setOnClickListener {
            val intent = Intent(this, LocalFaceRecognitionActivity::class.java)
            startActivity(intent)
        }
        binding.facialRecognition.setOnClickListener {

        }
        binding.btnSetting.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
    }
    
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun EventBusMessageEvent(event: MessageEvent){
//        when(event.code){
//            Constant.EVENT_LOCAL_FACE -> {
//                val eventFaceBean = event.any as EventFaceBean
//                Log.d(TAG, "EventBusMessageEvent: 人脸识别结果：${eventFaceBean.code}  ${eventFaceBean.msg}")
//            }
//            Constant.EVENT_LOCAL_FACE_PATH -> {
//                val str = event.any as String
//                Log.d(TAG, "EventBusMessageEvent: 人脸活检图片路径：$str")
//                binding.facePath.setImageURI(Uri.parse(str))
//            }
//        }
//    }

}