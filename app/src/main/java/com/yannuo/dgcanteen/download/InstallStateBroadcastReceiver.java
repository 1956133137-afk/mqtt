package com.yannuo.dgcanteen.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InstallStateBroadcastReceiver extends BroadcastReceiver {
    public static final String UPDATE_ACTION = "android.intent.action.PACKAGE_REPLACED";

    @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UPDATE_ACTION)) {

                Intent intent2 = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Log.e("install","覆盖了:" + intent.getDataString() + "包名的程序");
                context.startActivity(intent2);
            }
//            //接收安装广播
//            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
//                String packageName = intent.getDataString();
//                Log.e("install","安装了:" + packageName + "包名的程序");
//
//            }
//                //接收卸载广播
//                if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
//                    String packageName = intent.getDataString();
//            Log.e("install","卸载了:" + packageName + "包名的程序");
//                }
            }




}
