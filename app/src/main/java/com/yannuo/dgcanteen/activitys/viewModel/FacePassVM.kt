package com.yannuo.dgcanteen.activitys.viewModel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.facepass.CameraUtil
import com.yannuo.dgcanteen.facepass.FaceResultListener
import com.yannuo.dgcanteen.facepass.FaceSDKHelper
import com.yannuo.dgcanteen.model.EncryptedDataRequest
import com.yannuo.dgcanteen.model.EventFaceBean
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mcv.facepass.types.FacePassRecognitionResult

class FacePassVM: ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv: MMKV = MMKV.defaultMMKV()
    private val mRespository by lazy { PayRepositoryOfPay() }

    fun localScanFacePayment(){
        viewModelScope.launch(Dispatchers.IO){
            val res = mRespository.localScanFacePayment(EncryptedDataRequest(""))
        }
    }


}