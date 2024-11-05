package com.yannuo.dgcanteen.activitys

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.DisplayUtils
import com.yannuo.dgcanteen.util.LogUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class BaseActivity<T :ViewBinding> : AppCompatActivity()  {
    lateinit var binding:T
    protected var TAG = javaClass.simpleName
    private lateinit var mHandle: CoroutineExceptionHandler
    protected lateinit var mScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        DisplayUtils.setCustomDensity(null, this, application)

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val uiOption =  //                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE //                |View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN) //

        window.decorView.systemUiVisibility = uiOption
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.statusBarColor = Color.TRANSPARENT
        supportActionBar?.hide()


        bindLayout()
        setContentView(binding.root)
        mHandle = CoroutineExceptionHandler { coroutineContext, e ->
            LogUtil.e(TAG, "Exception: ${e.message}")
        }
        mScope = CoroutineScope(Dispatchers.Default + mHandle)
//        mScope.launch {
//            getPayCfg()
//        }

        onInit()

    }




    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus){
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            val uiOption =  //                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE //                |View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN) //

            window.decorView.systemUiVisibility = uiOption
            supportActionBar?.hide()
        }
    }

    abstract fun bindLayout()

    abstract fun onInit()
    override fun onResume() {
        super.onResume()
//        mScope.launch {
//            getPayCfg()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mScope.cancel()
    }

//    private suspend fun getPayCfg() {
//        val result = PayRepositoryOfPay().getPayCfg()
//        if (result.code == "200") {
//            val mv = MMKV.defaultMMKV()
//            mv.encode(Constant.PAY_CONFIG, result.data)
//            LogUtil.i(TAG, "已更新配置信息！")
//        } else {
//            LogUtil.w(TAG, "更新配置信息失败！")
//        }
//    }
}