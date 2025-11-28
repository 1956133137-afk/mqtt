package com.yannuo.dgcanteen.activitys.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.Persons
import com.yannuo.dgcanteen.greendao.entity.UserFaceData
import com.yannuo.dgcanteen.model.EncryptedDataRequest
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.UploadFaceImageRequest
import com.yannuo.dgcanteen.model.UploadFaceRequest
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.DES3CBCUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FaceVM: ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private var listener: onListener? = null
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()

    fun setListener(listener: onListener){
        this.listener = listener
    }

    /**
     * 上传人脸特征值
     */
    fun uploadFaceToken(persons: Persons, eigenvalue: String){
        viewModelScope.launch(Dispatchers.Main) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
            if(payCfg == null){
                ToastShowUtil.show("未获取到配置信息")
                return@launch
            }
            val bean = UploadFaceRequest().apply {
                this.userId = persons.userId
                this.custId = persons.custId
                this.campusId = payCfg.campusId
                this.eigenvalue = eigenvalue
                this.version = "1.1"
            }
            Log.d(TAG, "uploadFaceToken: 未加密上传数据：${Gson().toJson(bean)}")
            val encryption = DES3CBCUtil.encryption(Gson().toJson(bean))
            val data = mRepository.uploadUserEigenvalue(EncryptedDataRequest(encryption))
            if(data.code == "200"){
                //解密
                val decryptRSA = DES3CBCUtil.decryptRSA(data.data)
                val fromJson = Gson().fromJson(decryptRSA, UserFaceData::class.java)
                //更新人员信息
                DishesDBHelper.getInstance().insertFaceData(fromJson)
                listener?.uploadResult(2,true, "上传成功")
            }else{
                LogUtil.e(TAG,"人脸特征值上传异常：${data.code} - ${data.msg}")
                listener?.uploadResult(2,false, data.msg)
            }
        }
    }

    /**
     * 上传人脸图片
     */
    fun UploadFaceImage(persons: Persons, img: String){
        viewModelScope.launch(Dispatchers.Main) {
            val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java)
            if(payCfg == null){
                ToastShowUtil.show("未获取到配置信息")
                return@launch
            }
            val bean = UploadFaceImageRequest().apply {
                this.userId = persons.userId
                this.custId = persons.custId
                this.campusId = payCfg.campusId
                this.faceBase64 = img
            }
            Log.d(TAG, "uploadFaceToken: 未加密上传数据：${Gson().toJson(bean)}")
            val encryption = DES3CBCUtil.encryption(Gson().toJson(bean))
            val data = mRepository.uploadFaceDBImg(EncryptedDataRequest(encryption))
            if(data.code == "200"){
                //解密
                val decryptRSA = DES3CBCUtil.decryptRSA(data.data)
                val fromJson = Gson().fromJson(decryptRSA, UserFaceData::class.java)
                //更新人员信息
                DishesDBHelper.getInstance().insertFaceData(fromJson)
                listener?.uploadResult(1,true, "上传成功")
            }else{
                LogUtil.e(TAG,"人脸图片上传异常：${data.code} - ${data.msg}")
                listener?.uploadResult(1,false, data.msg)
            }
        }
    }

    interface onListener{
        /**
         * 特征值上传结果
         */
        fun uploadResult(code: Int, type: Boolean,msg: String)

    }

}