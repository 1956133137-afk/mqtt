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

/**
 * Created by zhanmian on 2024-07-22 19:46
 */
public class DisplayUtils {

    /**
     * 适配：修改设备密度
     */
    private static float sNoncompatDensity;
    private static float sNoncompatScaledDensity;

    public static void setCustomDensity(Presentation presentation, @NonNull Activity activity, @NonNull final Application application) {
        DisplayMetrics appDisplayMetrics = application.getResources().getDisplayMetrics();
        if (sNoncompatDensity == 0) {
            sNoncompatDensity = appDisplayMetrics.density;
            sNoncompatScaledDensity = appDisplayMetrics.scaledDensity;
            // 防止系统切换后不起作用
            application.registerComponentCallbacks(new ComponentCallbacks() {
                @Override
                public void onConfigurationChanged(Configuration newConfig) {
                    if (newConfig != null && newConfig.fontScale > 0) {
                        sNoncompatScaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
                    }
                }

                @Override
                public void onLowMemory() {

                }
            });
        }
        float targetDensity = (float) appDisplayMetrics.widthPixels / 1920;
        // 防止字体变小
        float targetScaleDensity = targetDensity * (sNoncompatScaledDensity / sNoncompatDensity);
        int targetDensityDpi = (int) (160 * targetDensity);

        appDisplayMetrics.density = targetDensity;
        appDisplayMetrics.scaledDensity = targetScaleDensity;
        appDisplayMetrics.densityDpi = targetDensityDpi;

        final DisplayMetrics activityDisplayMetrics = activity.getResources().getDisplayMetrics();
        activityDisplayMetrics.density = targetDensity;
        activityDisplayMetrics.scaledDensity = targetScaleDensity;
        activityDisplayMetrics.densityDpi = targetDensityDpi;

        if (presentation != null) {
           final DisplayMetrics presentationDisplayMetrics = presentation.getResources().getDisplayMetrics();
           presentationDisplayMetrics.densityDpi = targetDensityDpi; // 160
           presentationDisplayMetrics.density = targetDensity;  // 1.0
           presentationDisplayMetrics.scaledDensity = targetScaleDensity; // 1.0
        }
    }

}
