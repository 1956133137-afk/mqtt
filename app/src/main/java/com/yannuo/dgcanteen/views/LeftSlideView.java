package com.yannuo.dgcanteen.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.yannuo.dgcanteen.R;

import androidx.recyclerview.widget.RecyclerView;

public class LeftSlideView extends RecyclerView {
    private boolean once = false ;
    private int maxLength, mTouchSlop;
    private View mTextView_Delete;
    private int mScrollWidth;
    private String TAG = getClass().getSimpleName();


    private Context mContext;

    public LeftSlideView(Context context) {
        this(context,null);
    }

    public LeftSlideView(Context context, AttributeSet attrs) {
        super(context, attrs,0);
    }

    public LeftSlideView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
         mContext = context;

    }


}
