package com.yannuo.dgcanteen.nets;

import android.graphics.Typeface;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.interfaces.ApiService;
import com.yannuo.dgcanteen.util.Constant;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    //        private final static  String BASE_URL ="http://192.168.2.82:9001/";
//    private final  String BASE_URL ="https://test.yannuozhineng.com/ccb/canteen/api/";
    private static ApiService mService;
    private static ApiService personService;
    private static ApiService mCcbService;

    private RetrofitClient() {
        mService = new Retrofit.Builder()
                .client(OkHttpUtils.Companion.getInstance())
                .baseUrl(MMKV.defaultMMKV().decodeString(Constant.ADDRESS))
//                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    public static ApiService getApi() {
        if (mService == null) {
            synchronized (RetrofitClient.class) {
                new RetrofitClient();
            }
        }
        return mService;
    }

    private static void setPersonService() {
        personService = new Retrofit.Builder()
                .client(OkHttpUtils.Companion.getInstance())
                .baseUrl(MMKV.defaultMMKV().decodeString(Constant.PERSON_ADDRESS))
//                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    public static ApiService getPersonApi() {
        if (personService == null) {
            synchronized (RetrofitClient.class) {
                setPersonService();
            }
        }
        return personService;
    }

    public static ApiService getApiCcb() {
        if (mCcbService == null) {
            synchronized (RetrofitClient.class) {
//                String basePath = "https://dining.icenter.ccb.com/CCBIS/"; //生产
//                String basePath = "http://121.40.54.232:8090/CCBIS/"; //测试
                String basePath = Constant.CCB_API_PATH; //测试
                mCcbService = new Retrofit.Builder()
                        .client(OkHttpUtils.Companion.getInstance())
                        .baseUrl(basePath)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(ApiService.class);
            }
        }
        return mCcbService;
    }

    public static void overLoad() {
        mService = null;
    }
}
