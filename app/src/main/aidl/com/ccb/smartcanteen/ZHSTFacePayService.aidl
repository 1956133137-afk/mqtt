// ZHSTFacePayService.aidl
package com.ccb.smartcanteen;
import com.ccb.smartcanteen.PayResultListener;

// Declare any non-default types here with import statements

interface ZHSTFacePayService {

    /** 调用人脸支付 */
    void startFacePay(String orderParams, String offline, PayResultListener listener);
    /** 取消人脸支付 */
    void stopFacePay();
    /** 开启人脸离线交易 */
    void openOffline(PayResultListener listener);

    /** 关闭人脸离线交易 */
    void closeOffline(PayResultListener listener);
    /** 查看人脸离线订单 */
    void checkOfflineOrder();
    /** 设置活检超时时间 */
    void setTimeOut(int timeOut);

    void registFace(String dataJson, PayResultListener listener);
}