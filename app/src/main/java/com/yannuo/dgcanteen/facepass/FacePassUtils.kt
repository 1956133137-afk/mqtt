package com.yannuo.dgcanteen.facepass

import android.graphics.Bitmap
import android.text.TextUtils
import com.yannuo.dgcanteen.dao.FaceTokens
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import mcv.facepass.FacePassException
import mcv.facepass.FacePassHandler

object FacePassUtils {
    private val TAG = javaClass.simpleName
    private var addmsg = 200 //绑定人脸信息码

    private var msg = "" //绑定人脸信息


    fun unbindFaceFromGroup(handler : FacePassHandler?, faceToken: String): Boolean {
        if (handler == null) {
            LogUtil.d(TAG, "handler is null")
            return false
        }
        var result: Boolean
        try {
            if (TextUtils.isEmpty(faceToken)) return false
            val token = faceToken.toByteArray()
            result = handler.unBindGroup(Constant.GROUP_NAME, token)
            if (!result) return false
            LogUtil.i(TAG, "人脸解绑成功...")
            result = handler.deleteFace(token)
            if (!result) return false
            LogUtil.i(TAG, "人脸删除成功...")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    /***
     *
     * @param
     * @return
     */
    fun registerFaceForOne(handler : FacePassHandler?,bitmap: Bitmap?, faceTokens: FaceTokens): Boolean {
        if (bitmap == null) {
            LogUtil.e(TAG, "图片对象空...")
            return false
        }

        val token = registerFace(handler,bitmap)
        if (token == null || token == "") {
            LogUtil.e(TAG, "人脸入库失败")
            return false
        }
        faceTokens.token = token
        var result: Boolean = checkGroupExist(handler)
        if (!result) {
            LogUtil.i(TAG, "尝试创建人脸库")
            result = createFaceGroup(handler)
        }
        if (!result) {
            LogUtil.i(TAG, "创建人脸库失败")
            msg = "创建人脸库失败"
            return false
        }
        result = bindFaceToGroup(handler, token)
        if (!result) msg = "绑定人脸库失败"
        return result
    }


    /**
     * 须先注册人脸，再绑定到groupName
     */
    private fun bindFaceToGroup(handler : FacePassHandler?, token: String): Boolean {
        if (handler == null) {
            LogUtil.d(TAG, "handler is null")
            return false
        }
        var b = false
        val faceToken = token.toByteArray()
        if (faceToken.isEmpty() || TextUtils.isEmpty(Constant.GROUP_NAME)) {
            LogUtil.d(TAG, "params error！")
            return false
        }
        try {
            b = handler.bindGroup(Constant.GROUP_NAME, faceToken)
            if (b) LogUtil.i(TAG, "bind  success !") else LogUtil.e(TAG, "bind  failed !")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            LogUtil.d(TAG, e.message)
        }
        return b
    }

    fun getCode(): Int {
        return addmsg
    }

    fun getMsg(): String? {
        return msg
    }

    /**
     * 新增底库
     * @param groupName
     * @return
     */
    private fun createFaceGroup(handler : FacePassHandler?): Boolean {
        if (handler == null) {
            LogUtil.d(TAG, "handler is null")
            return false
        }
        try {
            handler.createLocalGroup(Constant.GROUP_NAME)
        } catch (e: FacePassException) {
            e.printStackTrace()
        }
        val haveGroup =  checkGroupExist(handler)
        LogUtil.d(TAG, "create group $haveGroup")
        return haveGroup
    }
    /**
     * 查找底库是否存在目标group_name
     *
     */
    private fun checkGroupExist(handler : FacePassHandler?): Boolean {
        if (handler == null) {
            LogUtil.d(TAG, "handler is null")
            return false
        }
        var isLocalGroupExist = false
        try {

            val localGroups: Array<String> = handler.localGroups
            if (localGroups.isEmpty()) {
                LogUtil.d(TAG, "底库不存在group...")
                return isLocalGroupExist
            }
            for (group in localGroups) {
                if (Constant.GROUP_NAME == group) {
                    isLocalGroupExist = true
                    break
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return isLocalGroupExist
    }

    /**
     * 整个文件夹里注册人脸
     * @param  bitmap 图片
     * @return   faceToken索引
     */
    private fun registerFace(handler : FacePassHandler?,bitmap: Bitmap): String? {
        if (handler == null) {
            msg = "句柄空"
            return null
        }
        var faceToken: String? = null
        try {
            val result = handler.addFace(bitmap)
            result?.also {
                when(it.result){
                    0->{
                        faceToken = it.faceToken.toString()
                        msg = "人脸添加成功"
                        addmsg = 200
                        LogUtil.i(TAG, "add face successfully！")
                    }
                    1->{
                        msg = "无人脸"
                        LogUtil.e(TAG, "add face fail no face !")
                        addmsg = 400001
                    }
                    else ->{
                        msg = "人脸质量不合格"
                        LogUtil.e(TAG, "add face fail quality problem !")
                        addmsg = 400002
                    }
                }
            }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
                System.gc()
            }
        } catch (e: FacePassException) {
            LogUtil.e(TAG, e.message)
        }
        return faceToken
    }

    /**
     * 删除底库groupName，对应的也删除了groupName和faceToken的绑定关系。
     * @param groupName
     * @return
     */
    fun deleteFaceLocalGroup(handler : FacePassHandler?): Boolean {
        if (handler == null) {
            LogUtil.d(TAG, "handler is null")
            return false
        }
        var isSuccess = false
        try {
            isSuccess = handler.clearAllGroupsAndFaces()

        } catch (e: FacePassException) {
            e.printStackTrace()
        }
        LogUtil.d(TAG, "删除底库:$isSuccess")
        return isSuccess
    }
}