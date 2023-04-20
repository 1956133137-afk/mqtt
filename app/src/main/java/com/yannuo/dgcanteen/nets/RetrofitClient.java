package com.yannuo.dgcanteen.nets;


import android.graphics.Typeface;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.interfaces.ApiService;
import com.yannuo.dgcanteen.util.Constant;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
//        private final static  String BASE_URL ="http://192.168.2.50:28099/";
    private final  String BASE_URL ="https://test.yannuozhineng.com/ccb/canteen/api/";
    private static ApiService mService ;
    private static ApiService mCcbService ;

    private RetrofitClient(){

        mService = new Retrofit.Builder()
                .client(OkHttpUtils.Companion.getInstance())
                .baseUrl(MMKV.defaultMMKV().decodeString(Constant.ADDRESS))
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


    public static ApiService getApiCcb(){
        if (mCcbService == null) {
            synchronized (RetrofitClient.class) {
                String basePath = "http://121.40.54.232:8090/CCBIS/";
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



    public static void overLoad(){
        mService = null;
    }
}
