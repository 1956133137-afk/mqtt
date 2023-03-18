package com.yannuo.dgcanteen.nets;


import com.yannuo.paylib.nets.ApiServiceOfPay;
import com.yannuo.paylib.nets.OkHttpUtilsOfPay;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientOfPay {
//        private final static  String BASE_URL ="http://192.168.2.50:28099/";
    private final  String BASE_URL ="https://test.yannuozhineng.com/ccb/api/";
    private static RetrofitClientOfPay object;
    private static ApiServiceOfPay mService ;

    private RetrofitClientOfPay(){
        mService = new Retrofit.Builder()
                .client(OkHttpUtilsOfPay.Companion.getInstance())
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiServiceOfPay.class);
    }


    public static ApiServiceOfPay getApi() {
        if (mService == null) {
            synchronized (RetrofitClientOfPay.class) {
                object = new RetrofitClientOfPay();
            }
        }
        return mService;
    }

}
