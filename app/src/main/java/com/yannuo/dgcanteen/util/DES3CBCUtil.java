package com.yannuo.dgcanteen.util;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DES3CBCUtil {

    //加密模式
    private static final String encryptionMode = "DESede/CBC/PKCS7Padding";
    //填充
    private static final String encryption = "DESede";
    //密码
//    private static final String cipher = "JfP81cjP2QYHjKsrmRKG49v0";
    private static final String cipher = Constant.CIPHER;
    //加密向量
    private static final String encryptionVector = Constant.ENCRYPTION_VECTOR;
//    private static final String encryptionVector = "6Zt1MTo6";



    //private static final String cipher = "siclrkuYnJMEwGIy4bGGneqc";
    //private static final String encryptionVector = "sps49NVv";
    /**
     * 返回离线码过期时间戳
     */
    public static long getTimestamp(String PlainStr){
        return Long.parseLong(PlainStr.substring(PlainStr.lastIndexOf("@")+1));
    }

    /**
     * 获取密文串
     * @param src
     * @return
     */
    public static String transDecryption(String src){
        return decode(src.substring(3,src.indexOf("@")).replace(",","+"));
    }

    /**
     *  3DES 解密
     */
    private static String decode(String decryption){
        try {
            DESedeKeySpec spec =new DESedeKeySpec(cipher.getBytes(StandardCharsets.UTF_8));
            SecretKey deskey = SecretKeyFactory.getInstance(encryption).generateSecret(spec);
            Cipher cipher1 = Cipher.getInstance(encryptionMode);
            IvParameterSpec ips = new IvParameterSpec(encryptionVector.getBytes(StandardCharsets.UTF_8));
            cipher1.init(Cipher.DECRYPT_MODE,(Key) deskey, ips);
            byte[] bOut = cipher1.doFinal(Base64.decode(decryption.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
            return new String(bOut,StandardCharsets.UTF_8);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }



    public static String decryptRSA(String result,String pkey){
        String res = "";

        try {
            byte[] rsaDeBytes = Base64.decode(pkey.getBytes(), Base64.NO_WRAP);
            PKCS8EncodedKeySpec rsaDeKeySpec = new PKCS8EncodedKeySpec(rsaDeBytes);
            KeyFactory rsaDeFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = rsaDeFactory.generatePrivate(rsaDeKeySpec);
            Cipher rsaDeCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaDeCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] rsaDeMsgBytes = rsaDeCipher.doFinal(Base64.decode(result.substring(result.length() - 172), Base64.NO_WRAP));
            String ppk = new String(rsaDeMsgBytes,"utf-8");

            // dse解密
            Cipher deCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            KeySpec deKeySpec = new DESKeySpec(ppk.getBytes());
            SecretKeyFactory deDecretKeyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey deSecretKey = deDecretKeyFactory.generateSecret(deKeySpec);
            deCipher.init(Cipher.DECRYPT_MODE, deSecretKey,new SecureRandom());
            byte[] deMsgBytes = deCipher.doFinal(Base64.decode(result.substring(0,result.length() - 172),Base64.NO_WRAP));
            res = new String(deMsgBytes);
        }catch (Exception e){
            LogUtil.e("decryptRSA", e.getMessage());
        }
        return res;
    }

}
