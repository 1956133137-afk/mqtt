package com.yannuo.dgcanteen.views

import android.content.Context
import android.util.AttributeSet
import android.view.animation.*
import com.yannuo.dgcanteen.R

class MoveTextview2 : androidx.appcompat.widget.AppCompatTextView {
    private var animationSet: AnimationSet? = null

    constructor(cnt: Context) : this(cnt, null)

    constructor(cnt: Context, atr: AttributeSet?) : this(cnt, atr, 0)

    constructor(cnt: Context, atr: AttributeSet?, def: Int) : super(cnt, atr, def) {
        initData()
    }

    private fun initData() {
        setPadding(10, 10, 10, 10)
        setBackgroundResource(R.drawable.common_bt_bg)
        animationSet = AnimationSet(false)
//        animationSet?.interpolator = DecelerateInterpolator()

        val showAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1f,
            Animation.RELATIVE_TO_SELF, 1f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        showAnimation.duration = 2000

        animationSet?.addAnimation(showAnimation)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        if (animationSet != null) {
            animationSet?.cancel()
            startAnimation(animationSet)
        }
    }

    fun stopAnima() {
        animationSet?.cancel()
    }

}