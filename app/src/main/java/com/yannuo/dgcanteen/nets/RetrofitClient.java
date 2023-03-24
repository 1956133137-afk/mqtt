package com.yannuo.dgcanteen.nets;


import com.yannuo.dgcanteen.interfaces.ApiService;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
//        private final static  String BASE_URL ="http://192.168.2.50:28099/";
    private final  String BASE_URL ="https://test.yannuozhineng.com/ccb/api/";
    private static ApiService mService ;

    private RetrofitClient(){
        mService = new Retrofit.Builder()
                .client(OkHttpUtils.Companion.getInstance())
                .baseUrl(BASE_URL)
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

}
