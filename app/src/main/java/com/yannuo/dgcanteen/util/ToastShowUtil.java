package com.yannuo.dgcanteen.util;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.yannuo.dgcanteen.MyApplication;
import com.yannuo.dgcanteen.R;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


public class ToastShowUtil {
    private static Toast sToast;
    private static Disposable sSubscribe;


    public static void show(String showStr){
        if (sToast==null){
            sToast = Toast.makeText(MyApplication.applicationContext,"", Toast.LENGTH_LONG);
            LinearLayout view = (LinearLayout) sToast.getView();
            TextView childAt = (TextView) view.getChildAt(0);
            view.setBackgroundResource(R.drawable.toast_bg);
            childAt.setPadding(5,1,1,5);
            childAt.setTextSize(20);
            childAt.setTextColor(Color.WHITE);

        }
        sToast.setText(showStr);
        sToast.show();
    }

    public static void showt(String str){
        if (sSubscribe != null) {
            sSubscribe.dispose();
        }
        sSubscribe = Observable.just(str).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        show(s);
                    }
                });

    }

    public static void show(Context context, String desc){
        Toast.makeText(context,desc,Toast.LENGTH_SHORT).show();
    }
}
