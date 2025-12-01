package com.yannuo.dgcanteen.facepass;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.common.MyApplication;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.Utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;

import mcv.facepass.FacePassHandler;
import mcv.facepass.auth.FacePassAuthCode;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by zhanmian on 2021-08-20 15:14
 */
public class AuthFace {

    private final String TAG = "AuthFace";
    private String filedir = "lic";
    private String fileName = "license.cert";
    private int FACE_ALGOMALL_CERT_PATH = R.raw.face_algomall_auth_cert;
    private FaceInitListener callback;
    private Context context;

    private boolean hasAuth = false;
    private Handler handler;
    public AuthFace(Context context) {
        this.context = context;
    }

    // 人脸算法授权是否成功
    public void authCheck(FaceInitListener callback) {
        //初始化SDK
        FacePassHandler.initSDK(MyApplication.applicationContext, "");
        //获取到主线程
        handler = new Handler(Looper.getMainLooper());
        this.callback = callback;
        //获取应用内部存储目录
        File filesDir = context.getFilesDir();
        //应用内部存储目录的lic文件夹
        File file = new File(filesDir, filedir);
        //在目录下查找特定文件
        File[] files = file.listFiles((dir, name) -> name.equals(fileName));
        if (files!=null && files.length > 0){
            LogUtil.i("authApi0", "有效授权文件");
            //检查API状态
            activate();
        }else {
            //第一个参数是通过 MediaType.parse("application/json; charset=utf-8") 创建的媒体类型对象，用于指定请求体中数据的格式为 JSON 格式且字符编码采用 UTF-8,第二个参数是通过 Utils.getSN() 方法获取到的具体数据内容,然后将这些数据包装进请求体中，以便随 POST 请求一起发送给服务器。
            Request request = new Request.Builder().url("https://acms.yannuozhineng.com/api/deviceData/checkSN").post( RequestBody.create(MediaType.parse("application/json; charset=utf-8"), Utils.getSN())).build();
            Call call = OkHttpUtils.getInstance().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    authState(2, e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.code() == 200) {
                        String str = response.body().string();
                        LogUtil.i(TAG,str);
                        LicenseBean bean = new Gson().fromJson(str, LicenseBean.class);
                        if (bean.getCode() == 500){
                            authState(5,"未获取授权，请联系激活设备");
                        }else {
                            saveKey(String.valueOf(bean.getData()));
                            activate();
                            authCheck(callback);
                        }
                    }
                }
            });
        }
    }

    private void activate(){
        if (FacePassHandler.checkAuth_mcvsafe() || FacePassHandler.authCheck_algomall()){
            authState(0,"授权成功");
            return;
        }
        File filesDir = context.getFilesDir();
        File file = new File(filesDir, filedir);
        File licenseFile = new File(file, fileName);
        if (!licenseFile.exists()) {
            authState(3,"授权文件不存在");
            return;
        }
        FileInputStream fileInputStream ;
        byte[] bytes = new byte[1024];
        try {
            fileInputStream = new FileInputStream(licenseFile);
            fileInputStream.read(bytes);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String data = "";
        try {
            data = EncryptUtils.decrypt("yannuo_tech_2016", new String(bytes, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(data)){
            authState(4,"授权文件解密出错");
            return;
        }
        try{
            algomallCertification();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void algomallCertification() throws IOException {
        String cert = readInternal(FACE_ALGOMALL_CERT_PATH).trim();
        if (TextUtils.isEmpty(cert)) {
            LogUtil.i(TAG, "cert is null");
            return;
        }
        if (!FacePassHandler.authCheck_algomall()) {
            int ret = FacePassHandler.auth_algomall(cert);
            if (ret == FacePassAuthCode.FP_AUTH_OK) handler.post(() -> authState(0, "授权成功"));
            else authState(1, "授权失败: " + authErrorMsg(ret));
        }else{
            authState(0, "授权成功");
        }
    }

    private String authErrorMsg(int authCode){
        switch (authCode){
            case 0: return "Apply update: OK";
            case 1: return "未初始化授权或未调用initSDK接口";
            case 2: return "face++授权未调用 prepare_facepplus 接口";
            case 10000: return "不支持目标算法类型的授权";
            case 10001: return "解析JSON出错，JSON格式非法等";
            case 10002: return "JSON的参数无效或参数名称不存在";
            case 10003: return "创建文件夹失败";
            case 10004: return "写内容至文件错误";
            case 10005: return "读文件内容错误";
            case 10006: return "获取设备信息失败";
            case 10007: return "授权信息加密失败";
            case 10008: return "授权信息解密失败";
            case 10009: return "不存在的底库类型";
            case 10010: return "联网请求授权时 https 网络请求状态码非 200 错误";
            case 10011: return "网络故障";
            case 10012: return "目标算法未授权";
            case 10200: return "face++授权 license.bin 中若指定包名，则运行包名不符合授权要求";
            case 10201: return "face++授权 license.bin 中若指定平台，则运行平台不符合授权要求";
            case 10202: return "face++授权联网请求授权服务器返回错误";
            case 10203: return "face++授权过期";
            case 10204: return "face++授权设备与运行设备不匹配";
            case 10205: return "设备原先未进行过 face++授权或者原本地授权文件已经不存在";
            case 10300: return "硬授权（芯片授权）未知错误";
            case 10301: return "硬授权（芯片授权）测试授权过期";
            case 10302: return "硬授权（芯片授权）KEY 错误";
            case 10303: return "硬授权（芯片授权）没有 KEY";
            case 10304: return "硬授权（芯片授权）没有驱动";
            case 10305: return "硬授权（芯片授权）没有权限";
            case 10400: return "算法商城授权联网请求授权服务器返回错误";
            case 10401: return "算法商城授权过期";
            case 10502: return "算法商城 sku 与当前授权算法不匹配";
            case 10603: return "算法商城授权文件解密失败，或授权设备与运行设备不匹配";
        }
        return "未知";
    }

    private void authState(int code ,String message){
        LogUtil.d(TAG,"code "+code + " message "+message);
        if (callback!=null) callback.faceLicenseResult(code,message );
    }

    private String readInternal(int filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (String.valueOf(filename)!=null){
            InputStream inputStream = context.getResources().openRawResource(filename);
            byte[] buffer = new byte[1024];
            int len = inputStream.read(buffer);
            while (len > 0) {
                sb.append(new String(buffer, 0, len));
                len = inputStream.read(buffer);
            }
            inputStream.close();
        }

        return sb.toString();
    }


    // 保存激活码离线授权文件
    private void saveKey(String paramStr) {
        if(TextUtils.isEmpty(paramStr))return;
        File filesDir = context.getFilesDir();
        //内部存储目录下的lic文件
        File file = new File(filesDir, filedir);
        //lic文件夹下的license.cert文件
        File licenseFile = new File(file,fileName);

        if (!licenseFile.exists()) {
            try {
                //一次性将整个目录路径中的各级目录都创建出来
                file.mkdirs();
                //在对应的文件系统位置去创建这个文件,若已经存在则返回false，表示创建失败
                licenseFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String data = "";
        try {
            data = EncryptUtils.encrypt("yannuo_tech_2016", paramStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(licenseFile, false);
            fileOutputStream.write(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (fileOutputStream!=null) fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
