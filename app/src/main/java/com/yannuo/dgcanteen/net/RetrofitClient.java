package com.yannuo.dgcanteen.net;






import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitClient {
//    private final static  String BASE_URL ="http://192.168.2.166:28099/";
    private final static  String BASE_URL ="https://test.yannuozhineng.com/ccb/api/";

    private static RetrofitClient mInstance;

//    private static Retrofit mRetrofit = new Retrofit.Builder()
//            .client(OkHttpUtils.Companion.getInstance())
//            .baseUrl(BASE_URL)
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//            .addConverterFactory(GsonConverterFactory.create())
//            .build();

    private static ApiService mApiService ;

    private static Retrofit mCommonRetrofit;

    private RetrofitClient(){
        mCommonRetrofit = new Retrofit.Builder()
                .client(OkHttpUtils.Companion.getInstance())
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mApiService =  mCommonRetrofit.create(ApiService.class);
    }

    public static ApiService getService() {
        if (mInstance == null) {
            synchronized (RetrofitClient.class) {
                mInstance = new RetrofitClient();
            }
        }
        return mApiService;
    }

    public static void reInitCommonRetrofit() {
        mCommonRetrofit = null;
    }




//    public void reInit() {
//        mRetrofit = new Retrofit.Builder()
//                .client(OkHttpUtils.getInstance())
//                .baseUrl(BASE_URL)
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//        mApiService = ((ApiService)mRetrofit.create(ApiService.class));
//    }

//    public static Retrofit getRetrofit() {
//        return mRetrofit;
//    }
   /* public static Request getRequest() {
        if (request == null) {
            synchronized (Request.class) {
                request = mRetrofit.create(Request.class);
            }
        }
        return request;
    }*/



}
