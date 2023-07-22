package com.yannuo.dgcanteen.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yannuo.dgcanteen.activitys.CommodityActivity;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ToastShowUtil;


public class MyBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = "android.intent.action.BOOT_COMPLETED";
        if (intent.getAction().equals(action) && (!MyApplication.getCreate())){
            ToastShowUtil.show("boot");
            LogUtil.i("MyBroadcast", "reboot...");
            Intent intent1 = new Intent(context, CommodityActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent1.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(intent1);
        }
    }
}
