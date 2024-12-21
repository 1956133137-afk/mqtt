package com.yannuo.dgcanteen.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class LoadingView extends View {
    private Paint paint;           // 画笔
    private int circleRadius = 4; // 小圆点半径
    private int viewSize = 45;    // View 的大小
    private float rotationAngle = 0f; // 旋转角度
    private int dotCount = 6;     // 小圆点个数
    private boolean isAnimating = true; // 动画控制

    public LoadingView(Context context) {
        super(context);
        init();
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化画笔
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLUE);

        // 开启动画
        startAnimation();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 设置 View 的大小
        setMeasuredDimension(viewSize, viewSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 绘制小圆点
        for (int i = 0; i < dotCount; i++) {
            float angle = (float) (2 * Math.PI / dotCount * i) + rotationAngle;
            float x = (float) (centerX + (viewSize / 3) * Math.cos(angle));
            float y = (float) (centerY + (viewSize / 3) * Math.sin(angle));
            int alpha = (int) (255 * ((i + dotCount - (rotationAngle / (2 * Math.PI)) * dotCount) % dotCount) / dotCount);
            paint.setAlpha(alpha);
            canvas.drawCircle(x, y, circleRadius, paint);
        }
    }

    private void startAnimation() {
        post(new Runnable() {
            @Override
            public void run() {
                if (isAnimating) {
                    rotationAngle += 0.1f; // 旋转角度增量
                    if (rotationAngle >= 2 * Math.PI) {
                        rotationAngle -= 2 * Math.PI;
                    }
                    invalidate(); // 重绘
                    postDelayed(this, 16); // 每 16ms 刷新一次 (约 60fps)
                }
            }
        });
    }

    public void stopAnimation() {
        isAnimating = false;
    }

    public void startAnimationAgain() {
        if (!isAnimating) {
            isAnimating = true;
            startAnimation();
        }
    }
}
