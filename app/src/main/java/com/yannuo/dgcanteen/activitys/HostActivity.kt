package com.yannuo.dgcanteen.activitys

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ActivityHostBinding
import com.yannuo.dgcanteen.util.Constant


class HostActivity : AppCompatActivity() {
    private lateinit var binding : ActivityHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        initSystemBar()
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener(object : OnSystemUiVisibilityChangeListener{
            override fun onSystemUiVisibilityChange(visibility: Int) {
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    initSystemBar()
                }
            }
        })
        setContentView(binding.root)
//        intent.getParcelableExtra<>()
//        val bundle = intent.(Constant.PAY_DATE) ?: throw Throwable("非法参数")

        val navHostFragment =
            supportFragmentManager.findFragmentById(binding.mainFragmentContainer.id) as NavHostFragment?
        navHostFragment!!.navController.navigate(R.id.scanFragment)

    }


    override fun onStart() {
        super.onStart()
//        val cl = binding.mainFragmentContainer.findNavController()
////         findNavController()
//        cl.navigate(R.id.scanFragment)
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        initSystemBar()
    }

    private fun initSystemBar() {
        val window = window
        val uiOption = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //                |View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        window.decorView.systemUiVisibility = uiOption
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    override fun onBackPressed() {
    }





}


