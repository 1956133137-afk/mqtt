package com.yannuo.dgcanteen.common;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

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
    private volatile static boolean create = false;


    @Override
    public void onCreate() {
        super.onCreate();
        create = true;
        applicationContext = this;
        LogManager.initLog();
        LogUtil.setLev(LogLevel.INFO);

        CrashReport.initCrashReport(this, "fd7e9dd24e", false); //初始化Bugly

        DishesDBHelper.getInstance(this);
        CommonAndDpToPxUtil.speakInit();

        String rootDir = MMKV.initialize(this);
        LogUtil.i(TAG,"mmkv root: " + rootDir);

        initMMKV();

//        Intent intent = new Intent(this, MyMqttService.class);
//        startService(intent);



    }

    public static boolean getCreate(){
       return create ;
    }

    public void initMMKV(){
        kv = MMKV.defaultMMKV();

        //调试模式
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0){
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
            Constant.CORP_ID = "1041";
            Constant.CCB_API_PATH = "http://121.40.54.232:8090/CCBIS/"; //测试
            Constant.STR_KEY  = "MKnzkGMRe08NmPv2TP6YbEzMOdjZzeEG"; //测试
            Constant.CIPHER  = "JfP81cjP2QYHjKsrmRKG49v0"; //测试
            Constant.ENCRYPTION_VECTOR  = "6Zt1MTo6"; //测试

            LogUtil.setLev(LogLevel.DEBUG);

        }else { //生产环境配置
            if (kv.decodeString(Constant.ADDRESS) == null){
            kv.encode(Constant.ADDRESS,"https://canteen.yannuozhineng.com/api/");
            }
            if (kv.decodeString(Constant.MQTT_ADDRESS) == null){
                kv.encode(Constant.MQTT_ADDRESS,"tcp://acms.yannuozhineng.com:3883");
            }
            if (kv.decodeString(Constant.MQTT_ACCOUNT) == null){
                kv.encode(Constant.MQTT_ACCOUNT,"acms");
            }
            if (kv.decodeString(Constant.MQTT_PASSWORD) == null){
                kv.encode(Constant.MQTT_PASSWORD,"ACMS2022~!@");
            }
            Constant.CORP_ID = "1046";
            Constant.CCB_API_PATH = "https://dining.icenter.ccb.com/CCBIS/"; //生产
            Constant.STR_KEY = "RReTnEXt6ebGdVfMybRrWU5CC46pJ9Mu"; //生产
            Constant.CIPHER  = "siclrkuYnJMEwGIy4bGGneqc"; //生产
            Constant.ENCRYPTION_VECTOR  = "sps49NVv"; //生产
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
