package com.yannuo.dgcanteen.util;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class DES3CBCUtil {

    //加密模式
    private static final String encryptionMode = "DESede/CBC/PKCS7Padding";
    //填充
    private static final String encryption = "DESede";
    //密码
    private static final String cipher = "JfP81cjP2QYHjKsrmRKG49v0";
    //加密向量
    private static final String encryptionVector = "6Zt1MTo6";

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

}
