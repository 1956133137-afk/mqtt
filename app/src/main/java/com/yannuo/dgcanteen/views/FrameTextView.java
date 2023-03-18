package com.yannuo.dgcanteen.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class FrameTextView extends androidx.appcompat.widget.AppCompatEditText {

    private Paint paint;

    public FrameTextView(Context context) {
        super(context);
        init();
    }

    public FrameTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrameTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setARGB(255,255,255,255);
        setBackground(null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0,0,getMeasuredWidth(),getMeasuredHeight(),paint);
    }
}
