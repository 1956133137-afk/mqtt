package com.yannuo.dgcanteen.util;

import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import androidx.annotation.RequiresApi;

import com.yannuo.dgcanteen.common.MyApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;


public class CommonAndDpToPxUtil {

        private static TextToSpeech mSpeech;
        private static Bundle sBundle;

    /**
     * Dp单位转换
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
            float scale = context.getResources().getDisplayMetrics().density;

            return (int) (dpValue * scale + 0.5f);
        }


    /**
     * 获取设备的序列号
     * @return
     */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getDeviceSerial() {
        String serial = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            serial = getWlanMac().replace(":", "");
            return serial;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serial = Build.getSerial();
//            LogUtil.d("CommonAndDpToPxUtil", "SDK_INT >= 8.0 --> "+serial);
            return serial;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (MyApplication.applicationContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                LogUtil.d("CommonAndDpToPxUtil", "PERMISSION_GRANTED is get fail");
                return "no permission";
            }
        }

        try {
            Class<?> aClass = Class.forName("android.os.SystemProperties");
            Method get = aClass.getMethod("get", String.class);
            serial = (String) get.invoke(aClass, "ro.serialno");
//            LogUtil.d("CommonAndDpToPxUtil", "SDK_INT < 8.0 --> "+serial);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return serial;
    }

    /**
     * 获取 WLAN MAC 地址
     */
    public static String getWlanMac() {
        String macSerial = "";
        try {
            /*通过反射获取MAC地址*/
            Process process = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            macSerial = reader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        /*转大写*/
        return macSerial.trim().toUpperCase();
    }

    public static String getHttpCodeMessage(byte code){
        String message="未知错误";
        if (100 <= code &&  code <= 199)message = "成功接受请求，客户端需提交下一次请求才能完成整个处理过程";
        else if (300 <= code &&  code <= 399)message = "请求资源已移到新的地址(302,307,304)";
        else if (401 == code)message = "请求未授权";
        else if (403 == code )message = "服务器收到请求，但是拒绝提供服务";
        else if (404 == code)message = "请求资源不存在";
        else if (500 == code)message = "服务器发生不可预期的错误";
        else if (503 == code)message = "服务器当前不能处理客户端请求，一段时间后可能恢复正常";
        return message;
    }

    public synchronized static void speakInit(){
        if (mSpeech == null || sBundle == null) {
//            AudioManager am = (AudioManager) MyApplication.applicationContext.
//                    getSystemService(MyApplication.applicationContext.AUDIO_SERVICE);
//            //STREAM_MUSIC
//            int streamMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//            LogUtil.i("CommonAndDpToPxUtil","streamMaxVolume--> "+streamMaxVolume);
//            am.setStreamVolume(AudioManager.STREAM_MUSIC,100 ,0);
            sBundle = new Bundle();
            sBundle.putFloat(KEY_PARAM_VOLUME, 0.2f);

            mSpeech = new TextToSpeech(MyApplication.applicationContext, new MySpeechListener());

            sBundle = null;
        }
    }

    public static void speakWork(String work){
        //设置声音
        if (mSpeech == null) speakInit();
        boolean speaking = mSpeech.isSpeaking();
        if (speaking)mSpeech.stop();
        mSpeech.setSpeechRate(1.5f);
        mSpeech.speak( work, TextToSpeech.QUEUE_FLUSH,sBundle, null);
    }

    public static void release (){
        if (mSpeech != null) {
            mSpeech.stop();
            mSpeech.shutdown();
        }
    }


   static class MySpeechListener implements TextToSpeech.OnInitListener{
        @Override
        public void onInit(int status) {
            // TODO Auto-generated method stub
            if (status == TextToSpeech.SUCCESS) {
                LogUtil.i("CommonAndDpToPxUtil", "onInit: TTS INIT OK!");
                mSpeech.setLanguage(Locale.CHINESE);
//                mSpeech.setSpeechRate(1.8f);
            }
            else{
                LogUtil.i("CommonAndDpToPxUtil", "onInit:  TTS INIT NOK!");
            }
        }
    }

}
