package com.yannuo.dgcanteen.util;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Presentation;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.greendao.annotation.Id;

import java.util.Objects;

/**
 * Created by zhanmian on 2024-07-22 19:46
 */
public class DisplayUtils {

    /**
     * 适配：修改设备密度
     */
    private static float sNoncompatDensity;
    private static float sNoncompatScaledDensity;
    private static Utils utils;

    public static void setCustomDensity(int width, Presentation presentation, @NonNull Activity activity, @NonNull final Application application) {
        DisplayMetrics activityMetrics = activity.getResources().getDisplayMetrics();
        DisplayMetrics displayMetrics = application.getResources().getDisplayMetrics();

        /*适配 1920 * 1080 大小屏幕*/
        float targetDensity = (float) activityMetrics.widthPixels / 1920;
        int targetDensityDpi = (int) (160 * targetDensity);

        displayMetrics.density = activityMetrics.density = targetDensity;
        displayMetrics.scaledDensity = activityMetrics.scaledDensity = targetDensity;
        displayMetrics.densityDpi = activityMetrics.densityDpi = targetDensityDpi;

        if (presentation != null) {
            final DisplayMetrics presentationDisplayMetrics = presentation.getResources().getDisplayMetrics();
            presentationDisplayMetrics.density = targetDensity;  // 1.0
            presentationDisplayMetrics.scaledDensity = targetDensity; // 1.0
            presentationDisplayMetrics.densityDpi = targetDensityDpi; // 160
        }

//        DisplayMetrics appDisplayMetrics = application.getResources().getDisplayMetrics();
//        if (sNoncompatDensity == 0) {
//            sNoncompatDensity = appDisplayMetrics.density;
//            sNoncompatScaledDensity = appDisplayMetrics.scaledDensity;
//            // 防止系统切换后不起作用
//            application.registerComponentCallbacks(new ComponentCallbacks() {
//                @Override
//                public void onConfigurationChanged(Configuration newConfig) {
//                    if (newConfig != null && newConfig.fontScale > 0) {
//                        sNoncompatScaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
//                    }
//                }
//
//                @Override
//                public void onLowMemory() {
//
//                }
//            });
//        }
//        float targetDensity = (float) appDisplayMetrics.widthPixels / width;
//        // 防止字体变小
//        float targetScaleDensity = targetDensity * (sNoncompatScaledDensity / sNoncompatDensity);
//        int targetDensityDpi = (int) (160 * targetDensity);
//
//        appDisplayMetrics.density = targetDensity;
//        appDisplayMetrics.scaledDensity = targetScaleDensity;
//        appDisplayMetrics.densityDpi = targetDensityDpi;
//
//        final DisplayMetrics activityDisplayMetrics = activity.getResources().getDisplayMetrics();
//        activityDisplayMetrics.density = targetDensity;
//        activityDisplayMetrics.scaledDensity = targetScaleDensity;
//        activityDisplayMetrics.densityDpi = 160;

//        utils = new Utils();
//        if (Objects.equals(utils.getDeviceName(), "rk3288")) {
//            if (presentation != null) {
//                final DisplayMetrics presentationDisplayMetrics = presentation.getResources().getDisplayMetrics();
//                float value = presentationDisplayMetrics.scaledDensity / appDisplayMetrics.scaledDensity;
//                presentationDisplayMetrics.densityDpi = 160; // 160
//                presentationDisplayMetrics.density = targetDensity;  // 1.0
//                presentationDisplayMetrics.scaledDensity = targetScaleDensity * value; // 1.0
//            }
//        } else {
//            if (presentation != null) {
//                final DisplayMetrics presentationDisplayMetrics = presentation.getResources().getDisplayMetrics();
//                presentationDisplayMetrics.densityDpi = 160; // 160
//                presentationDisplayMetrics.density = 1.0f;  // 1.0
//                presentationDisplayMetrics.scaledDensity = 1.0f; // 1.0
//            }
//        }
    }

}
