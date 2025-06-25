package com.yannuo.dgcanteen.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;


public abstract class BaseDialog extends Dialog implements View.OnClickListener {
    private int layoutId;
    private Context mContext;


    public BaseDialog(@NonNull Context context, int layoutId) {
        super(context);
        mContext = context;
        this.layoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!(mContext instanceof Activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY - 1);
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            else getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | WindowManager.LayoutParams.TYPE_PHONE);
        }
        super.onCreate(savedInstanceState);

        setContentView(layoutId);
        setCancelable(false);
        initViewAndEvent();

//        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        getWindow().setBackgroundDrawable(new ColorDrawable());
        getWindow().setDimAmount(0.2f);

    }

    protected abstract void initViewAndEvent();


    private void initSystemBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        int uiOption =
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                |View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;    //
        getWindow().getDecorView().setSystemUiVisibility(uiOption);


    }


    @Override
    public void show() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        super.show();
        initSystemBar();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                initSystemBar();
            }
        });
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    @Override
    public void onClick(View v) {

    }
}
