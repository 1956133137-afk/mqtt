package com.yannuo.dgcanteen.facepass;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;

import com.srp.AuthApi.AuthApi;
import com.srp.AuthApi.ErrorCodeConfig;
import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.Utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
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
    private int CERT_PATH_ONE ;
    //    private int CERT_PATH_TWO ;
//    private int ACTIVE_PATH ;
    private SDKInitResult callback;
    private Context context;

    private boolean hasAuth = false;
    private AuthApi obj;
    private Handler handler;
    public AuthFace(Context context) {
        this.context = context;
    }

    // 人脸算法授权是否成功
    public void authCheck(SDKInitResult callback) {
        handler = new Handler(Looper.getMainLooper());
        this.callback = callback;
        obj = new AuthApi();
        File filesDir = context.getFilesDir();
        File file = new File(filesDir, filedir);
        File[] files = file.listFiles((dir, name) -> name.equals(fileName));
        if (files!=null && files.length > 0){
            LogUtil.i("authApi0", "有效授权文件");
            boolean result = obj.authCheck(context);
            if (!result){
                activate();
            }else {
                authState(0,"授权成功");
            }
        }else {
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
                        }
                    }
                }
            });
        }
    }

    private void activate(){
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
        switch (data) {
            case "3423": // zhanmian@yeah.net 签约
                CERT_PATH_ONE = R.raw.id_3423_zm_cbg_36500_formal_one_stage;
                break;
            case "1926": // zhangzhanmian@yannuozhineng.com 试用
                CERT_PATH_ONE = R.raw.cbg30_one_stage;
                break;
                //工行额度
//            default:
//                CERT_PATH_ONE = R.raw.shengwei_face_one_reco;
        }

        try {
            LogUtil.i(TAG, "currentThread"+Thread.currentThread().getName());
            singleCertification();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void singleCertification() throws IOException {
        String cert = readInternal(CERT_PATH_ONE).trim();
        if (TextUtils.isEmpty(cert)) {
            LogUtil.i(TAG, "cert is null");
            return;
        }

        obj.authDevice(context.getApplicationContext(), cert, "", result -> {
            if (result.errorCode == ErrorCodeConfig.AUTH_SUCCESS) {
                handler.post(() -> {
                    authState(0,"授权成功");
                    LogUtil.d(TAG, "hasAuth "+ hasAuth);
                });

            }else {
                authState(1,"授权失败 "+result.errorMessage);
            }
        });

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
        File file = new File(filesDir, filedir);
        File licenseFile = new File(file,fileName);
        if (!licenseFile.exists()) {
            try {
                file.mkdirs();
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
