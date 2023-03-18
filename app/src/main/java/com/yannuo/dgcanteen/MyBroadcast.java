package com.yannuo.dgcanteen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yannuo.dgcanteen.activitys.CommodityActivity;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ToastShowUtil;


public class MyBroadcast extends BroadcastReceiver {
    private String action = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(action)){
            ToastShowUtil.show("boot");
            LogUtil.i("MyBroadcast", "reboot...");
            Intent intent1 = new Intent(context, CommodityActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(intent1);
        }
    }
}
