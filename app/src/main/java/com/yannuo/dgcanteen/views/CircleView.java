package com.yannuo.dgcanteen.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class CircleView extends androidx.appcompat.widget.AppCompatImageView {
   private Paint mPaint;
   private Paint mPaint1;
   private int mRadius;
   private float mScale;

    public CircleView(Context context) {
        super(context);
        initElement();
    }

    public CircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initElement();
    }

    public CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initElement();
    }

    private void initElement(){
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);

        mPaint1 = new Paint();
        mPaint1.setAntiAlias(true);
        mPaint1.setDither(true);
        mPaint1.setColor(Color.parseColor("#00ff00"));
        mPaint1.setStrokeWidth(5);
        mPaint1.setStrokeJoin(Paint.Join.ROUND);
        mPaint1.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(widthMeasureSpec));
        mRadius = width / 2;
//        LogUtil.i("CircleView", "mRadius..."+mRadius + " width --> " +width);
        setMeasuredDimension(width, width);
    }

    private Bitmap drawableToBitmap(Drawable drawable){
        if (drawable instanceof BitmapDrawable){
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0,0,w,h);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }


//    @Override
//    public void setImageBitmap(Bitmap bm) {
//
//        super.setImageBitmap(bm);
//    }


    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        Bitmap bitmap = drawableToBitmap(getDrawable());
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        int min = Math.min(bitmap.getWidth(), bitmap.getHeight());
        mScale = (mRadius * 2f) / min;
//        LogUtil.i("CircleView", "onDraw..."+" mScale--> "+mScale+" bitmap.width --> "+bitmap.getWidth()+" bitmap.height --> "+bitmap.getHeight());
        Matrix matrix = new Matrix();
        matrix.setScale(mScale, mScale);
        bitmapShader.setLocalMatrix(matrix);
        mPaint.setShader(bitmapShader);
        canvas.drawCircle(mRadius, mRadius, mRadius,mPaint);
        canvas.drawCircle(mRadius, mRadius, mRadius-2.5f,mPaint1);

    }
}
