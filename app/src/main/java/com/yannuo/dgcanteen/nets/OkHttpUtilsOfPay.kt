package com.yannuo.paylib.nets


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class OkHttpUtilsOfPay private constructor(){


    companion object {
        private const val HTTP_CONNECT_TIMEOUT = 10L
        private const val HTTP_READ_TIMEOUT: Long = 30


        val instance: OkHttpClient by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            val logInterceptor = HttpLoggingInterceptor()
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val builder = OkHttpClient.Builder()
            builder.connectTimeout(HTTP_CONNECT_TIMEOUT,TimeUnit.SECONDS)
                .readTimeout(HTTP_READ_TIMEOUT,TimeUnit.SECONDS)
                .addInterceptor( logInterceptor)

            builder.sslSocketFactory(createSSlSocketFactory()!!, TrustAllCerts())
                .hostnameVerifier(TrustAllHostnameVerifier())
            builder.build()
        }

        private fun createSSlSocketFactory() :SSLSocketFactory?{
            var ssfFactory :SSLSocketFactory ?= null
            try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, arrayOf(TrustAllCerts()), SecureRandom())
                ssfFactory = sc.socketFactory
            }catch (e :Exception){
                e.printStackTrace()
            }
            return ssfFactory
        }

        private class TrustAllCerts : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }

        private class TrustAllHostnameVerifier : HostnameVerifier{
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return true
            }


        }
    }



}
