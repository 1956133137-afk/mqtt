package com.yannuo.dgcanteen.common;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
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
import com.yannuo.dgcanteen.service.MyMqttService;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogManager;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ScanDevice;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    private MMKV kv;


    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        LogManager.initLog();
        LogUtil.setLev(LogLevel.DEBUG);

        DishesDBHelper.getInstance(this);
        CommonAndDpToPxUtil.speakInit();

        String rootDir = MMKV.initialize(this);
        LogUtil.i(TAG,"mmkv root: " + rootDir);

        initMMKV();

        Intent intent = new Intent(this, MyMqttService.class);
        startService(intent);



    }

    public void initMMKV(){
        kv = MMKV.defaultMMKV();
        if (kv.decodeString(Constant.ADDRESS) == null){
            kv.encode(Constant.ADDRESS,"https://test.yannuozhineng.com/");
        }
        if (kv.decodeString(Constant.MQTT_ADDRESS) == null){
            kv.encode(Constant.MQTT_ADDRESS,"tcp://acms.yannuozhineng.com:0001");
        }
        if (kv.decodeString(Constant.MQTT_ACCOUNT) == null){
            kv.encode(Constant.MQTT_ACCOUNT,"yannuo");
        }
        if (kv.decodeString(Constant.MQTT_PASSWORD) == null){
            kv.encode(Constant.MQTT_PASSWORD,"123456");
        }
        if (kv.decodeString(Constant.FINAL_TIME) == null){
            kv.encode(Constant.FINAL_TIME, new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date()));
        }
    }




    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

}
