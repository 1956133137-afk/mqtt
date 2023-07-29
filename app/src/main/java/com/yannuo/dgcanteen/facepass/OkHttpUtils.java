package com.yannuo.dgcanteen.facepass;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

public class OkHttpUtils {
    private static final long HTTP_CONNECT_TIMEOUT = 10;
    private static final long HTTP_READ_TIMEOUT = 30;
    private static final String RESPONSE_CACHE = "alex_http_cache";
    private static final long RESPONSE_CACHE_SIZE = 10485760;
    private static OkHttpClient singleton;

    public static OkHttpClient getInstance() {
        if (singleton == null) {
            synchronized (OkHttpUtils.class) {
                if (singleton == null) {
                    HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
                    logInterceptor.setLevel(Level.BODY);
                    Builder builder = new Builder();
                    builder.connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS).readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS).addInterceptor(logInterceptor);
                    //httts临时方案信任所有证书
                    builder.sslSocketFactory(createSSLSocketFactory(), new TrustAllCerts()).hostnameVerifier(new TrustAllHostnameVerifier());
                    singleton = builder.build();
                }
            }
        }
        return singleton;
    }

    private static class TrustAllCerts implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) { return true; }
    }

    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssfFactory;
    }


}
