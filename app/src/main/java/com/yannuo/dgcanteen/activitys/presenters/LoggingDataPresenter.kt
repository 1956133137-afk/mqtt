package com.yannuo.dgcanteen.activitys.presenters

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.yannuo.dgcanteen.dao.ProductsTable
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LoggingDataPresenter(context :Context) {
    private val TAG = javaClass.simpleName
    private var cnt = context

    /**
     * 保存商品到数据库，并判断是否存在旧的照片，存在就删除掉
     * file  选择的图片文件路径
     * product  商品信息
     * oldPicName 被替换的图片名
     * update 是否需要重新拷贝图片文件
     */
    fun saveProductData(file : String?,product :ProductsTable,oldPicName :String?,update :Boolean){
        if (update) {
            file?.also {
                var fileName = it.split(File.separator).last()
                var savePath = cnt.filesDir.absolutePath + File.separator + "myPic"
                var saveFile = File(savePath)
                if (saveFile.exists().not()) {
                    saveFile.mkdirs()
                }
                //删除旧图片
                oldPicName?.apply {
                    val oldFile = File(saveFile, oldPicName)
                    if (oldFile.exists()) {
                        oldFile.delete()
                        LogUtil.d(TAG, "已删除旧图片:$oldPicName")
                    }
                }
                //拷贝新图片到商品图片目录
                var len = 0
                val buffer = ByteArray(4096)
                val file = savePath + File.separator + fileName
                val inputFile = FileInputStream(it)
                val outputFile = FileOutputStream(file)
                while (inputFile.read(buffer).also { len = it } != -1) {
                    outputFile.write(buffer, 0, len)
                }
                outputFile.flush()
                outputFile.close()
                inputFile.close()
                product.pictureName = fileName
            }
        }else{
            product.pictureName = oldPicName
        }
        DbHelper.getInstance().insertOrReplaceProduct(product)
        LogUtil.d(TAG,"保存的图片:${product.pictureName}")
    }


    fun addScanListener(listener:ScanDevice.DataCallBack?){
        ScanDevice.setCallbackListener(listener)
    }


    fun release(){

    }

    fun deleteProduct(it: ProductsTable) {
        DbHelper.getInstance().deleteProduct(it)
    }

    fun getPicturePath(it: Uri): String? {
        var imagePath :String? = null
        if (DocumentsContract.isDocumentUri(cnt, it)) {
            val docId = DocumentsContract.getDocumentId(it)
            if ("com.android.providers.media.documents".equals(it.authority)) {
                val id = docId.split(":")[1]
                val selection = MediaStore.Images.Media._ID + "=" + id
                imagePath = getImagePath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    selection
                )
            } else if ("com.android.providers.downloads.documents".equals(it.authority)) {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    docId.toLong()
                )
                imagePath = getImagePath(contentUri, null)
            } else if ("com.android.externalstorage.documents".equals(it.authority)) {
                var usbbase = Environment.getExternalStorageDirectory().absolutePath
                var index = usbbase.indexOf("/", 1)
                usbbase = usbbase.substring(0, index) + File.separator + docId.replace(
                    ":",
                    "/"
                )
                imagePath = usbbase
            }
        } else if ("content".equals(it.scheme, true)) {
            imagePath = getImagePath(it, null)
        } else if ("file".equals(it.scheme, true)) {
            imagePath = it.path
        }
        return imagePath
    }


    private fun getImagePath(uri: Uri, selection: String?): String? {
        var path :String? = null
        val cursor =  cnt.contentResolver.query(uri,null,selection,null,null)
        cursor?.also {
            if (it.moveToFirst()) {
                path = cursor.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path
    }
}