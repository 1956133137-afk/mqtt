package com.yannuo.dgcanteen.common;

import android.content.Context;
import android.hardware.usb.UsbManager;

import com.niu_tu.scanclient.UARTDriver;
import com.yannuo.dgcanteen.util.LogUtil;

public class NTScanHelp {
    private static final String TAG = "NTScanHelp";
    private static final String ACTION_USB_PERMISSION = "com.niu_tu.scanclient.USB_PERMISSION";
    public byte[] writeBuffer;
    public byte[] readBuffer;
    private UARTDriver uartDriver;
    private ScanDevice.DataCallBack callback;
    private Thread thread;

    private Long currentTime = 0L;

    /**
     * 初始化并打开扫描功能，与指定的回调进行关联
     * @param callback
     * @param context
     */
    public void OpenScanCode(ScanDevice.DataCallBack callback, Context context) {
        this.callback = callback;
        thread = new readThread();
        uartDriver = new UARTDriver((UsbManager) context.getSystemService(Context.USB_SERVICE), context, ACTION_USB_PERMISSION);
        if (!uartDriver.UsbFeatureSupported()) {
            LogUtil.e(TAG, "你的设备不支持USB HOST，请更换设备重试！");
        }
        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        int result = uartDriver.ResumeUsbPermission();
        if (result == 0) {
            result = uartDriver.ResumeUsbList();
            if (result == -1) {
                LogUtil.e(TAG, "没有找到牛图扫码设备");
                uartDriver.CloseDevice();
            } else if (result == 0) {
                if (uartDriver.mDeviceConnection != null) {
                    currentTime = System.currentTimeMillis();
                    LogUtil.d(TAG, "扫码头连接成功");
                    thread.start();
                }
            }
        }
    }

    /**
     * 关闭扫码
     */
    public void CloseScanCode() {
        LogUtil.d(TAG, "扫码设备已关闭！");
        uartDriver.CloseDevice();
        thread.interrupt();
        callback = null;
    }


    /**
     * 读取扫码数据线程
     */
    private class readThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            while (true) {
                if (uartDriver!= null) {
                    int length = uartDriver.ReadData(buffer, 4096);
                    if (System.currentTimeMillis() - currentTime < 200) continue;
                    if (length > 0) {
                        String recv = new String(buffer, 0, length);        //以字符串形式输出
                        LogUtil.d(TAG, "二维码数据: "+recv);
                        if (callback != null) {
                            callback.onData(recv);
                        }
                    }
                }
            }
        }
    }

}