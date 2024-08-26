package com.yannuo.dgcanteen.util;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class Utils {

    public static void fullScreen(Window window) {

        WindowManager.LayoutParams params = window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
        window.setAttributes(params);
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
        if (Build.VERSION.SDK_INT >= 19) {
            uiFlags |= 0x00001000;  //SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide navigation bars - compatibility: building API level is lower thatn 19, use magic number directly for higher API target level
        } else {
            uiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        window.getDecorView().setSystemUiVisibility(uiFlags);
    }

    public static String getSN() {
        String serial = "";
        //通过android.os获取sn号
        try {
            serial = Build.SERIAL;
            if (!serial.equals("")&&!serial.equals("unknown"))return serial;
        }catch (Exception e){
            serial="";
        }

        //通过反射获取sn号
        try {
            Class<?> c =Class.forName("android.os.SystemProperties");
            Method get =c.getMethod("get", String.class);
            serial = (String)get.invoke(c, "ro.serialno");
            if (!serial.equals("")&&!serial.equals("unknown"))return serial;
        } catch (Exception e) {
            serial="";
        }
        return serial;
    }

    public String getDeviceName() {
        return Build.MODEL;
    }

//    public static void rotateNV21(int angle , byte[] dst , CameraPreviewData src ) {
//        switch (angle) {
//            case 0:
//                System.arraycopy(src.nv21Data, 0, dst, 0, src.nv21Data.length);
////                LogUtil.d(TAG,"进行0°旋转")
//                break;
//            case 90:
//                int index = 0;
//                int oldIndex = 0;
//                int x = 0;
//                for (int y = 0; y < src.width; y++) {
//                    x = 0;
//                    for (; x < src.height; x++) {
//                        oldIndex = (src.height - 1 - x) * src.width + y;
//                        dst[index++] = src.nv21Data[oldIndex];
//                    }
//                }
//
//                for (int y = 0; y < src.width; y += 2) {
//                    x = 0;
//                    for ( ; x < src.height; x += 2) {
//                        oldIndex = (src.height + (src.height - x - 2) / 2) * src.width + y;
//                        dst[index++] = src.nv21Data[oldIndex];
//                        dst[index++] = src.nv21Data[oldIndex + 1];
//                    }
//                }
//                break;
////                LogUtil.d(TAG,"进行90°旋转")
//        }
//    }
//            180 ->{
//                var index = 0
//                var oldX = 0
//                var oldY = 0
//                var oldIndex = 0
//                var vuY = 0
//
//                for (y in 0 until src.height){
//                    for (x in 0 until src.width){
//                        oldY = (src.height - 1 ) - y
//                        oldX = (src.width - 1) - x
//                        oldIndex  = oldY * src.width + oldX
//                        dst[index++] = src.nv21Data[oldIndex]
//                    }
//                }
//
//                for (y in 0 until src.height step 2){
//                    for (x in 0 until src.width step 2){
//                        oldY  = (src.height - 1 ) - (y +1)
//                        oldX  = (src.width - 1 ) - (x +1 )
//                        vuY  = src.height + oldY / 2
////                        val vuX  = oldX
//                        oldIndex  = vuY * src.width +oldX
//                        dst[index++] = src.nv21Data[oldIndex]
//                        dst[index++] = src.nv21Data[oldIndex + 1]
//                    }
//                }
//                LogUtil.d(TAG,"进行180°旋转")
//            }
//            270 ->{
//                var index = 0
//                var oldX = 0
////                var oldY = 0
//                var oldIndex = 0
//                var vuY = 0
//
//                for (y in 0 until src.width){
//                    for (x in 0 until src.height){
////                        val oldY = x
//                        oldX = src.width - 1 - y
//                        oldIndex  = x * src.width + oldX
//                        dst[index++] = src.nv21Data[oldIndex]
//                    }
//                }
//
//                for (y in 0 until src.width step 2){
//                    for (x in 0 until src.height step 2){
////                        val oldY  = x
//                        oldX  = src.width - 1  - (y +1 )
//                        vuY  = src.height + x / 2
////                        val vuX  = oldX
//                        oldIndex  = vuY * src.width +oldX
//                        dst[index++] = src.nv21Data[oldIndex]
//                        dst[index++] = src.nv21Data[oldIndex + 1]
//                    }
//                }
//                LogUtil.d(TAG,"进行270°旋转")
//            }
//        }
//    }

}
