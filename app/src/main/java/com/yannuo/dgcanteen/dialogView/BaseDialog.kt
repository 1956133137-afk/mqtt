package com.yannuo.dgcanteen.dialogView

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

/**
 * Author: filowl
 * Description: 弹窗基类
 * Date: 2023/6/8 10:02
 **/
abstract class BaseDialog<T : ViewBinding>(context: Context) : Dialog(context) {
    private val TAG = javaClass.simpleName
    lateinit var binding: T
    private val mContext = context

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "CoroutineExceptionHandler $throwable ${throwable.message}")
    }
    var mScope = CoroutineScope(Dispatchers.Main + mHandler)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (mContext !is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY - 1)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            else window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT or WindowManager.LayoutParams.TYPE_PHONE)
        }
        super.onCreate(savedInstanceState)
        initDialogView()
        setContentView(binding.root)

        initDialogStyle()
        initOperation()
    }

    abstract fun initDialogView()   //初始化弹窗界面
    abstract fun initOperation()    //其他处理

    private fun initDialogStyle() {  //设置弹出框样式
        //点击弹窗的外部不关闭
        setCancelable(false)
        //隐藏背景
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //弹出框大小
        window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)
//        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
    }

    //隐藏状态栏和底部导航栏
    private fun fullScreenImmersive() {
//        window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val uiOptions: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            window?.decorView?.setSystemUiVisibility(uiOptions)
        }
    }

    override fun show() {
        window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        super.show()
        fullScreenImmersive()
//        window?.decorView?.setOnSystemUiVisibilityChangeListener { fullScreenImmersive() }
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun onDetachedFromWindow() {
        mScope.cancel()
        super.onDetachedFromWindow()
    }
}