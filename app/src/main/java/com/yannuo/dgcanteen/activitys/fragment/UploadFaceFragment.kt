package com.yannuo.dgcanteen.activitys.fragment

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.EnrollFaceActivity
import com.yannuo.dgcanteen.activitys.FacialRecognitionActivity
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.activitys.viewModel.FacePassVM
import com.yannuo.dgcanteen.activitys.viewModel.FaceVM
import com.yannuo.dgcanteen.adapters.UserPersonsAdapter
import com.yannuo.dgcanteen.databinding.FragmentUploadFaceBinding
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FacePass
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.util.Base64Util
import com.yannuo.dgcanteen.util.ToastShowUtil
import java.util.Arrays

class UploadFaceFragment : BaseFragment<FragmentUploadFaceBinding>(),UserPersonsAdapter.OnClickListener,FaceVM.onListener {
    private var userPersonsAdapter: UserPersonsAdapter? = null
    private var facePass: FacePass? = null
    private val faceVM: FaceVM by lazy { FaceVM() }
    private var persons: Persons? = null

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
            if(this.persons == null){
                ToastShowUtil.show("请先选择需要录入的人员")
                return@setOnClickListener
            }
            val intent = Intent(requireContext(), EnrollFaceActivity::class.java)
            startForResult.launch(intent)
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
            //人脸特征值token
            val eigenvalue = Arrays.toString(extractFeature)
            Log.d(TAG, "initEvent: 提取特征值：$eigenvalue")
            if(eigenvalue.isEmpty()){
                ToastShowUtil.show("人脸特征值提取失败")
                return@setOnClickListener
            }
            //注册人脸
            val token = facePass?.registerFaces(eigenvalue)
            if(token.isNullOrEmpty()){
                ToastShowUtil.show("人脸注册失败，请重新录入人脸")
            }else{
                //特征值绑定人脸底库
                val bindFaceToGroup = facePass?.bindFaceToGroup(token)
                if(bindFaceToGroup == true){
                    ToastShowUtil.show("人脸绑定成功，正在上传...")
                    val bitmapToBase64 = CameraUtil.instance.bitmapToBase64(bitmap)
                    faceVM.UploadFaceImage(this.persons!!,bitmapToBase64)
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