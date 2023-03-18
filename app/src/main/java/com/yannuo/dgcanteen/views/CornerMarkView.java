package com.yannuo.dgcanteen.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.util.LogUtil;

public class CornerMarkView extends FrameLayout {
    private int currentValue = 0; //当前的角标值
    private String TAG  =getClass().getSimpleName();
    private float mMainSize;
    private float mSecondarySize;
    private int myPadding  = 3;
    private TextView mSecondView;

    public CornerMarkView(@NonNull Context context) {
        this(context,null);
    }

    public CornerMarkView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CornerMarkView(@NonNull Context cnt, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(cnt, attrs, defStyleAttr);
        init(cnt,attrs);
    }

    private void init(Context cnt, AttributeSet attrs){
        TypedArray attrsArray = cnt.obtainStyledAttributes(attrs, R.styleable.corner);
        mMainSize = attrsArray.getDimension(R.styleable.corner_mainSize, 55);
        mSecondarySize = attrsArray.getDimension(R.styleable.corner_secondarySize, 20);
        Drawable mainBg = attrsArray.getDrawable(R.styleable.corner_mainBg);
        Drawable secondaryBg = attrsArray.getDrawable(R.styleable.corner_secondaryBg);

        LogUtil.i(TAG,"mainSize:"+ mMainSize +" secondarySize:"+ mSecondarySize);
        ImageView mainView = new ImageView(cnt);
        mSecondView = new TextView(cnt);
        mSecondView.setTextSize(mSecondarySize);
        mSecondView.setBackground(secondaryBg);
        mSecondView.setTextColor(Color.WHITE);
        mSecondView.setText("0");
        mSecondView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mSecondView.setGravity(Gravity.CENTER);
        int lTextSize = (int) mSecondView.getTextSize();
        mSecondarySize +=lTextSize;

//        mainView.setScaleType(ImageView.ScaleType.FIT_XY);
        mainView.setImageDrawable(mainBg);
        LayoutParams mainParams = new LayoutParams((int)mMainSize, (int)mMainSize);
//        mainParams.width = (int) mMainSize;
//        mainParams.height = (int) mMainSize;
        mainView.setLayoutParams(mainParams);

//        ViewGroup.LayoutParams secondaryParams = secondView.getLayoutParams();
//        secondaryParams.width = (int) mSecondarySize;
//        secondaryParams.height = (int) mSecondarySize;
        LayoutParams secondaryParams = new LayoutParams((int)mSecondarySize, (int)mSecondarySize);
        mSecondView.setLayoutParams(secondaryParams);
        attrsArray.recycle();
        addView(mainView);
//        addView(secondView);

//        initEvent();
//        setOnClickListener(this);
    }

    public void updateValue(int value){
        if (value <=0 && currentValue==0)return;
        if (value <=0){
            currentValue = 0;
            removeView(mSecondView);
            requestLayout();
        }else {
            if (getChildCount() <2){
                addView(mSecondView);
                requestLayout();
            }
            currentValue = value;
            mSecondView.setText(String.valueOf(currentValue));
        }
    }


//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (ev.getAction() ==MotionEvent.ACTION_DOWN)
//            return true;
//        return super.dispatchTouchEvent(ev);
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int padding = myPadding;
//        if (widthMode == MeasureSpec.UNSPECIFIED || heightMeasureSpec == MeasureSpec.UNSPECIFIED) {
        //计算宽度//计算需要的高度
        padding = (int) (mMainSize + (padding * 2) + mSecondarySize / 6);
//        padding = (int) (mMainSize + (padding * 2) );
//        padding = (int) (mMainSize + (padding * 2) );
//            heightMode = widthMode;
//        }
        setMeasuredDimension(padding, padding);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        super.onLayout(changed, left, top, right, bottom);
        int lTop  = myPadding;
        int lLeft = myPadding;
        int childCount = getChildCount();
        for (int i=0 ;i< childCount ;i++){
            View lChildAt = getChildAt(i);
            if (lChildAt instanceof ImageView) {
                lTop = (int) (lTop +  mSecondarySize/6);
                lChildAt.layout(lLeft, lTop,lLeft +lChildAt.getMeasuredWidth(), lTop +lChildAt.getMeasuredHeight());
                lLeft = lLeft + lChildAt.getMeasuredWidth();
            }
            else if (lChildAt instanceof TextView){
                lTop = (int) ( lTop  - mSecondarySize / 6);
                lLeft = lLeft - (lChildAt.getMeasuredWidth() /6) *5;

                lChildAt.layout(lLeft,lTop,lLeft+ lChildAt.getMeasuredWidth(),lTop+ lChildAt.getMeasuredHeight());
            }
        }
    }

//    @Override
//    public void onClick(View v) {
//
//    }
}
