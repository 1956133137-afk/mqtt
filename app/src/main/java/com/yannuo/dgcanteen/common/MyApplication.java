package com.yannuo.dgcanteen.common;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.safframework.log.LogLevel;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.service.MyMqttService;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogManager;
import com.yannuo.dgcanteen.util.LogUtil;

import java.text.SimpleDateFormat;
import java.util.Date;


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

        CrashReport.initCrashReport(this, "fd7e9dd24e", false); //初始化Bugly

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
            kv.encode(Constant.ADDRESS,"https://test.yannuozhineng.com/ccb/canteen/api/");
        }
        if (kv.decodeString(Constant.MQTT_ADDRESS) == null){
            kv.encode(Constant.MQTT_ADDRESS,"tcp://test.yannuozhineng.com:1883");
        }
        if (kv.decodeString(Constant.MQTT_ACCOUNT) == null){
            kv.encode(Constant.MQTT_ACCOUNT,"acms");
        }
        if (kv.decodeString(Constant.MQTT_PASSWORD) == null){
            kv.encode(Constant.MQTT_PASSWORD,"ACMS2022~!@");
        }
        if (kv.decodeString(Constant.FINAL_TIME) == null){
            kv.encode(Constant.FINAL_TIME, new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date()));
        }
        if (kv.decodeInt(Constant.SHOW_TIME,-2) == -2){
            kv.encode(Constant.SHOW_TIME,2);
        }
    }




    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

}
