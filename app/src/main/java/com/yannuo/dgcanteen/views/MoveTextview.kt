package com.yannuo.dgcanteen.views

import android.content.Context
import android.util.AttributeSet
import android.view.animation.*
import android.widget.TextView
import com.yannuo.dgcanteen.R
import java.util.jar.Attributes


class MoveTextview : androidx.appcompat.widget.AppCompatTextView {
    var anima : AnimationSet?= null

   constructor(cnt :Context ):this(cnt,null){

   }

    constructor(cnt: Context,atr :AttributeSet?):this(cnt,atr,0){

    }

    constructor(cnt: Context,atr :AttributeSet?,def : Int):super(cnt,atr,def){
       initData()
    }


    private fun initData() {

        setPadding(10,10,10,10)
        setBackgroundResource(R.drawable.common_bt_bg)
        anima = AnimationSet(false)
        anima?.interpolator = DecelerateInterpolator()
        anima?.duration = 2000
        val tranl = TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,
            Animation.RELATIVE_TO_SELF,1f,
            Animation.RELATIVE_TO_SELF,0f,
            Animation.RELATIVE_TO_SELF,0f)
        tranl.repeatMode =  2
        tranl.repeatCount = 1

        val tranl2 = TranslateAnimation(Animation.RELATIVE_TO_SELF,1f,
            Animation.RELATIVE_TO_SELF,0f,
            Animation.RELATIVE_TO_SELF,0f,
            Animation.RELATIVE_TO_SELF,0f)
        tranl2.repeatMode =  2
        tranl2.repeatCount = 1

        anima?.addAnimation(tranl)

    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        anima?.also {
            it.cancel()
           startAnimation(it)
        }

    }

    fun stopAnima(){
        anima?.cancel()
    }

}