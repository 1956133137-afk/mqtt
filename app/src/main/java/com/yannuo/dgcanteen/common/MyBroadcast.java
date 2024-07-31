package com.yannuo.dgcanteen.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.yannuo.dgcanteen.activitys.InitActivity;
import com.yannuo.dgcanteen.printer.USBPrinterHelper;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ToastShowUtil;

public class MyBroadcast extends BroadcastReceiver {
    private String TAG = "MyBroadcast";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "create ：" + MyApplication.getCreate());
        switch (intent.getAction()) {
            case "android.intent.action.BOOT_COMPLETED":
                if (!MyApplication.getCreate()) {
                    ToastShowUtil.show("boot");
                    LogUtil.i("MyBroadcast", "reboot...");
                    Intent intent1 = new Intent(context, InitActivity.class);
                    intent1.setAction(Intent.ACTION_MAIN);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent1.addCategory(Intent.CATEGORY_LAUNCHER);
                    context.startActivity(intent1);
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                USBPrinterHelper.Companion.getInstance().queryPrinter();
                LogUtil.d(TAG, "连接USB设备");
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                LogUtil.d(TAG, "断开USB设备");
                break;
            default:
                break;
        }
    }
}
