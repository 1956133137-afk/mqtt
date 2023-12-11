package com.yannuo.dgcanteen.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.yannuo.dgcanteen.R

class LoadingDialog2(context: Context) : Dialog(context) {
    //       private lateinit var binding :DialogLaodingBinding

    private  var tv_hit :ImageView ?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        binding = DialogLaodingBinding.inflate(layoutInflater)
        setContentView(R.layout.dialog_laoding2)
        tv_hit = findViewById(R.id.tv_hit_str2)
        ////         透明背景
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)

    }


    override fun show() {
        if (isShowing)return
        super.show()
        val rocketAnimation = tv_hit?.background
        if (rocketAnimation is Animatable) {
            rocketAnimation.start()
        }
    }

    override fun dismiss() {
        super.dismiss()
        val rocketAnimation = tv_hit?.background
        if (rocketAnimation is Animatable) {
            rocketAnimation.stop()
        }
    }







}