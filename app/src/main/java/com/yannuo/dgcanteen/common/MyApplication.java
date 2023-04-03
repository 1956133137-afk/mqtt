package com.yannuo.dgcanteen.common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import com.safframework.log.LogLevel;
import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.download.CheckVersionWorker;
import com.yannuo.dgcanteen.service.KeepAliveJobService;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.LogManager;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ScanDevice;

import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


/**
 * author:Almighty
 * date: 2021/9/2 11:45.
 */
public class MyApplication extends Application {
    public static Context applicationContext;
    private String TAG = "MyApplication";
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEdit;


    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        LogManager.initLog();
        LogUtil.setLev(LogLevel.DEBUG);

        DishesDBHelper.getInstance(this);
        CommonAndDpToPxUtil.speakInit();
        ScanDevice.INSTANCE.openScan();
        String rootDir = MMKV.initialize(this);
        LogUtil.i(TAG,"mmkv root: " + rootDir);


        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                CheckVersionWorker.class,
                15,
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork("app-update-task", ExistingPeriodicWorkPolicy.REPLACE,work);

        // JobScheduler 拉活
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeepAliveJobService.startJob(this);
        }


    }


    @Override
    public void onTerminate() {
        LogUtil.i(TAG,"应用 terminate ！");
        ScanDevice.INSTANCE.closeScan();
        super.onTerminate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }




    protected void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
    }


    /**
     * 初始化配置值
     */
//    private void initConfig() {
//
//        mPreferences = getSharedPreferences(Constant.fileName, Context.MODE_PRIVATE);
//        mEdit = mPreferences.edit();
//
//        saveInt(Constant.codeKey, Constant.codeValue);
//
//        //*************************平台服务配置
//        saveStr(Constant.platformAddressKey ,Constant.platformAddressValue);
//        saveStr(Constant.mqttAddressKey,Constant.mqttAddressValue);
//        saveStr(Constant.mqttAccountKey,Constant.mqttAccountValue);
//        saveStr(Constant.mqttPassworkKey,Constant.mqttPassworkValue);
//
//
//        //*********************人脸算法
//        saveStr(Constant.yawKey,Constant.yawValue);
//        saveStr(Constant.pitchKey,Constant.pitchValue);
//        saveStr(Constant.rollKey,Constant.rollValue);
//        saveStr(Constant.liveKey,Constant.liveValue);
//        saveStr(Constant.recognizeKey,Constant.recognizeValue);
//        saveInt(Constant.rotationKey,Constant.rotationValue);
//        saveInt(Constant.previewKey, Constant.previewValue);
//        saveInt(Constant.distanceKey, Constant.distanceValue);
//        saveInt(Constant.ambiguityKey, Constant.ambiguityValue);
//        saveBoolean(Constant.mirrorKey, Constant.mirrorValue);
//        saveBoolean(Constant.maskKey, Constant.maskValue);
//
//
//        //************************其他设置
//        saveBoolean(Constant.codeVoiceKey, Constant.codeVoiceValue);
//        saveBoolean(Constant.antigenVoiceKey, Constant.antigenVoiceValue);
//        saveBoolean(Constant.throughKey, Constant.throughValue);
//        saveInt(Constant.timeoutKey, Constant.timeoutValue);
//
//        //********************健康码参数
//        saveStr(Constant.deviceIdKey, Constant.deviceIdValue);
//        saveStr(Constant.sceneNameKey, Constant.sceneNameValue);
//        saveStr(Constant.sceneAccountKey, Constant.sceneAccountValue);
//        saveStr(Constant.sceneCategoryKey, Constant.sceneCategoryValue);
//
//    }


    private void saveBoolean(String key ,boolean value){
        boolean swc = mPreferences.getBoolean(key,value);
        if (swc == value)
            mEdit.putBoolean(key,value).apply();
    }

    private void saveInt(String key ,int value){
        int swc = mPreferences.getInt(key,-1);
        if (swc == -1)
            mEdit.putInt(key,value).apply();
    }

}
