package com.yannuo.dgcanteen.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.TextView
import com.yannuo.dgcanteen.R
import kotlinx.coroutines.*

class LoadingDialog(context: Context) : BaseDialog(context,R.layout.dialog_laoding) {
    //       private lateinit var binding :DialogLaodingBinding
    private var scope = CoroutineScope(Dispatchers.Main )
    private lateinit var tv_hit :TextView


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
////        binding = DialogLaodingBinding.inflate(layoutInflater)
//        setContentView(R.layout.dialog_laoding)
//        tv_hit = findViewById(R.id.tv_hit_str)
//        ////         透明背景
//        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        setCancelable(false)
//
//    }

    override fun initViewAndEvent() {
        tv_hit = findViewById(R.id.tv_hit_str)
        ////         透明背景
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
    }

    private fun updateText(){
        scope.launch {
            val str = tv_hit.text
            var value = 0
            while (isActive) {
                delay(200)
                value++
                tv_hit.text = str.substring(0,str.length - 3 + value%4)
            }
        }
    }

     fun show(str : String) {
        if (!isShowing) {
            super.show()
        }
         if (str.isEmpty())return
         tv_hit.text = "$str..."
    }

    fun changeText(str : String){
        if (str.isEmpty())return
        tv_hit.text = "$str..."
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateText()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }



}