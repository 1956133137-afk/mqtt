package com.yannuo.dgcanteen.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.util.LogUtil;


public class AddDeleteCountView extends LinearLayout {
    private String TAG = getClass().getSimpleName();
    private TextView mTvCount;
    private ImageView mIBDel;
    private ImageView mIBAdd;
    //
    private short mCountTotal;
    private short mMiddleResult;
    private IAddDeleteCountViewClickListener  mListener;
    private byte mode;

    public AddDeleteCountView(Context context) {
        super(context);
        init( context);
        initEven();
    }

    public AddDeleteCountView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init( context);
        initEven();
    }

    public AddDeleteCountView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init( context);
        initEven();
    }

   private void init(Context context){
       mTvCount = new TextView(context);
       mIBDel = new ImageView(context);
       mIBAdd = new ImageView(context);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

       mIBDel.setBackgroundResource(R.drawable.reduce);
       mIBAdd.setBackgroundResource(R.drawable.add_icon);
       LayoutParams layoutParams = new LayoutParams(40, 40);
       mIBDel.setLayoutParams(layoutParams);
       LayoutParams layoutParams2 = new LayoutParams(40, 40);
//
       mIBAdd.setLayoutParams(layoutParams2);
       LayoutParams layoutParams3 = new LayoutParams(55, 50);
       mTvCount.setLayoutParams(layoutParams3);
       mTvCount.setGravity(Gravity.CENTER);
       mTvCount.setTextSize(20);

       addView(mIBDel);
       addView(mTvCount);
       addView(mIBAdd);
   }

   private void initEven(){
        mIBAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCount(true);
            }
        });

        mIBDel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCount(false);
            }
        });
   }

    private void changeCount(boolean value){
        if (value){
            mMiddleResult+=1;
            if (mMiddleResult > mCountTotal){
                if (mode == 0) mMiddleResult = mCountTotal;
            }

        }else {
            mMiddleResult -=1;
            if (mMiddleResult < 0)mMiddleResult = 0;
        }
        mTvCount.setText(String.valueOf(mMiddleResult));
        if (mListener != null) {
            mListener.changeValue(mMiddleResult);
            LogUtil.i(TAG,"select count is --> "+ mMiddleResult);
        }
    }

   public void setMode(int mode){
        this.mode = (byte) mode;
   }


    /**
     *
     * @param count
     */
   public void setCount(int count){
       mCountTotal = (short) count;
       mMiddleResult = (short) count;
       mTvCount.setText(count+"");
    }

    public void setListener(IAddDeleteCountViewClickListener listener) {
        mListener = listener;
    }


    public interface IAddDeleteCountViewClickListener{
        void changeValue(int value);
    }

}
