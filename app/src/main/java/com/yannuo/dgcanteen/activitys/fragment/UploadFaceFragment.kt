package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.EnrollFaceActivity
import com.yannuo.dgcanteen.activitys.SettingActivity
import com.yannuo.dgcanteen.activitys.viewModel.FaceVM
import com.yannuo.dgcanteen.adapters.UserPersonsAdapter
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentUploadFaceBinding
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FacePass
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.greendao.entity.UserFaceData
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.Arrays

class UploadFaceFragment : BaseFragment<FragmentUploadFaceBinding>(),UserPersonsAdapter.OnClickListener,FaceVM.OnListener {
    private var userPersonsAdapter: UserPersonsAdapter? = null
    private var facePass: FacePass? = null
    private val faceVM: FaceVM by lazy { FaceVM() }
    private var persons: Persons? = null
    private val kv = MMKV.defaultMMKV()

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentUploadFaceBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        initObject()
        initEvent()
    }

    // 1. 注册 ActivityResultLauncher
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            // 3. 处理从 Activity 返回的数据
            val returnedData = data?.getStringExtra("face_path")
            returnedData?.let {
                // 在这里使用返回的数据，例如更新 UI
                // textView.text = it
                Log.d(TAG, "获取到activity人脸数据: $it")
                binding.faceImg.setImageURI(Uri.parse(it))
            }
        }
    }

    fun initObject(){
        userPersonsAdapter = UserPersonsAdapter()
        binding.userRec.adapter = userPersonsAdapter
        binding.userRec.layoutManager = LinearLayoutManager(requireContext())
        userPersonsAdapter?.setListener(this)
        faceVM.setListener(this)
        facePass = FaceSDKHelper.getInstance().getCameraManager()?.getFacePass()
    }

    fun initEvent(){
        //查询人员信息
        binding.queryOrder.setOnClickListener {
            val userName = binding.userName.text.toString()
            val userPhone = binding.userPhone.text.toString()
            val queryPersonToName = DishesDBHelper.getInstance().queryPersonToName(userName, userPhone)
            userPersonsAdapter?.data = queryPersonToName
        }
        //录入人脸
        binding.getFace.setOnClickListener {
            if(!kv.decodeBool(Constant.OPEN_LOCAL_FACE,false)){
                ToastShowUtil.show("尚未开启本地脸库，无法使用")
                return@setOnClickListener
            }
            if(this.persons == null){
                ToastShowUtil.show("请先选择需要录入的人员")
                return@setOnClickListener
            }
            val intent = Intent(requireContext(), EnrollFaceActivity::class.java)
            startForResult.launch(intent)
            Handler(MyApplication.applicationContext.mainLooper).postDelayed({
                val settingActivity = requireActivity() as SettingActivity
                settingActivity.settingDisplay.hide()
            },300)
        }
        //上传人脸
        binding.uploadFace.setOnClickListener {
            val drawable = binding.faceImg.drawable
            if(drawable == null){
                ToastShowUtil.show("请先录入人脸")
                return@setOnClickListener
            }
            val bitmapDrawable = binding.faceImg.drawable as BitmapDrawable
            val bitmap = bitmapDrawable.bitmap
            //抽取特征值
            val extractFeature = facePass?.extractFeature(bitmap)
            if(extractFeature == null){
                ToastShowUtil.show("人脸特征值提取失败")
                return@setOnClickListener
            }
            //人脸特征值token
            val eigenvalue = Arrays.toString(extractFeature)
            Log.d(TAG, "initEvent: 提取特征值：$eigenvalue")
            //注册人脸
            val token = facePass?.registerFaces(eigenvalue)
            Log.d(TAG, "initEvent: 人脸特征值ID：$token")
            if(token.isNullOrEmpty()){
                ToastShowUtil.show("人脸注册失败，请重新录入人脸")
            }else{
                //特征值绑定人脸底库
                val bindFaceToGroup = facePass?.bindFaceToGroup(token)
                if(bindFaceToGroup == true){
                    //删除原先的人脸
                    val queryFaceByCustId = DishesDBHelper.getInstance().queryFaceByCustId(persons?.custId)
                    if(queryFaceByCustId != null){
                        if(queryFaceByCustId.eigenvalue != null){
                            val deleteFace = facePass?.deleteFace(queryFaceByCustId.eigenvalue)
                            DishesDBHelper.getInstance().deleteFaceByCustId(persons?.custId)
                            Log.d(TAG, "initEvent: 删除旧人脸：$deleteFace")
                        }
                    }
                    Log.d(TAG, "initEvent: 入库成功，保存本地数据")
                    val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
                    val faceData = UserFaceData().apply {
                        this.custId = persons?.custId
                        this.userId = persons?.userId
                        this.campusId = payCfg?.campusId
                        this.eigenvalue = token
                        this.way = "3"
                        this.version = FaceSDKHelper.getInstance().getCameraManager()?.getFacePass()?.getVersion() ?: ""
                        this.updateTime = TimeUtil.timeFormat("yyyy-MM-dd HH:mm:ss",System.currentTimeMillis())
                    }
                    Log.d(TAG, "initEvent: 入库的人脸信息：${Gson().toJson(faceData)}")
                    DishesDBHelper.getInstance().insertFaceData(faceData)
                    ToastShowUtil.show("人脸绑定成功，正在上传...")
                    val bitmapToBase64 = CameraUtil.instance.bitmapToBase64(bitmap)
                    faceVM.uploadFaceImage(this.persons!!,bitmapToBase64)
                    faceVM.uploadFaceToken(this.persons!!,eigenvalue)
                }else{
                    ToastShowUtil.show("人脸绑定失败，请重新录入人脸")
                }
            }
        }
    }

    /**
     * 更新人员信息
     */
    private fun uploadUser(persons: Persons){
        binding.name.text = persons.personName
        binding.grade.text = "${persons.grade}${persons.userClass}"
        binding.phone.text = persons.phone
    }

    /**
     * 清除人员信息
     */
    private fun removeUser(){
        binding.name.text = ""
        binding.grade.text = ""
        binding.phone.text = ""
        binding.faceImg.setImageDrawable(null)
        binding.uploadText.text = ""
    }

    /**
     * 当前人员信息
     */
    override fun clickUserPersons(persons: Persons) {
        removeUser()
        this.persons = persons
        uploadUser(persons)
    }

    /**
     * 人脸上传结果
     */
    override fun uploadResult(code: Int, type: Boolean, msg: String) {
        Handler(MyApplication.applicationContext.mainLooper).post {
            when(code){
                1 -> {
                    if(type){
                        binding.uploadImgText.text = "人脸图片上传成功"
                        binding.uploadImgText.setTextColor(resources.getColor(R.color.pass_color))
                    }else{
                        binding.uploadImgText.text = "人脸图片上传失败: $msg"
                        binding.uploadImgText.setTextColor(resources.getColor(R.color.red))
                    }
                }
                else -> {
                    if(type){
                        binding.uploadText.text = "特征值上传成功"
                        binding.uploadText.setTextColor(resources.getColor(R.color.pass_color))
                    }else{
                        binding.uploadText.text = "特征值上传失败: $msg"
                        binding.uploadText.setTextColor(resources.getColor(R.color.red))
                    }
                }
            }
        }
    }

    override fun facePayResult(payForUI: PayForUI) {

    }
}