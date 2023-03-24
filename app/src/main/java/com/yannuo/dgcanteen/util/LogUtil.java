package com.yannuo.dgcanteen.util;


import android.util.Log;

import com.safframework.log.L;
import com.safframework.log.LogLevel;


/**
 * author:Almighty
 * date: 2021/3/26 13:46.
 */
public class LogUtil {
    public static byte currentLev = 4 ;
    public static byte debugLev = 4;
    public static byte infoLev = 3;
    public static byte warningLev = 2;
    public static byte errorLev = 1;

//    public static void d(String clazz,String log){
//        if (currentLev >= debugLev) Log.d(clazz, log);
//
//    }
//
//    public static void i(String clazz,String log){
//        if (currentLev>=infoLev) Log.i(clazz, log);
//    }
//
//    public static void w(String clazz,String log){
//        if (currentLev >= warningLev) Log.w(clazz, log);
//    }
//
//    public static void e(String clazz,String log){
//        if (currentLev>=errorLev) Log.e(clazz, log);
//    }


    public static void setLev(LogLevel lev){
        L.setLogLevel(lev);
    }

    public static void d(String clazz,String log){
        L.d(clazz, log);
    }

    public static void i(String clazz,String log){
        L.i(clazz, log);
    }

    public static void w(String clazz,String log){
        L.w(clazz, log);
    }

    public static void e(String clazz,String log){
        L.e(clazz, log);
    }

}
